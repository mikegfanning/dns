package org.code_revue.dns.server.engine;

import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.resolver.DnsResolver;

import java.util.*;

/**
 * Chain of {@link org.code_revue.dns.server.engine.ResolverRule}s which is used to map incoming DNS queries to the
 * appropriate {@link org.code_revue.dns.server.resolver.DnsResolver}. The rules are check in order and the first
 * matching question resolver is returned.
 *
 * @author Mike Fanning
 */
public class ResolverChain {

    private List<ResolverRule> resolverRules = new ArrayList<>();

    /**
     * Add a new {@link org.code_revue.dns.server.engine.ResolverRule} to this chain.
     * @param rule
     * @return this
     */
    public ResolverChain addRule(ResolverRule rule) {
        resolverRules.add(rule);
        return this;
    }

    /**
     * Remove a {@link org.code_revue.dns.server.engine.ResolverRule} from this chain.
     * @param index Index of rule in chain.
     * @return
     */
    public ResolverRule removeRule(int index) {
        if (index < 0 || index >= resolverRules.size()) {
            throw new IllegalArgumentException("Index out of bounds.");
        }

        return resolverRules.remove(index);
    }

    /**
     * Given a DNS query payload, return the appropriate {@link org.code_revue.dns.server.resolver.DnsResolver}.
     * @param payload Socket address and binary DNS data
     * @return Question resolver
     */
    public DnsResolver getResolver(DnsPayload payload) {
        DnsResolver answer = null;

        for (ResolverRule rule: resolverRules) {
            if (rule.isValid(payload)) {
                answer = rule.getResolver();
                break;
            }
        }

        return answer;
    }

    /**
     * Resturns list of resolver rules, in the order they are tested and applied.
     * @return
     */
    public List<ResolverRule> getResolverRules() {
        return resolverRules;
    }

}
