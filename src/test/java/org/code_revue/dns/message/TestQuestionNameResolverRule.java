package org.code_revue.dns.message;

import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.engine.QuestionNameResolverRule;
import org.code_revue.dns.server.resolver.DnsResolver;
import org.code_revue.dns.server.resolver.SimpleResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author Mike Fanning
 */
public class TestQuestionNameResolverRule {

    private QuestionNameResolverRule resolverRule;

    @Before
    public void setup() {
        resolverRule = new QuestionNameResolverRule(new SimpleResolver());
        resolverRule.addDomain("cnn.com");
        resolverRule.addDomain("facebook.com");
    }

    @Test
    public void checkResolverRule() {
        for (DnsRecordType recordType: DnsRecordType.values()) {
            Assert.assertTrue(resolverRule.isValid(createPayload("www.cnn.com", recordType)));
        }

        Assert.assertTrue(resolverRule.isValid(createPayload("a.b.cnn.com", DnsRecordType.A)));
        Assert.assertTrue(resolverRule.isValid(createPayload("cnn.com", DnsRecordType.A)));

        Assert.assertTrue(resolverRule.isValid(createPayload("CNN.COM", DnsRecordType.A)));

        Assert.assertTrue(resolverRule.isValid(createPayload("www.facebook.com", DnsRecordType.A)));

        Assert.assertFalse(resolverRule.isValid(createPayload("www.google.com", DnsRecordType.A)));

        Assert.assertFalse(resolverRule.isValid(createPayload("acnn.com", DnsRecordType.A)));
    }

    private DnsPayload createPayload(String question, DnsRecordType recordType) {
        DnsQueryBuilder builder = new DnsQueryBuilder();
        builder.addQuestion(new DnsQuestion(question, recordType, DnsRecordClass.IN));
        return new DnsPayload(null, builder.build());
    }

}
