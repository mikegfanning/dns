package org.code_revue.dns.message;

import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.engine.QuestionNameResolverRule;
import org.code_revue.dns.server.resolver.SimpleResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.*;

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

        Assert.assertFalse(resolverRule.isValid(createPayload("cnn.net", DnsRecordType.A)));
    }

    @Test
    public void concurrentModification() throws InterruptedException, TimeoutException, BrokenBarrierException {

        final String[] domainNames = new String[] {
          "cnn.com", "facebook.com", "microsoft.com", "google.com", "arstechnica.com", "sdlfkjdlk.com",
                "sdlkjfflkj.com", "sdlkjkdjkjd.net", "sdlkjslkj.org"
        };

        final int numThreads = 20;
        final int numIterations = 10000;
        final CyclicBarrier barrier = new CyclicBarrier(numThreads + 1);

        for (int c = 0; c < numThreads; c++) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {

                        Random rand = ThreadLocalRandom.current();
                        barrier.await(5, TimeUnit.SECONDS);

                        for (int c = 0; c < numIterations; c++) {
                            if (rand.nextBoolean()) {
                                resolverRule.addDomain(domainNames[rand.nextInt(domainNames.length)]);
                            } else {
                                resolverRule.removeDomain(domainNames[rand.nextInt(domainNames.length)]);
                            }
                        }

                        barrier.await(5, TimeUnit.SECONDS);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
        }

        Random rand = ThreadLocalRandom.current();
        barrier.await(5, TimeUnit.SECONDS);

        for (int c = 0; c < numIterations; c++) {
            DnsPayload payload = createPayload(domainNames[rand.nextInt(domainNames.length)], DnsRecordType.A);
            if (resolverRule.isValid(payload)) {
                // Do something so the compiler doesn't try to optimize all this junk out.
                Assert.assertNotNull(resolverRule.getResolver());
            }
        }

        barrier.await(5, TimeUnit.SECONDS);

    }

    private DnsPayload createPayload(String question, DnsRecordType recordType) {
        DnsQueryBuilder builder = new DnsQueryBuilder();
        builder.addQuestion(new DnsQuestion(question, recordType, DnsRecordClass.IN));
        return new DnsPayload(null, builder.build());
    }

}
