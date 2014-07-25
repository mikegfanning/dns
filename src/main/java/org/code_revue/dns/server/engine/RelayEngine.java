package org.code_revue.dns.server.engine;

import org.code_revue.dns.message.DnsMessageOverlay;
import org.code_revue.dns.message.DnsResponseBuilder;
import org.code_revue.dns.message.DnsResponseCode;
import org.code_revue.dns.server.DnsPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Engine that relays all queries to another server and passes responses off as its own. Mostly used for testing.
 *
 * @author Mike Fanning
 */
public class RelayEngine implements DnsEngine {

    private final Logger logger = LoggerFactory.getLogger(RelayEngine.class);

    public static final int DEFAULT_DNS_PORT = 53;

    private boolean running = false;
    private byte[] dnsServerIp;
    private int port;
    private DatagramChannel channel;

    public RelayEngine(byte[] dnsServerIp) {
        this(dnsServerIp, DEFAULT_DNS_PORT);
    }

    public RelayEngine(byte[] dnsServerIp, int port) {
        this.dnsServerIp = dnsServerIp;
        this.port = port;
    }

    /**
     * Starts the engine, opening a channel to the relay DNS server and connecting to it.
     * @throws java.lang.IllegalStateException If the engine is already running
     * @throws IOException If there is a problem communicating with the relay server
     * @throws UnknownHostException
     */
    public void start() throws IOException, UnknownHostException {

        logger.info("Starting Relay Engine");

        if (running) {
            throw new IllegalStateException("Engine is already running");
        }

        logger.debug("Opening DatagramChannel");
        channel = DatagramChannel.open();

        logger.debug("Connecting to relay DNS server {} port {}", dnsServerIp, port);
        channel.connect(new InetSocketAddress(Inet4Address.getByAddress(dnsServerIp), port));
        running = true;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Send DNS message data to external server and pass response back to caller.
     * @param payload Query
     * @return
     */
    @Override
    public DnsPayload processDnsPayload(DnsPayload payload) {
        try {
            channel.write(payload.getMessageData());
            logger.debug("DNS query forwarded to relay server");

            ByteBuffer response = ByteBuffer.allocateDirect(DnsMessageOverlay.MAX_UDP_DNS_LENGTH);
            channel.receive(response);
            logger.debug("DNS response received");

            response.limit(response.position());
            response.position(0);
            payload.setMessageData(response.slice());
            return payload;
        } catch (IOException e) {
            logger.error("Error communicating with relay server", e);
        }

        logger.debug("Returning SERVER_FAILURE message");
        DnsResponseBuilder builder = new DnsResponseBuilder(payload.getMessageData());
        builder.setResponseCode(DnsResponseCode.SERVER_FAILURE)
                .setRecursionAvailable(true);
        payload.setMessageData(builder.build());
        return payload;
    }

    /**
     * Stops the relay engine, closing and releasing all underlying resources.
     * @throws IOException If there is a problem closing the channel to the relay server
     */
    public void stop() throws IOException {

        logger.info("Stopping Relay Engine");

        if (!running) {
            logger.warn("Relay Engine is already stopped");
        } else {
            running = false;
            channel.close();
        }
    }

}