package org.code_revue.dns.message;

/**
 * List of DNS response codes.
 *
 * @author Mike Fanning
 * @see <a href="http://www.tcpipguide.com/free/t_DNSMessageHeaderandQuestionSectionFormat.htm">
 *     http://www.tcpipguide.com/free/t_DNSMessageHeaderandQuestionSectionFormat.htm</a>
 */
public enum DnsResponseCode {

    NO_ERROR(0),
    FORMAT_ERROR(1),
    SERVER_FAILURE(2),
    NAME_ERROR(3),
    NOT_IMPLEMENTED(4),
    REFUSED(5),
    YX_DOMAIN(6),
    YX_RR_SET(7),
    NX_RR_SET(8),
    NOT_AUTH(9),
    NOT_ZONE(10);

    private final int responseCode;

    private DnsResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * Get the numeric representation for this response code.
     * @return Numeric response code
     */
    public int getResponseCodeValue() {
        return responseCode;
    }

    /**
     * Converts a numeric response code into the enumerated value.
     * @param code Numeric code
     * @return Enumerated value
     */
    public static DnsResponseCode getResponseType(int code) {
        if (code < 0 || code >= DnsResponseCode.values().length) {
            throw new IllegalArgumentException("Invalid response code.");
        }
        return DnsResponseCode.values()[code];
    }
}
