package org.code_revue.dns.server.engine;

import org.code_revue.dns.message.DnsMessageOverlay;
import org.code_revue.dns.message.DnsResponseBuilder;
import org.code_revue.dns.message.DnsResponseCode;
import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.exception.LifecycleException;
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

    public void start() throws LifecycleException {

        logger.info("Starting Relay Engine");

        if (running) {
            throw new LifecycleException("Engine is already running");
        }

        try {
            logger.debug("Opening DatagramChannel");
            channel = DatagramChannel.open();

            logger.debug("Connecting to relay DNS server {} port {}", dnsServerIp, port);
            channel.connect(new InetSocketAddress(Inet4Address.getByAddress(dnsServerIp), port));
            running = true;
        } catch (UnknownHostException e) {
            logger.error("Could not connect to relay DNS server", e);
            throw new LifecycleException("Could not connect to server IP address", e);
        } catch (IOException e) {
            logger.error("Could not open and connect DatagramChannel", e);
            throw new LifecycleException("Could not open DatagramChannel", e);
        }
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

    public void stop() throws LifecycleException {

        logger.info("Stopping Relay Engine");

        if (!running) {
            logger.warn("Relay Engine is already stopped");
        } else {
            running = false;
            try {
                channel.close();
            } catch (IOException e) {
                logger.error("Error closing DatagramChannel", e);
                throw new LifecycleException("Error closing DatagramChannel", e);
            }
        }
    }

}