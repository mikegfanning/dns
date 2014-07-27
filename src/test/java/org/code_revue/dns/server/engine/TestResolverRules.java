package org.code_revue.dns.server.engine;

import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.engine.AddressRegexResolverRule;
import org.code_revue.dns.server.resolver.SimpleResolver;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Only have a test for the {@link org.code_revue.dns.server.engine.AddressRegexResolverRule} at the moment.
 *
 * @author Mike Fanning
 */
public class TestResolverRules {

    private static DnsPayload payload[];

    @BeforeClass
    public static void setup() throws UnknownHostException {
        byte[] address1 = new byte[] { (byte) 192, (byte) 168, (byte) 1, (byte) 1 };
        byte[] address2 = new byte[] { (byte) 192, (byte) 168, (byte) 1, (byte) 11 };
        byte[] address3 = new byte[] { (byte) 65, (byte) 64, (byte) 63, (byte) 62 };
        byte[] address4 = new byte[] { (byte) 10, (byte) 0, (byte) 0, (byte) 1 };

        payload = new DnsPayload[4];
        payload[0] = new DnsPayload(new InetSocketAddress(InetAddress.getByAddress(address1), 1111), null);
        payload[1] = new DnsPayload(new InetSocketAddress(InetAddress.getByAddress(address2), 2222), null);
        payload[2] = new DnsPayload(new InetSocketAddress(InetAddress.getByAddress(address3), 3333), null);
        payload[3] = new DnsPayload(new InetSocketAddress(InetAddress.getByAddress(address4), 4444), null);
    }

    @Test
    public void addressRegexResolverRule() throws UnknownHostException {

        String regex = ".*";
        AddressRegexResolverRule rule = new AddressRegexResolverRule(regex, new SimpleResolver());

        Assert.assertEquals(regex, rule.getRegex());
        for (DnsPayload p: payload) {
            Assert.assertTrue(rule.isValid(p));
        }

        regex = ".*666.*";
        rule.setRegex(regex);

        Assert.assertEquals(regex, rule.getRegex());
        for (DnsPayload p: payload) {
            Assert.assertFalse(rule.isValid(p));
        }

        regex = ".*192\\.168\\.1\\..*";
        rule.setRegex(regex);
        for (int i = 0; i < payload.length; i++) {
            if (i < 2) {
                Assert.assertTrue(rule.isValid(payload[i]));
            } else {
                Assert.assertFalse(rule.isValid(payload[i]));
            }
        }

    }

}
