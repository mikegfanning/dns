package org.code_revue.dns.server.resolver;

import org.code_revue.dns.message.DnsQuestion;
import org.code_revue.dns.message.DnsRecord;
import org.code_revue.dns.message.DnsRecordClass;
import org.code_revue.dns.message.DnsRecordType;
import org.code_revue.dns.util.ByteBufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The crux of the whole project, this class returns the local server address for all type A queries, creates bogus
 * CNAME and NS records for such queries (hopefully leading to an A query), and mocks users of TXT queries.
 * <p>
 * This class can contain an exception list of domains that should not be resolved, so that they can fall through to the
 * relay and be resolved correctly.
 * </p>
 *
 * @author Mike Fanning
 */
public class SingleHostResolver implements DnsResolver {

    private final Logger logger = LoggerFactory.getLogger(SingleHostResolver.class);

    private List<String> exceptionList = new ArrayList<>();
    private final byte[] hostIp;
    private int ttl = 120;
    private byte[] text;

    /**
     * Creates a new resolver and fetches the local server IP address.
     * @throws UnknownHostException If the local address cannot be retrieved
     */
    public SingleHostResolver() throws UnknownHostException {
        hostIp = InetAddress.getLocalHost().getAddress();
        String textString = "type=lol";
        text = new byte[textString.length() + 1];
        text[0] = (byte) textString.length();
        int position = 1;
        for (byte b: textString.getBytes()) {
            text[position] = b;
            position++;
        }
    }

    /**
     * Checks to see if the question is in the exception list and, if not, resolves to the local IP address.
     * @param question Question
     * @return List of responses
     */
    @Override
    public List<DnsRecord> resolve(DnsQuestion question) {
        assert null != question;

        logger.debug("Attempting to resolve {}", question);
        String questionName = question.getQuestionName().toLowerCase();

        // Might want to use a better data structure and algorithm - probably a tree with name segments
        for (String exception: exceptionList) {
            if (questionName.endsWith(exception)) {
                logger.debug("Exception found {}, returning empty list", exception);
                return Collections.emptyList();
            }
        }

        List<DnsRecord> answers = new ArrayList<>();
        byte[] resourceData;
        switch (question.getQuestionType()) {
            case A:
                answers.add(new DnsRecord(questionName, DnsRecordType.A, DnsRecordClass.IN, ttl, hostIp));
                break;
            case CNAME:
                resourceData = ByteBufferUtils.encodeDomainName("a." + questionName);
                answers.add(new DnsRecord(questionName, DnsRecordType.CNAME, DnsRecordClass.IN, ttl, resourceData));
                break;
            case MX:
                // Do nothing and let it resolve for real I guess.
                break;
            case NS:
                resourceData = ByteBufferUtils.encodeDomainName("ns." + questionName);
                answers.add(new DnsRecord(questionName, DnsRecordType.NS, DnsRecordClass.IN, ttl, resourceData));
                break;
            case PTR:
                // Not doing any reverse lookup at the moment.
                break;
            case SOA:
                // Nope.
                break;
            case TXT:
                resourceData = text;
                answers.add(new DnsRecord(questionName, DnsRecordType.TXT, DnsRecordClass.IN, ttl, resourceData));
                break;
        }
        logger.debug("Resolved answers {}", answers);
        return answers;

    }

    /**
     * Get the list of domains that are excepted from this resolver.
     * @return
     */
    public List<String> getExceptionList() {
        return exceptionList;
    }

    /**
     * Sets exception list, overwriting any previous exceptions. Spring DI friendly method.
     * @param exceptions Domains that this resolver should <b>not</b> attempt to resolve
     */
    public void setExceptionList(List<String> exceptions) {
        logger.debug("Clobbering exception list with {}", exceptions);
        exceptionList = exceptions;
    }

    /**
     * Add an exception to the list of domains that will not be resolved to local address.
     * @param exception Domain name
     */
    public void addException(String exception) {
        logger.debug("Exception added \"{}\"", exception);
        exceptionList.add(exception.toLowerCase());
    }

    /**
     * Remove an exception from the list of domain that will not be resolved to local address.
     * @param exception Domain name
     */
    public void removeException(String exception) {
        logger.debug("Attempting to remove exception \"{}\"", exception);
        exceptionList.remove(exception.toLowerCase());
    }

    /**
     * Get the time-to-live value that is used for all resolved questions.
     * @return
     */
    public int getTtl() {
        return ttl;
    }

    /**
     * Set the time-to-live value that is used for all resolved questions.
     * @param ttl
     */
    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

}
