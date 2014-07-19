package org.code_revue.dns.message;

/**
 * Immutable class representing a question from the question section of a DNS message.
 *
 * @author Mike Fanning
 */
public final class DnsQuestion {

    private final String questionName;

    private final DnsRecordType questionType;

    private final DnsRecordClass questionClass;

    public DnsQuestion(String questionName, DnsRecordType questionType, DnsRecordClass questionClass) {
        this.questionName = questionName;
        this.questionType = questionType;
        this.questionClass = questionClass;
    }

    public String getQuestionName() {
        return questionName;
    }

    public DnsRecordType getQuestionType() {
        return questionType;
    }

    public DnsRecordClass getQuestionClass() {
        return questionClass;
    }

}
