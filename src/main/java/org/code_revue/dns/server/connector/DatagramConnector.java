package org.code_revue.dns.server.connector;

import org.code_revue.dns.message.DnsMessageOverlay;
import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.exception.ConnectorException;
import org.code_revue.dns.server.exception.LifecycleException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link org.code_revue.dns.server.connector.DnsConnector} which uses datagrams for communication.
 * This implementation does not currently support non-blocking operation.
 * <p>
 * The connector also captures some simple data about the number of messages received and sent.
 * </p>
 *
 * @author Mike Fanning
 */
public class DatagramConnector implements DnsConnector {

    private boolean blocking = true;
    private boolean running = false;

    private DatagramChannel channel;
    private String hostname = null;
    private static final int DEFAULT_SERVER_PORT = 53;
    private int port = DEFAULT_SERVER_PORT;

    private AtomicLong receiveCount = new AtomicLong(0);
    private AtomicLong sendCount = new AtomicLong(0);

    /**
     * Opens a datagram channel and binds it to the supplied host and port.
     * @throws LifecycleException
     */
    public void start() throws LifecycleException {
        if (running) {
            throw new LifecycleException("Connector is already running");
        }

        try {
            channel = DatagramChannel.open();
            if (null == hostname) {
                channel.bind(new InetSocketAddress(port));
            } else {
                channel.bind(new InetSocketAddress(hostname, port));
            }
            running = true;
        } catch (IOException e) {
            new LifecycleException("Could not open and bind DatagramChannel", e);
        }
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Reads from the datagram channel and returns a {@link org.code_revue.dns.server.DnsPayload}, which contains the
     * client's socket address and the binary data from the DNS query.
     * @return Query data
     * @throws ConnectorException If the {@link #start()} method has not been called or there is a communication error
     */
    @Override
    public DnsPayload read() throws ConnectorException {
        if (!running) {
            throw new ConnectorException(new LifecycleException("Connector is not running"));
        }

        try {
            ByteBuffer message = ByteBuffer.allocateDirect(DnsMessageOverlay.MAX_UDP_DNS_LENGTH);
            SocketAddress address = channel.receive(message);
            receiveCount.incrementAndGet();

            message.limit(message.position());
            message.position(0);
            return new DnsPayload(address, message);

        } catch (IOException e) {
            throw new ConnectorException("Error while reading from DatagramChannel", e);
        }
    }

    /**
     * Send a DNS response back to the client.
     * @param payload Response address and data
     * @return Number of bytes sent
     * @throws ConnectorException If the connector has not been started or there is a communication error
     */
    @Override
    public int write(DnsPayload payload) throws ConnectorException {
        if (!running) {
            throw new ConnectorException(new LifecycleException("Connector is not running"));
        }

        try {
            int result = channel.send(payload.getMessageData(), payload.getRemoteAddress());
            sendCount.incrementAndGet();
            return result;
        } catch (IOException e) {
            throw new ConnectorException("Error while writing to DatagramChannel", e);
        }
    }

    /**
     * Stops this connector and closes the underlying channel.
     * @throws LifecycleException If the connector is not running or there is a problem closing the channel
     */
    public void stop() throws LifecycleException {
        if (!running) {
            throw new LifecycleException("Connector is not running");
        }

        running = false;
        try {
            channel.close();
        } catch (IOException e) {
            throw new LifecycleException("Could not close DatagramChannel", e);
        }
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Get the number of datagram packets received. Note that this may not reflect the number of valid DNS queries
     * received.
     * @return Number of datagram packets received
     */
    public long getReceiveCount() {
        return receiveCount.get();
    }

    /**
     * Get the number of datagram packets sent.
     * @return Number of datagram packets sent
     */
    public long getSendCount() {
        return sendCount.get();
    }

}
