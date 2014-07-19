package org.code_revue.dns.message;

import org.junit.Assert;
import org.junit.Test;
import org.code_revue.dns.server.DnsServer;
import org.code_revue.dns.server.connector.DatagramConnector;
import org.code_revue.dns.server.engine.DnsEngine;
import org.code_revue.dns.server.engine.SingleTubeEngine;
import org.code_revue.dns.server.exception.LifecycleException;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Should probably create more unit tests. Also segment them according to the type of the test.
 *
 * @author Mike Fanning
 */
public class TestDnsMessages {

    @Test
    public void testDnsMessageOverlayHeader() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(DnsMessageOverlay.MAX_UDP_DNS_LENGTH);
        DnsMessageOverlay overlay = new DnsMessageOverlay(buffer);

        short id = (short) 0xaaaa;
        overlay.setIdentifier(id);
        Assert.assertEquals(id, overlay.getIdentifier());

        id = (short) 0x5555;
        overlay.setIdentifier(id);
        Assert.assertEquals(id, overlay.getIdentifier());

        overlay.setQuery();
        Assert.assertFalse(overlay.isResponse());
        overlay.setResponse();
        Assert.assertTrue(overlay.isResponse());

        for (DnsOpCode code: DnsOpCode.values()) {
            overlay.setOperationCode(code);
            Assert.assertEquals(code, overlay.getOperationCode());
        }

        overlay.setAuthoritativeAnswer(true);
        Assert.assertTrue(overlay.isAuthoritativeAnswer());
        overlay.setAuthoritativeAnswer(false);
        Assert.assertFalse(overlay.isAuthoritativeAnswer());

        overlay.setTruncated(true);
        Assert.assertTrue(overlay.isTruncated());
        overlay.setTruncated(false);
        Assert.assertFalse(overlay.isTruncated());

        overlay.setRecursionDesired(true);
        Assert.assertTrue(overlay.isRecursionDesired());
        overlay.setRecursionDesired(false);
        Assert.assertFalse(overlay.isRecursionDesired());

        overlay.setRecursionAvailable(true);
        Assert.assertTrue(overlay.isRecursionAvailable());
        overlay.setRecursionAvailable(false);
        Assert.assertFalse(overlay.isRecursionAvailable());

        for (DnsResponseCode code: DnsResponseCode.values()) {
            overlay.setResponseCode(code);
            Assert.assertEquals(code, overlay.getResponseCode());
        }

        overlay.setQuestionCount(Short.MAX_VALUE);
        Assert.assertEquals(Short.MAX_VALUE, overlay.getQuestionCount());
        overlay.setQuestionCount((short) 0);
        Assert.assertEquals(0, overlay.getQuestionCount());

        overlay.setAnswerCount(Short.MAX_VALUE);
        Assert.assertEquals(Short.MAX_VALUE, overlay.getAnswerCount());
        overlay.setAnswerCount((short) 0);
        Assert.assertEquals(0, overlay.getAnswerCount());

        overlay.setNameServerCount(Short.MAX_VALUE);
        Assert.assertEquals(Short.MAX_VALUE, overlay.getNameServerCount());
        overlay.setNameServerCount((short) 0);
        Assert.assertEquals(0, overlay.getNameServerCount());

