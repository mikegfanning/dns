package org.code_revue.dns.server.engine;

import org.code_revue.dns.message.DnsQuestion;
import org.code_revue.dns.message.DnsRecord;
import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.engine.ResolverChain;
import org.code_revue.dns.server.engine.ResolverRule;
import org.code_revue.dns.server.resolver.DnsResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests for {@link org.code_revue.dns.server.engine.ResolverChain}. Thread safety is out the window for now.
 *
 * @author Mike Fanning
 */
public class TestResolverChain {

    private ResolverChain chain;

    @Before
    public void setup() {

        chain = new ResolverChain();

        chain.addRule(new DumbRule("1", false))
             .addRule(new DumbRule("2", false))
             .addRule(new DumbRule("3", false))
             .addRule(new DumbRule("4", true));
    }

    @Test
    public void addRemoveRules() {

        int size = chain.getResolverRules().size();
        String id = UUID.randomUUID().toString();

        chain.addRule(new DumbRule(id, false));
        {
            // Scoping dis here list.
            List<ResolverRule> rules = chain.getResolverRules();
            Assert.assertEquals(id, ((DumbRule) rules.get(rules.size() - 1)).getId());
        }

        chain.removeRule(0);
        for (ResolverRule rule: chain.getResolverRules()) {
            DumbRule dumb = (DumbRule) rule;
            Assert.assertNotSame("1", dumb.getId());
        }
        Assert.assertEquals(size, chain.getResolverRules().size());
    }

    @Test
    public void getResolver() {
        DumbResolver resolver = (DumbResolver) chain.getResolver(null);
        Assert.assertEquals("4", resolver.getId());
    }

    /**
     * This test is meant to stress cause a bunch of concurrent access to the
     * {@link org.code_revue.dns.server.engine.ResolverChain} and test it for thread safety.
     * <p>
     * Ignoring this test for now, as the chain is not thread safe - if it's going to be a component that can be
     * modified while the server is in operation it will need to be though.
     * </p>
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws BrokenBarrierException
     */
    @Test
    public void concurrentModification() throws InterruptedException, TimeoutException, BrokenBarrierException {

        // Put a bunch of false junk at the beginning of the resolver chain so that we have to iterate over some stuff.
        chain = new ResolverChain();
        for (int c = 0; c < 1000; c++) {
            chain.addRule(new DumbRule(UUID.randomUUID().toString(), false));
        }

        final ResolverChain resolverChain = chain;
        final int numThreads = 10;
        final int numIterations = 100000;
        final Random rand = new Random(System.nanoTime());
        final CyclicBarrier barrier = new CyclicBarrier(numThreads + 1);

        Runnable worker = new Runnable() {
            @Override
            public void run() {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                    for (int num = 0; num < numIterations; num++) {
                        if (num % numThreads >= (numThreads / 2)) {
                            resolverChain.addRule(new DumbRule(Long.toString(num), rand.nextFloat() > 0.9f));
                        } else {
                            resolverChain.removeRule(0);
                        }
                    }
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        for (int c = 0; c < numThreads; c++) {
            Thread thread = new Thread(worker);
            thread.start();
        }

        barrier.await(5, TimeUnit.SECONDS);

        for (int c = 0; c < numIterations; c++) {
            DnsResolver resolver = chain.getResolver(null);
            // Do something with the resolver so the compiler doesn't optimize all this out.
            if (null != resolver) {
                Assert.assertEquals(0, resolver.resolve(null).size());
            }
        }

        barrier.await(10, TimeUnit.SECONDS);

    }

    private static class DumbRule implements ResolverRule {

        private final String id;
        private final boolean isValid;
        private final DnsResolver resolver;

        public DumbRule(String id, boolean isValid) {
            this.id = id;
            this.isValid = isValid;
            this.resolver = new DumbResolver(id);
        }

        public String getId() {
            return id;
        }

        @Override
        public boolean isValid(DnsPayload payload) {
            return isValid;
        }

        @Override
        public DnsResolver getResolver() {
            return resolver;
        }
    }

    private static class DumbResolver implements DnsResolver {

        private final String id;

        public DumbResolver(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public List<DnsRecord> resolve(DnsQuestion question) {
            return Collections.emptyList();
        }
    }

}
