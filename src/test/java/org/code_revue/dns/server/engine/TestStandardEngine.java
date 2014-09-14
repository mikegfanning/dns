package org.code_revue.dns.server.engine;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Mike Fanning
 */
public class TestStandardEngine {

    @Test
    public void stringConstructor() {
        StandardEngine engine = new StandardEngine("208.67.222.222");
        Assert.assertArrayEquals(new byte[] { (byte) 208, (byte) 67, (byte) 222, (byte) 222}, engine.getDnsServerIp());

        engine = new StandardEngine("0.0.0.0", 80);
        Assert.assertArrayEquals(new byte[] { 0, 0, 0, 0 }, engine.getDnsServerIp());
        Assert.assertEquals(80, engine.getPort());

        engine = new StandardEngine("1.1.1.1");
        Assert.assertArrayEquals(new byte[] { 1, 1, 1, 1 }, engine.getDnsServerIp());

        engine = new StandardEngine("255.255.255.255", 95);
        Assert.assertArrayEquals(new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 },
                engine.getDnsServerIp());
    }

}
