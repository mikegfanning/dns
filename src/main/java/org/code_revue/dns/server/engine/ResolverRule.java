package org.code_revue.dns.server.engine;

import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.resolver.DnsResolver;

/**
 * Interface for a rule that maps {@link org.code_revue.dns.server.DnsPayload}s to
 * {@link org.code_revue.dns.server.resolver.DnsResolver}s. This is meant to be used in conjunction with the
 * {@link org.code_revue.dns.server.engine.ResolverChain} to map incoming queries to the appropriate resolver.
 *
 * @author Mike Fanning
 */
public interface ResolverRule {

    /**
     * Is this resolver valid for this DNS payload?
     * @param payload Socket address and binary DNS data
     * @return
     */
    public boolean isValid(DnsPayload payload);

    /**
     * Gets the {@link org.code_revue.dns.server.resolver.DnsResolver} associated with this rule.
     * @return Question resolver
     */
    public DnsResolver getResolver();

}
