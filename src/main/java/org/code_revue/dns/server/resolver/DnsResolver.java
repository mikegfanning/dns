package org.code_revue.dns.server.resolver;

import org.code_revue.dns.message.DnsQuestion;
import org.code_revue.dns.message.DnsRecord;

import java.util.List;

/**
 * Interface for classes that answer {@link org.code_revue.dns.message.DnsQuestion}s.
 *
 * @author Mike Fanning
 */
public interface DnsResolver {

    /**
     * Given a question, return a list of response records.
     * @param question
     * @return
     */
    public List<DnsRecord> resolve(DnsQuestion question);

}
