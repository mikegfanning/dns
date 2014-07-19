package org.code_revue.dns.message;

/**
 * List of DNS OP codes.
 *
 * @author Mike Fanning
 * @see <a href="http://www.tcpipguide.com/free/t_DNSMessageHeaderandQuestionSectionFormat.htm">
 *     http://www.tcpipguide.com/free/t_DNSMessageHeaderandQuestionSectionFormat.htm</a>
 */
public enum DnsOpCode {

    QUERY(0),
    IQUERY(1),
    STATUS(2),
    RESERVED(3),
    NOTIFY(4),
    UPDATE(5);

    private final int opCodeValue;

    private DnsOpCode(int opCodeValue) {
        this.opCodeValue = opCodeValue;
    }

    /**
     * Returns the numeric value for this OP code.
     * @return Numeric OP code
     */
    public int getOpCodeValue() {
        return opCodeValue;
    }

    /**
     * Converts numeric OP code to enumerated value.
     * @param value Numeric OP code
     * @return Enumerated OP code
     */
    public static DnsOpCode getOpCode(int value) {
        if (value < 0 || value >= DnsOpCode.values().length) {
            throw new IllegalArgumentException("Illegal Opcode value.");
        }
        return DnsOpCode.values()[value];
    }

}
