package org.code_revue.dns.message;

/**
 * Class representing a DNS record.
 *
 * @author Mike Fanning
 * @see <a href="http://www.tcpipguide.com/free/t_DNSMessageResourceRecordFieldFormats-2.htm">
 *     http://www.tcpipguide.com/free/t_DNSMessageResourceRecordFieldFormats-2.htm</a>
 */
public final class DnsRecord {

    private final String recordName;

    private final DnsRecordType recordType;

    private final DnsRecordClass recordClass;

    private final int ttl;

    private final byte[] resourceData;

    public DnsRecord(String recordName, DnsRecordType recordType, DnsRecordClass recordClass, int ttl,
                     byte[] resourceData) {
        this.recordName = recordName;
        this.recordType = recordType;
        this.recordClass = recordClass;
        this.ttl = ttl;
        // TODO: Defensive copy
        this.resourceData = resourceData;
    }

    public String getRecordName() {
        return recordName;
    }

    public DnsRecordType getRecordType() {
        return recordType;
    }

    public DnsRecordClass getRecordClass() {
        return recordClass;
    }

    public int getTtl() {
        return ttl;
    }

    public byte[] getResourceData() {
        return resourceData;
    }

}
