package org.code_revue.dns.server;

import org.code_revue.dns.message.DnsResponseBuilder;
import org.code_revue.dns.message.DnsResponseCode;
import org.code_revue.dns.server.connector.DatagramConnector;
import org.code_revue.dns.server.connector.DnsConnector;
import org.code_revue.dns.server.engine.DnsEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class ties together the other server components ({@link org.code_revue.dns.server.connector.DnsConnector},
 * {@link org.code_revue.dns.server.engine.DnsEngine}) to create a functioning DNS server. This component must be
 * started and stopped via the {@link #start()} and {@link #stop()} methods.
 * <p>
 * When the server is started, each connector is wrapped in a thread, allowing for simultaneous communications. When DNS
 * query messages are received on the connectors, they are passed along to an
 * {@link java.util.concurrent.ExecutorService} that invokes the engine and processes the message. The response is then
 * passed back to the original connector and sent to the client.
 * </p>
 * <p>
 * It is important to make the {@link java.util.concurrent.ExecutorService} sufficiently parallel to handle incoming
 * requests; otherwise the processing capabilities of the server could be exhausted by long running recursive queries
 * to other servers.
 * </p>
 *
 * @author Mike Fanning
 */
public class DnsServer {

    private final Logger logger = LoggerFactory.getLogger(DnsServer.class);

    private volatile boolean running = false;

    private ConcurrentMap<DnsConnector, ConnectorWorker> connectorWorkers = new ConcurrentHashMap<>();
    private DnsEngine engine;
    private ExecutorService executor;
    private AtomicLong connectorIndex = new AtomicLong(0);

    /**
     * Starts the server. After this method is invoked, all connectors begin reading messages and passing them to the
     * engine for processing. If an {@link java.util.concurrent.ExecutorService} has not been set, a
     * {@link java.util.concurrent.ThreadPoolExecutor} will be created by default.
     * @throws java.lang.IllegalStateException If the server is already running
     */
    public void start() {

        logger.info("Starting DNS Server");

        if (running) {
            throw new IllegalStateException("Server is already running");
        }

        if (null == executor) {
            logger.debug("No ExecutorService found, creating ThreadPoolExecutor");
            ThreadPoolExecutor tpExec = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(40));
            logger.debug("Prestarting all cores in ThreadPoolExecutor");
            tpExec.prestartAllCoreThreads();
            executor = tpExec;
        }

        for (ConnectorWorker worker : connectorWorkers.values()) {
            logger.debug("Starting connector thread {}", worker.getName());
            worker.start();
        }

        running = true;
    }

    /**
     * Indicates whether the server is currently running.
     * @return
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Adds a connector. If the server has already been started, the worker thread will begin reading from the connector
     * immediately.
     * @param connector
     */
    public void addConnector(DnsConnector connector) {

        logger.debug("Adding connector to server");

        long index = connectorIndex.incrementAndGet();
        ConnectorWorker worker = new ConnectorWorker(connector, "connector-" + index);
        ConnectorWorker oldWorker = connectorWorkers.putIfAbsent(connector, worker);

        if (null != oldWorker) {
            worker = oldWorker;
        }

        if (!worker.isAlive() && !worker.isShutdown() && running) {
            logger.debug("Server is already running, starting connector thread");
            worker.start();
        }
    }

    /**
     * Remove a connector from the server. If the server has already been started, this will interrupt the worker thread
     * that sends and receives messages through this connector.
     * @param connector
     */
    public void removeConnector(DnsConnector connector) {
        logger.debug("Attempting to remove connector");
        ConnectorWorker worker = connectorWorkers.remove(connector);

        if (null != worker && !worker.isShutdown()) {
            logger.debug("Stopping connector thread {}", worker.getName());
            worker.shutdown();
        }
    }

    /**
     * Stops the server and interrupts all connector threads. Also shuts down the underlying
     * {@link java.util.concurrent.ExecutorService}.
     */
    public void stop() {

        logger.info("Stopping DNS Server");

        if (!running) {
            logger.warn("DNS Server is already stopped");
        } else {

            running = false;

            for (ConnectorWorker worker : connectorWorkers.values()) {
                logger.debug("Stopping connector thread {}", worker.getName());
                worker.shutdown();
            }

            logger.debug("Stopping ExecutorService");
            executor.shutdown();
        }
    }

    private class ConnectorWorker extends Thread {

        private DnsConnector connector;
        private volatile boolean shutdown = false;

        public ConnectorWorker(DnsConnector connector, String name) {
            this.connector = connector;
            this.setName(name);
        }

        @Override
        public void run() {

            while (!shutdown) {
                if (connector.isBlocking()) {
                    try {

                        final DnsPayload payload = connector.read();

                        try {
                            if (null != payload) {

                                logger.debug("DNS query received from {}", payload.getRemoteAddress());

                                executor.submit(new Runnable() {
                                    @Override
                                    public void run() {
                                        DnsPayload response = engine.processDnsPayload(payload);
                                        try {
                                            logger.debug("Sending response to {}", payload.getRemoteAddress());
                                            connector.write(response);
                                        } catch (IOException e) {
                                            logger.error("Connector write error", e);
                                        } catch (Exception e) {
                                            logger.error("Connector error", e);
                                        }
                                    }
                                });
                            }
                        } catch (RejectedExecutionException e) {
                            logger.error("ExecutorService cannot accept any more tasks", e);
                            connector.write(returnServerFailure(payload));
                        } catch (Exception e) {
                            logger.error("Error resolving response", e);
                            connector.write(returnServerFailure(payload));
                        }
                    } catch (IOException e) {
                        logger.error("Error reading from connector", e);
                    }
                } else {
                    // uhhhh shit
                }
            }
        }

        private DnsPayload returnServerFailure(DnsPayload payload) {
            DnsResponseBuilder builder = new DnsResponseBuilder(payload.getMessageData());
            builder.setResponseCode(DnsResponseCode.SERVER_FAILURE);
            payload.setMessageData(builder.build());
            return payload;
        }

        public boolean isShutdown() {
            return shutdown;
        }

        public void shutdown() {
            this.shutdown = true;
        }
    }

    /**
     * Get the engine used to process DNS messages.
     * @return
     */
    public DnsEngine getEngine() {
        return engine;
    }

    /**
     * Set the engine used to process DNS messages.
     * @param engine
     */
    public void setEngine(DnsEngine engine) {
        assert null != engine;
        this.engine = engine;
    }

    /**
     * Get the {@link java.util.concurrent.ExecutorService} response for processing requests concurrently.
     * @return
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Set the {@link java.util.concurrent.ExecutorService} used to process requests concurrently.
     * @param executor
     */
    public void setExecutor(ExecutorService executor) {
        assert null != executor;
        this.executor = executor;
    }

}
