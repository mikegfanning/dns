package org.code_revue.dns.message;

import org.code_revue.dns.util.ByteBufferUtils;

import java.nio.ByteBuffer;

/**
 * Wraps a {@link java.nio.ByteBuffer} and provides bean-like getters and setters for underlying DNS message.
 *
 * @author Mike Fanning
 * @see <a href="http://www.tcpipguide.com/free/t_DNSMessageHeaderandQuestionSectionFormat.htm">
 *     http://www.tcpipguide.com/free/t_DNSMessageHeaderandQuestionSectionFormat.htm</a>
 */
public class DnsMessageOverlay {

    private ByteBuffer messageData;
    private ByteBuffer headerData;
    private ByteBuffer questionData;

    public static final int HEADER_LENGTH = 12;
    public static final int MAX_UDP_DNS_LENGTH = 512;

    /**
     * Creates a new overlay for a DNS message
     * @param data DNS message
     */
    public DnsMessageOverlay(ByteBuffer data) {
        this.messageData = data;
        this.messageData.position(0);
        this.messageData.limit(HEADER_LENGTH);
        this.headerData = this.messageData.slice();
        this.messageData.position(HEADER_LENGTH);
        this.messageData.limit(this.messageData.capacity());
        this.questionData = this.messageData.slice();
        this.messageData.position(0);
    }

    public short getIdentifier() {
        return headerData.getShort(0);
    }

    public void setIdentifier(short id) {
        headerData.putShort(0, id);
    }

    public boolean isResponse() {
        return (headerData.get(2) & 0b10000000) != 0;
    }

    public void setQuery() {
        headerData.put(2, (byte) (headerData.get(2) & 0b01111111));
    }

    public void setResponse() {
        headerData.put(2, (byte) (headerData.get(2) | 0b10000000));
    }

    public DnsOpCode getOperationCode() {
        return DnsOpCode.getOpCode((headerData.get(2) & 0b01111000) >> 3);
    }

    public void setOperationCode(DnsOpCode opCode) {
        if (null == opCode) {
            throw new IllegalArgumentException("Op Code cannot be null");
        }
        byte opCodeByte = (byte) ((headerData.get(2) & 0b10000111) | (opCode.getOpCodeValue() << 3));
        headerData.put(2, opCodeByte);
    }

    public boolean isAuthoritativeAnswer() {
        return (headerData.get(2) & 0b00000100) != 0;
    }

    public void setAuthoritativeAnswer(boolean authoritative) {
        if (authoritative) {
            headerData.put(2, (byte) (headerData.get(2) | 0b00000100));
        } else {
            headerData.put(2, (byte) (headerData.get(2) & 0b11111011));
        }
    }

    public boolean isTruncated() {
        return (headerData.get(2) & 0b00000010) != 0;
    }

    public void setTruncated(boolean truncated) {
        if (truncated)  {
            headerData.put(2, (byte) (headerData.get(2) | 0b00000010));
        } else {
            headerData.put(2, (byte) (headerData.get(2) & 0b11111101));
        }
    }

    public boolean isRecursionDesired() {
        return (headerData.get(2) & 0b00000001) != 0;
    }

    public void setRecursionDesired(boolean desired) {
        if (desired)  {
            headerData.put(2, (byte) (headerData.get(2) | 0b00000001));
        } else {
            headerData.put(2, (byte) (headerData.get(2) & 0b11111110));
        }
    }

    public boolean isRecursionAvailable() {
        return (headerData.get(3) & 0b10000000) != 0;
    }

    public void setRecursionAvailable(boolean available) {
        if (available)  {
            headerData.put(3, (byte) (headerData.get(3) | 0b10000000));
        } else {
            headerData.put(3, (byte) (headerData.get(3) & 0b01111111));
        }
    }

    public DnsResponseCode getResponseCode() {
        return DnsResponseCode.getResponseType(headerData.get(3) & 0b00001111);
    }

    public void setResponseCode(DnsResponseCode responseCode) {
        if (null == responseCode) {
            throw new IllegalArgumentException("Response code cannot be null");
        }
        byte responseCodeByte = (byte) ((headerData.get(3) & 0b10000000) | responseCode.getResponseCodeValue());
        headerData.put(3, responseCodeByte);
    }

    public short getQuestionCount() {
        return headerData.getShort(4);
    }

    public void setQuestionCount(short qCount) {
        headerData.putShort(4, qCount);
    }

    public short getAnswerCount() {
        return headerData.getShort(6);
    }

    public void setAnswerCount(short aCount) {
        headerData.putShort(6, aCount);
    }

    public short getNameServerCount() {
        return headerData.getShort(8);
    }

    public void setNameServerCount(short nsCount) {
        headerData.putShort(8, nsCount);
    }

    public short getAdditionalRecordCount() {
        return headerData.getShort(10);
    }

    public void setAdditionalRecordCount(short arCount) {
        headerData.putShort(10, arCount);
    }

    public int getQuestionSectionLength() {

        int questionCount = getQuestionCount();
        int questionLength = 0;

        for (int c = 0; c < questionCount; c++) {
            byte segmentLength = questionData.get(questionLength);
            questionLength++;
            while (segmentLength != 0) {
                questionLength += segmentLength;
                segmentLength = questionData.get(questionLength);
                questionLength++;
            }
            questionLength += 4;
        }
        return questionLength;
    }

    public DnsQuestion[] getQuestions() {
        int questionCount = getQuestionCount();
        DnsQuestion[] questions = new DnsQuestion[questionCount];
        questionData.position(0);
        for (int i = 0; i < questionCount; i++) {
            String questionName = ByteBufferUtils.readDomainName(questionData);
            DnsRecordType questionType = DnsRecordType.getRecordType(questionData.getShort());
            DnsRecordClass questionClass = DnsRecordClass.getRecordClass(questionData.getShort());
            questions[i] = new DnsQuestion(questionName, questionType, questionClass);
        }
        return questions;
    }

    public DnsRecord[] getAnswers() {
        int answerCount = getAnswerCount();
        DnsRecord[] answers = new DnsRecord[answerCount];
        questionData.position(getQuestionSectionLength());
        for (int i = 0; i < answerCount; i++) {

            String recordName;
            short temp = questionData.getShort(questionData.position());
            // Check for compression - if length byte starts with 11, it is a reference to a name elsewhere in the
            // message.
            if (0xc000 == (temp & 0xc000)) {
                // Call getShort again to advance the position
                int namePosition = questionData.getShort() & 0x3fff;
                // Offset of domain name is within the entire message block, so use messageData
                messageData.position(namePosition);
                recordName = ByteBufferUtils.readDomainName(messageData);
            } else {
                recordName = ByteBufferUtils.readDomainName(questionData);
            }

            DnsRecordType recordType = DnsRecordType.getRecordType(questionData.getShort());
            DnsRecordClass recordClass = DnsRecordClass.getRecordClass(questionData.getShort());
            int ttl = questionData.getInt();
            short resourceDataLength = questionData.getShort();
            byte[] resourceData = new byte[resourceDataLength];
            questionData.get(resourceData);
            answers[i] = new DnsRecord(recordName, recordType, recordClass, ttl, resourceData);
        }

        return answers;
    }

}
