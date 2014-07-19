package org.code_revue.dns.server.engine;

import org.code_revue.dns.message.*;
import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.exception.LifecycleException;
import org.code_revue.dns.server.resolver.DnsResolver;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Standard engine for processing DNS queries. This engine maintains a
 * {@link org.code_revue.dns.server.engine.ResolverChain}, which it uses to determine the appropriate
 * {@link org.code_revue.dns.server.resolver.DnsResolver}. All questions are passed to the resolver and answers are
 * added to a response. If none of the questions can be answered, the query is relayed to another DNS server and the
 * response is marked non-authoritative.
 * <p>
 * This engine must be started and stopped in order to process queries. It also tracks some simple statistics about the
 * number of payloads it has processed and the number of errors encountered while processing messages.
 * </p>
 *
 * @author Mike Fanning
 */
public class StandardEngine implements DnsEngine {

    private boolean running;
    private byte[] dnsServerIp;
    private DatagramChannel channel;

    private ResolverChain resolverChain;

    private AtomicLong payloadsProcessed = new AtomicLong(0);
    private AtomicLong processingErrors = new AtomicLong(0);

    /**
     * Creates a new engine and uses the provided server IP for relaying queries that cannot be authoritatively
     * answered.
     * @param dnsServerIp Server address
     */
    public StandardEngine(byte[] dnsServerIp) {
        this.dnsServerIp = dnsServerIp;
    }

    /**
     * Starts the engine. This will open a datagram connection to the relay DNS server.
     * @throws LifecycleException If the server is already running or a connection to the relay server cannot be
     * established
     */
    public void start() throws LifecycleException {
        if (running) {
            throw new LifecycleException("Engine is already running");
        }

        try {
            channel = DatagramChannel.open();
            channel.connect(new InetSocketAddress(Inet4Address.getByAddress(dnsServerIp), 53));
            running = true;
        } catch (UnknownHostException e) {
            throw new LifecycleException("Could not connect to server IP address", e);
        } catch (IOException e) {
            throw new LifecycleException("Could not open DatagramChannel", e);
        }
    }

    /**
     * Processes a DNS payload by passing it through the resolver chain, answering questions, and, if necessary,
     * relaying to another DNS server.
     * @param payload Query
     * @return Response
     */
    public DnsPayload processDnsPayload(DnsPayload payload) {

        DnsMessageOverlay overlay = new DnsMessageOverlay(payload.getMessageData());

        // Validation
        // TODO: Yeah actually validate this thing.

        // Resolver Chain
        DnsResolver resolver = null;
        if (null != resolverChain) {
            resolver = resolverChain.getResolver(payload);
        }

        // Resolve
        List<DnsRecord> answers = new ArrayList<>();
        boolean authoritative = false;

        if (null != resolver) {
            DnsQuestion[] questions = overlay.getQuestions();
            authoritative = true;
            for (DnsQuestion question : questions) {
                List<DnsRecord> answer = resolver.resolve(question);
                if (null == answer || 0 == answer.size()) {
                    authoritative = false;
                    break;
                }
                answers.addAll(answer);
            }
        }

        if (authoritative) {

            DnsResponseBuilder builder = new DnsResponseBuilder(payload.getMessageData());
            builder.setAuthoritativeAnswer(true)
                    .setRecursionAvailable(true)
                    .setResponseCode(DnsResponseCode.NO_ERROR);
            for (DnsRecord answer: answers) {
                builder.addAnswer(answer);
            }
            payload.setMessageData(builder.build());

        } else {

            // Recursive Query
            try {
                channel.write(payload.getMessageData());
                ByteBuffer response = ByteBuffer.allocateDirect(DnsMessageOverlay.MAX_UDP_DNS_LENGTH);
                channel.receive(response);
                response.limit(response.position());
                response.position(0);
                payload.setMessageData(response.slice());
            } catch (IOException e) {
                DnsResponseBuilder builder = new DnsResponseBuilder(payload.getMessageData());
                builder.setResponseCode(DnsResponseCode.SERVER_FAILURE);
                payload.setMessageData(builder.build());
                processingErrors.incrementAndGet();
            }

        }

        payloadsProcessed.incrementAndGet();
        return payload;

    }

    /**
     * Stops the server and closes connection to relay DNS server.
     * @throws LifecycleException If the server is not running or there is a problem closing the connection to the relay
     * server
     */
    public void stop() throws LifecycleException {
        if (!running) {
            throw new LifecycleException("Engine is not running");
        }

        running = false;
        try {
            channel.close();
        } catch (IOException e) {
            throw new LifecycleException("Error closing DatagramChannel", e);
        }
    }

    /**
     * Get the resolver chain used by this engine.
     * @return
     */
    public ResolverChain getResolverChain() {
        return resolverChain;
    }

    /**
     * Set the resolver chain used by this engine.
     * @param resolverChain
     */
    public void setResolverChain(ResolverChain resolverChain) {
        this.resolverChain = resolverChain;
    }

    /**
     * Get the number of payloads processed by this engine. Note that this only counts payloads that are successfully
     * processed - if an error occurs it will be accounted for in the {@link #getProcessingErrors()} value.
     * @return Number of payloads successfully processed
     */
    public long getPayloadsProcessed() {
        return payloadsProcessed.get();
    }

    /**
     * Get the number of errors that occurred during payload processing.
     * @return Number of processing errors
     */
    public AtomicLong getProcessingErrors() {
        return processingErrors;
    }
}
