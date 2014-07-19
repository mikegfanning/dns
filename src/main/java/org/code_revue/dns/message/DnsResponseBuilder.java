package org.code_revue.dns.message;

import org.code_revue.dns.util.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class can be used to build responses to DNS queries. First an instance is created using a
 * {@link java.nio.ByteBuffer} containing the binary data for the query. Then the appropriate response fields are set
 * using setter methods. Finally, a response is created using the {@link #build()} method.
 * <p>
 * The response is a slice of the original buffer, and is trimmed to only contain the bytes for the response message.
 * Also, callers must ensure that the buffer supplied in the constructor has sufficient space for the response data.
 * This class is destructive on the data in the underlying buffer, but will not manipulate the position or limit values.
 * </p>
 * <p>
 * Setter methods can be chained to make response building more concise, like this:
 * </p>
 * <pre>
 *     {@code
 *     ByteBuffer response = (new DnsResponseBuilder(query)).setAuthoritativeAnswer(true)
 *                                                          .setRecursionAvailable(true)
 *                                                          .setResponseCode(DnsResponseCode.NO_ERROR)
 *                                                          .addAnswer(answer)
 *                                                          .build();
 *     }
 * </pre>
 *
 * @author Mike Fanning
 */
public class DnsResponseBuilder {

    private ByteBuffer messageData;
    private DnsMessageOverlay message;
    private List<DnsRecord> answers = new ArrayList<DnsRecord>();
    private List<DnsRecord> authorities = new ArrayList<DnsRecord>();
    private List<DnsRecord> additionalRecords = new ArrayList<DnsRecord>();

    /**
     * Creates a new response builder on top of the supplied query data.
     * @param query Buffer containing binary DNS query data
     */
    public DnsResponseBuilder(ByteBuffer query) {
        this.messageData = query.duplicate();
        this.message = new DnsMessageOverlay(messageData);
    }

    /**
     * Flags the response as authoritative or non-authoritative.
     * @param authoritative
     * @return this
     */
    public DnsResponseBuilder setAuthoritativeAnswer(boolean authoritative) {
        message.setAuthoritativeAnswer(authoritative);
        return this;
    }

    /**
     * Sets the recursion available flag in the response.
     * @param recursionAvailable
     * @return this
     */
    public DnsResponseBuilder setRecursionAvailable(boolean recursionAvailable) {
        message.setRecursionAvailable(recursionAvailable);
        return this;
    }

    /**
     * Sets the DNS response code.
     * @see <a href="http://www.tcpipguide.com/free/t_DNSMessageHeaderandQuestionSectionFormat.htm">
     *     http://www.tcpipguide.com/free/t_DNSMessageHeaderandQuestionSectionFormat.htm</a>
     * @param responseCode
     * @return this
     */
    public DnsResponseBuilder setResponseCode(DnsResponseCode responseCode) {
        message.setResponseCode(responseCode);
        return this;
    }

    /**
     * Add an answer record to the DNS response.
     * @param answer
     * @param recordType
     * @param recordClass
     * @param ttl
     * @param resourceData
     * @return this
     */
    public DnsResponseBuilder addAnswer(String answer, DnsRecordType recordType, DnsRecordClass recordClass, int ttl,
                                        byte[] resourceData) {
        answers.add(new DnsRecord(answer, recordType, recordClass, ttl, resourceData));
        return this;
    }

    /**
     * Add an answer record to the DNS response.
     * @param answer
     * @return this
     */
    public DnsResponseBuilder addAnswer(DnsRecord answer) {
        answers.add(answer);
        return this;
    }

    /**
     * Add an authority record to the DNS response.
     * @param record
     * @return this
     */
    public DnsResponseBuilder addAuthorityRecord(DnsRecord record) {
        authorities.add(record);
        return this;
    }

    /**
     * Add an additional record to the DNS response.
     * @param record
     * @return this
     */
    public DnsResponseBuilder addAdditionalRecord(DnsRecord record) {
        additionalRecords.add(record);
        return this;
    }

    /**
     * Writes the response data from setter method invocation to a {@link java.nio.ByteBuffer}. A slice of the original
     * buffer is created containing only the data for the DNS response.
     * @return Buffer containing binary DNS response
     */
    public ByteBuffer build() {
        message.setResponse();
        message.setAnswerCount((short) answers.size());
        message.setNameServerCount((short) authorities.size());
        message.setAdditionalRecordCount((short) additionalRecords.size());

        // Build up cache of names and their positions for compression.
        // Currently using a pretty simplistic algorithm, could be more complex I guess.
        messageData.position(DnsMessageOverlay.HEADER_LENGTH);
        Map<String, Integer> nameCache = new HashMap<String, Integer>();
        int qCount = message.getQuestionCount();
        for (int i = 0; i < qCount; i++) {
            int position = messageData.position();
            String name = ByteBufferUtils.readDomainName(messageData);
            if (!nameCache.containsKey(name)) {
                nameCache.put(name, position);
            }
            // Skip over other question fields
            messageData.position(messageData.position() + 4);
        }

        for (DnsRecord answer: answers) {
            writeDnsRecord(answer, nameCache);
        }

        for (DnsRecord authority: authorities) {
            writeDnsRecord(authority, nameCache);
        }

        for (DnsRecord additionalRecord: additionalRecords) {
            writeDnsRecord(additionalRecord, nameCache);
        }

        messageData.limit(messageData.position());
        messageData.position(0);
        return messageData.slice();
    }

    private void writeDnsRecord(DnsRecord record, Map<String, Integer> nameCache) {
        if (nameCache.containsKey(record.getRecordName())) {
            short position = (short) (0xc000 | nameCache.get(record.getRecordName()));
            messageData.putShort(position);
        } else {
            int position = messageData.position();
            ByteBufferUtils.writeDomainName(record.getRecordName(), messageData);
            nameCache.put(record.getRecordName(), position);
        }
        messageData.putShort((short) record.getRecordType().getNumericCode());
        messageData.putShort((short) 1);
        messageData.putInt(record.getTtl());
        messageData.putShort((short) record.getResourceData().length);
        messageData.put(record.getResourceData());
    }

}
