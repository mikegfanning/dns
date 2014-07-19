package org.code_revue.dns.server.engine;

import org.code_revue.dns.server.DnsPayload;

/**
 * Interface for the engine component of the DNS server, which is responsible for taking a query
 * {@link org.code_revue.dns.server.DnsPayload} and returning a response payload. Note that the engine may be
 * destructive on the original query payload, as it is mutable.
 *
 * @author Mike Fanning
 */
public interface DnsEngine {

    /**
     * Method for processing DNS query payloads and returning responses.
     * @param payload Query
     * @return Response
     */
    public DnsPayload processDnsPayload(DnsPayload payload);

}
