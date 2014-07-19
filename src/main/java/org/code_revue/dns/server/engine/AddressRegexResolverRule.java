package org.code_revue.dns.server.engine;

import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.resolver.DnsResolver;

import java.util.regex.Pattern;

/**
 * A {@link org.code_revue.dns.server.engine.ResolverRule} that uses a regular expression match on the payload address
 * to determine whether or not this {@link org.code_revue.dns.server.resolver.DnsResolver} should be used.
 *
 * @author Mike Fanning
 */
public class AddressRegexResolverRule implements ResolverRule {

    private String regex;
    private Pattern pattern;
    private DnsResolver resolver;

    /**
     * Creates a new resolver rule with the supplied regular expression and resolver.
     * @param regex Regular expression string
     * @param resolver Resolver to be used in question resolution
     */
    public AddressRegexResolverRule(String regex, DnsResolver resolver) {
        this.resolver = resolver;
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Compares socket address to regular expression to determine whether or not this is a valid resolver rule.
     * @param payload
     * @return
     */
    @Override
    public boolean isValid(DnsPayload payload) {
        return pattern.matcher(payload.getRemoteAddress().toString()).matches();
    }

    /**
     * Get the regular expression used by this rule.
     * @return Regular expressions string
     */
    public String getRegex() {
        return regex;
    }

    /**
     * Set the regular expression used by this rule.
     * @param regex Regular expression string
     */
    public void setRegex(String regex) {
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Get the question resolver used by this rule.
     * @return Question resolver
     */
    @Override
    public DnsResolver getResolver() {
        return resolver;
    }

    /**
     * Set the question resolver used by thi rule.
     * @param resolver Question resolver
     */
    public void setResolver(DnsResolver resolver) {
        this.resolver = resolver;
    }
}
