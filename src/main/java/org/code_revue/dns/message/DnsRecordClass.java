package org.code_revue.dns.message;

/**
 * Enumeration of DNS record classes. Sorry Chaos and Hesiod, I'm just sticking with plain old Internet for now.
 *
 * @author Mike Fanning
 * @see <a href="http://en.wikipedia.org/wiki/Domain_Name_System#DNS_resource_records">
 *     http://en.wikipedia.org/wiki/Domain_Name_System#DNS_resource_records</a>
 */
public enum DnsRecordClass {
    IN;

    public static DnsRecordClass getRecordClass(int classCode) {
        if (classCode != 1) {
            throw new IllegalArgumentException("Invalid class code.");
        }
        return DnsRecordClass.IN;
    }
}
