package org.code_revue.dns.message;

/**
 * Partial enumeration of DNS record types.
 *
 * @author Mike Fanning
 * @see <a href="http://www.tcpipguide.com/free/t_DNSNameServerDataStorageResourceRecordsandClasses-3.htm#Table_166">
 *     http://www.tcpipguide.com/free/t_DNSNameServerDataStorageResourceRecordsandClasses-3.htm#Table_166</a>
 */
public enum DnsRecordType {

    A(1),
    NS(2),
    CNAME(5),
    SOA(6),
    PTR(12),
    MX(15),
    TXT(16);

    private final int numericCode;

    private DnsRecordType(int numericCode) {
        this.numericCode = numericCode;
    }

    /**
     * Get the numeric code used by the DNS protocol for this type of record.
     * @return Numeric code
     */
    public int getNumericCode() {
        return numericCode;
    }

    /**
     * Converts the numeric code for a record type into the enumerated value.
     * @param numericCode
     * @return Enumerated value
     */
    public static DnsRecordType getRecordType(int numericCode) {
        // Donald Knuth is muttering somewhere
        for (DnsRecordType type: DnsRecordType.values()) {
            if (numericCode == type.getNumericCode()) {
                return type;
            }
        }
        throw new IllegalArgumentException("Illegal numeric code.");
    }

}
