package org.code_revue.dns.server.resolver;

import org.code_revue.dns.message.DnsQuestion;
import org.code_revue.dns.message.DnsRecord;

import java.util.List;

/**
 * DNS Resolver that always returns null, indicating that the server cannot answer the supplied question.
 * @author Mike Fanning
 */
public class NullResolver implements DnsResolver {

    @Override
    public List<DnsRecord> resolve(DnsQuestion question) {
        return null;
    }

}
