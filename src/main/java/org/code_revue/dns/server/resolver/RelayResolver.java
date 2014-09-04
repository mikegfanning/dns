package org.code_revue.dns.server.resolver;

import org.code_revue.dns.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This resolver will forward questions to another DNS server for resolution.
 *
 * @author Mike Fanning
 */
public class RelayResolver implements DnsResolver {

    private final Logger logger = LoggerFactory.getLogger(RelayResolver.class);

    private static final int DEFAULT_DNS_PORT = 53;

    private volatile boolean running = false;

    private final byte[] dnsServerIp;
    private final int port;
    private DatagramChannel channel;

    /**
     * Creates a new resolver that will relay questions to the provided DNS server. The default DNS port (53) is used.
     * @param dnsServerIp
     */
    public RelayResolver(byte[] dnsServerIp) {
        this(dnsServerIp, DEFAULT_DNS_PORT);
    }

    /**
     * Creates a new resolver that will relay questions to the provided DNS server.
     * @param dnsServerIp
     * @param port
     */
    public RelayResolver(byte[] dnsServerIp, int port) {
        this.dnsServerIp = dnsServerIp;
        this.port = port;
    }

    /**
     * Starts the relay resolver.
     * @throws java.lang.IllegalStateException
     * @throws IOException
     */
    public void start() throws IOException {

        logger.info("Starting Relay Resolver");

        if (running) {
            throw new IllegalStateException("Relay Resolver is already running");
        }

        logger.debug("Opening DatagramChannel");
        channel = DatagramChannel.open();

        logger.debug("Connecting to relay DNS server {} port {}", dnsServerIp, port);
        channel.connect(new InetSocketAddress(Inet4Address.getByAddress(dnsServerIp), port));
        running = true;

    }

    /**
     * Resolves DNS questions by forwarding them to the supplied relay server.
     * @param question
     * @return List of answers or empty list if the relay server did not provide any or there was a communication error.
     */
    @Override
    public List<DnsRecord> resolve(DnsQuestion question) {

        if (!running) {
            throw new IllegalStateException("Relay Resolver is not running");
        }

        List<DnsRecord> answer = new ArrayList<>();

        try {
            DnsQueryBuilder builder = new DnsQueryBuilder();
            ByteBuffer buffer = builder.setOperationCode(DnsOpCode.QUERY)
                    .setRecursionDesired(true)
                    .addQuestion(question)
                    .build();

            logger.debug("Sending DNS query to relay server");
            channel.write(buffer);

            ByteBuffer response = ByteBuffer.allocate(DnsMessageOverlay.MAX_UDP_DNS_LENGTH);
            channel.receive(response);
            logger.debug("Response received from relay server");


            response.limit(response.position());
            response.position(0);
            DnsMessageOverlay overlay = new DnsMessageOverlay(response);
            answer.addAll(Arrays.asList(overlay.getAnswers()));
            // TODO: Get name servers and additional records you jerk
        } catch (IOException e) {
            logger.error("Error communicating with relay server, returning empty list of answers", e);
        }

        return answer;
    }

    /**
     * Stops the resolver, closing and releasing any underlying resources.
     * @throws IOException
     */
    public void stop() throws IOException {

        logger.info("Stopping Relay Resolver");

        if (!running) {
            logger.warn("Relay Resolver already stopped");
        } else {
            running = false;
            channel.close();
        }
    }
}
