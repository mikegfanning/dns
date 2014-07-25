package org.code_revue.dns.server.connector;

import org.code_revue.dns.server.DnsPayload;

import java.io.IOException;

/**
 * This interface represents an abstract endpoint for communicating binary DNS messages.
 *
 * @author Mike Fanning
 */
public interface DnsConnector {

    /**
     * Is the connector in blocking or non-blocking mode?
     * @return Duh
     */
    boolean isBlocking();

    /**
     * Reads a DNS message from the connector.
     * @return Payload containing sender address and binary message data
     * @throws java.io.IOException
     */
    DnsPayload read() throws IOException;

    /**
     * Send a DNS message through the connector.
     * @param payload Address of recipient and binary response data
     * @return Number of bytes sent
     * @throws java.io.IOException
     */
    int write(DnsPayload payload) throws IOException;
}
