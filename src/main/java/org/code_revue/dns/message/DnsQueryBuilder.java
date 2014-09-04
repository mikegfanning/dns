package org.code_revue.dns.message;

import org.code_revue.dns.util.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class can be used to create DNS queries from scratch. Users can set the various fields in a DNS query, then call
 * the {@link #build() build} method to get a {@link java.nio.ByteBuffer} containing the binary data for a query. Method
 * calls can be chained together for simplicity:
 *
 * <pre>
 *     {@code
 *     ByteBuffer message = (new DnsQueryBuilder()).setOperationCode(DnsOpCode.A)
 *                                                 .setRecursionDesired(true)
 *                                                 .addQuestion(question)
 *                                                 .build();
 *     }
 * </pre>
 * <p>
 * The id field is set to a random value using {@link java.security.SecureRandom}, if available. Otherwise it will use
 * {@link java.util.Random}. This value can be overridden using the {@link #setIdentifier(short)} method.
 * </p>
 *
 * @author Mike Fanning
 */
public class DnsQueryBuilder {

    private static Random random;

    static {
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            random = new Random();
        }
    }

    private ByteBuffer messageData;
    private DnsMessageOverlay overlay;
    private List<DnsQuestion> questions = new ArrayList<DnsQuestion>();

    /**
     * Creates a new DnsQueryBuilder, initializing the id field to a random value.
     */
    public DnsQueryBuilder() {
        messageData = ByteBuffer.allocate(DnsMessageOverlay.MAX_UDP_DNS_LENGTH);
        overlay = new DnsMessageOverlay(messageData);
        overlay.setIdentifier((short) random.nextInt());
    }

    /**
     * Set the identifier field of a DNS query.
     * @param id
     * @return this
     */
    public DnsQueryBuilder setIdentifier(short id) {
        overlay.setIdentifier(id);
        return this;
    }

    /**
     * Set OP code of DNS query.
     * @param code
     * @return this
     */
    public DnsQueryBuilder setOperationCode(DnsOpCode code) {
        overlay.setOperationCode(code);
        return this;
    }

    /**
     * Indicates whether resolver should use recursion (i.e. travers the DNS hierarchy) when resolving questions.
     * @param desired
     * @return this
     */
    public DnsQueryBuilder setRecursionDesired(boolean desired) {
        overlay.setRecursionDesired(desired);
        return this;
    }

    /**
     * Adds a {@link org.code_revue.dns.message.DnsQuestion} to the query. This can be called multiple times when
     * building a query.
     * @param question
     * @return this
     */
    public DnsQueryBuilder addQuestion(DnsQuestion question) {
        questions.add(question);
        return this;
    }

    /**
     * Creates a {@link java.nio.ByteBuffer} with the binary representation of this DNS query.
     * @return Binary data for this query
     */
    public ByteBuffer build() {
        overlay.setQuery();
        overlay.setTruncated(false);
        overlay.setQuestionCount((short) questions.size());

        messageData.position(DnsMessageOverlay.HEADER_LENGTH);
        for (DnsQuestion question: questions) {
            ByteBufferUtils.writeDomainName(question.getQuestionName(), messageData);
            messageData.putShort((short) question.getQuestionType().getNumericCode());
            messageData.putShort((short) 1);
        }

        messageData.limit(messageData.position());
        messageData.position(0);
        return messageData.slice();
    }

}
