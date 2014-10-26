package org.code_revue.dns.server.engine;

import org.code_revue.dns.message.DnsMessageOverlay;
import org.code_revue.dns.message.DnsQuestion;
import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.resolver.DnsResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This resolver rule uses a white list of acceptable domain names and the questions from the
 * {@link org.code_revue.dns.server.DnsPayload} to determine validity. For example, if "cnn.com" is added to the white
 * list, "www.cnn.com", "a.b.cnn.com" and "cnn.com" are valid.
 *
 * Adding, removing and resolving are all thread safe operations. This class makes no guarantees about the thread safety
 * of the {@link org.code_revue.dns.server.resolver.DnsResolver}.
 *
 * @author Mike Fanning
 */
public class QuestionNameResolverRule implements ResolverRule {

    private final Logger logger = LoggerFactory.getLogger(QuestionNameResolverRule.class);

    private NavigableSet<String> whiteList = new ConcurrentSkipListSet<>();

    private final DnsResolver resolver;

    /**
     * Creates a new resolver rule with nothing in the white list and the provided resolver.
     * @param resolver
     */
    public QuestionNameResolverRule(DnsResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Adds a domain to the white list of accepted domains.
     * @param domain
     * @return Indicates whether the domain was already in the white list
     */
    public boolean addDomain(String domain) {
        if (null == domain) {
            throw new IllegalArgumentException("Domain must not be null");
        }
        domain = scrubDomain(domain);
        logger.debug("Adding {} to whitelist set", domain);
        return whiteList.add(domain);
    }

    /**
     * Get list of domains in the white list. Note that these may not be formatted exactly the same way they were
     * originally added, but they will be equivalent.
     * @return List of domains
     */
    public List<String> getDomainIterator() {
        List<String> answer = new ArrayList<>(whiteList.size());
        for (String domain: whiteList) {
            answer.add(unScrubDomain(domain));
        }
        return answer;
    }

    /**
     * Removes a domain from the white list of accepted domains.
     * @param domain
     * @return Indicates whether the domain was in the white list
     */
    public boolean removeDomain(String domain) {
        if (null == domain) {
            throw new IllegalArgumentException("Domain must not be null");
        }
        domain = scrubDomain(domain);
        logger.debug("Removing {} from whitelist set", domain);
        return whiteList.remove(domain);
    }

    /**
     * Uses questions from {@link org.code_revue.dns.server.DnsPayload} and white list to determine validity of this
     * resolver rule.
     * @param payload Socket address and binary DNS data
     * @return
     */
    @Override
    public boolean isValid(DnsPayload payload) {

        DnsMessageOverlay overlay = new DnsMessageOverlay(payload.getMessageData());
        DnsQuestion[] questions = overlay.getQuestions();
        for (DnsQuestion question: questions) {
            String questionName = scrubDomain(question.getQuestionName());
            String lower = whiteList.lower(questionName);
            if (whiteList.contains(questionName) ||
                    (null != lower && questionName.startsWith(lower))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the resolver for this rule.
     * @return
     */
    @Override
    public DnsResolver getResolver() {
        return resolver;
    }

    private String scrubDomain(String domain) {
        return (new StringBuilder(domain)).reverse().append('.').toString().toLowerCase();
    }

    private String unScrubDomain(String domain) {
        StringBuilder builder = new StringBuilder(domain);
        return builder.reverse().substring(1, builder.length());
    }
}
