package org.code_revue.dns.server.resolver;

import org.code_revue.dns.message.DnsQuestion;
import org.code_revue.dns.message.DnsRecord;
import org.code_revue.dns.message.DnsRecordType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple resolver that has a map one record for each type of query. This was created for testing purposes.
 *
 * @author Mike Fanning
 */
public class SimpleResolver implements DnsResolver {

    private Map<DnsRecordType, DnsRecord> records = new HashMap<>();

    /**
     * Fetches DNS records from a map based on type of the question.
     * @param question
     * @return
     */
    @Override
    public List<DnsRecord> resolve(DnsQuestion question) {
        DnsRecord answer = records.get(question.getQuestionType());
        if (null == answer) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(answer);
        }
    }

    /**
     * Get the DNS record for a given type.
     * @param recordType
     * @return
     */
    public DnsRecord getDnsRecord(DnsRecordType recordType) {
        return records.get(recordType);
    }

    /**
     * Set the DNS record for a given type.
     * @param recordType
     * @param record
     */
    public void setDnsRecord(DnsRecordType recordType, DnsRecord record) {
        records.put(recordType, record);
    }

}