        overlay.setAdditionalRecordCount(Short.MAX_VALUE);
        Assert.assertEquals(Short.MAX_VALUE, overlay.getAdditionalRecordCount());
        overlay.setAdditionalRecordCount((short) 0);
        Assert.assertEquals(0, overlay.getAdditionalRecordCount());
    }

    @Test
    public void queryOpenDnsA() throws IOException {

        String domainName = "www.google.com";
        byte[] openDnsIp = new byte[] { (byte) 208, (byte) 67, (byte) 222, (byte) 222};

        DnsQueryBuilder builder = new DnsQueryBuilder();
        builder.setRecursionDesired(true)
                .setOperationCode(DnsOpCode.QUERY)
                .addQuestion(new DnsQuestion(domainName, DnsRecordType.A, DnsRecordClass.IN));

        ByteBuffer buffer = ByteBuffer.allocateDirect(DnsMessageOverlay.MAX_UDP_DNS_LENGTH);

        try (DatagramChannel channel = DatagramChannel.open()) {
            System.out.println("Querying OpenDNS for " + domainName + " A record(s)");
            channel.connect(new InetSocketAddress(Inet4Address.getByAddress(openDnsIp), 53));
            channel.write(builder.build());
            channel.receive(buffer);
        }

        DnsMessageOverlay overlay = new DnsMessageOverlay(buffer);
        Assert.assertEquals(DnsResponseCode.NO_ERROR, overlay.getResponseCode());
        System.out.println((overlay.isAuthoritativeAnswer() ? "Authoritative" : "Non-authoritative") + " answer(s):");
        for (DnsRecord answer: overlay.getAnswers()) {
            System.out.print(answer.getRecordName() + "\t" + answer.getRecordType().toString() + "\t" +
                    answer.getTtl() + "\t");
            for (byte b: answer.getResourceData()) {
                System.out.print((b & 0xff) + ".");
            }
            System.out.println();
        }

    }

    @Test
    public void testDnsServerConcurrency() throws LifecycleException, InterruptedException, BrokenBarrierException {

        final int port = 8053;

        DnsEngine engine = new SingleTubeEngine();

        DatagramConnector connector = new DatagramConnector();
        connector.setPort(port);
        connector.start();

        DnsServer server = new DnsServer();
        server.addConnector(connector);
        server.setEngine(engine);
        server.start();

        int numThreads = 50;
        final CyclicBarrier startBarrier = new CyclicBarrier(numThreads);
        final CountDownLatch stopLatch = new CountDownLatch(numThreads);
        final byte[] serverIp = new byte[] { 127, 0, 0, 1 };

        final AtomicLong sendCount = new AtomicLong(0);
        final AtomicLong receiveCount = new AtomicLong(0);

        for (int i = 0; i < numThreads; i++) {
            Thread runner = new Thread(new Runnable() {
                @Override
                public void run() {

                    DnsQueryBuilder builder = new DnsQueryBuilder();
                    builder.setRecursionDesired(true)
                            .setOperationCode(DnsOpCode.QUERY)
                            .addQuestion(new DnsQuestion("www.cnn.com", DnsRecordType.A, DnsRecordClass.IN));
                    ByteBuffer query = builder.build();

                    try (DatagramChannel channel = DatagramChannel.open()) {

                        SocketAddress serverAddress = new InetSocketAddress(Inet4Address.getByAddress(serverIp), port);
                        channel.connect(serverAddress);
                        startBarrier.await();

                        channel.write(query);
                        sendCount.incrementAndGet();

                        ByteBuffer responseBuffer = ByteBuffer.allocateDirect(DnsMessageOverlay.MAX_UDP_DNS_LENGTH);
                        channel.receive(responseBuffer);
                        receiveCount.incrementAndGet();

                        stopLatch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assert.fail();
                    }
                }
            });
            runner.start();
        }

        stopLatch.await(5, TimeUnit.SECONDS);

        System.out.println("Test Send Count: " + sendCount.get() + " Receive Count: " + receiveCount.get());
        System.out.println("Connector Receive Count: " + connector.getReceiveCount() + " Send Count: " +
                connector.getSendCount());
        ThreadPoolExecutor executor = (ThreadPoolExecutor) server.getExecutor();
        System.out.println("Active Count: " + executor.getActiveCount() + " Completed Tasks: " +
                executor.getCompletedTaskCount() + " Task Count: " + executor.getTaskCount());

        Assert.assertEquals("Test unable to send all requests", numThreads, sendCount.get());
        Assert.assertEquals("Server appears to be dropping packets", sendCount.get(), connector.getReceiveCount());
        Assert.assertEquals("Server didn't deliver enough responses", receiveCount.get(), connector.getSendCount());

        server.stop();
        connector.stop();

    }

}
