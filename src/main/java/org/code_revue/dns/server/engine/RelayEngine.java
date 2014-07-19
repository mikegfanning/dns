package org.code_revue.dns.server.engine;

import org.code_revue.dns.message.DnsMessageOverlay;
import org.code_revue.dns.message.DnsResponseBuilder;
import org.code_revue.dns.message.DnsResponseCode;
import org.code_revue.dns.server.DnsPayload;
import org.code_revue.dns.server.exception.LifecycleException;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Engine that relays all queries to another server and passes responses off as its own. Mostly used for testing.
 *
 * @author Mike Fanning
 */
public class RelayEngine implements DnsEngine {

    private boolean running = false;
    private byte[] dnsServerIp;
    private DatagramChannel channel;

    public RelayEngine(byte[] dnsServerIp) {
        this.dnsServerIp = dnsServerIp;
    }

    public void start() throws LifecycleException {
        if (running) {
            throw new LifecycleException("Engine is already running");
        }

        try {
            channel = DatagramChannel.open();
            channel.connect(new InetSocketAddress(Inet4Address.getByAddress(dnsServerIp), 53));
            running = true;
        } catch (UnknownHostException e) {
            throw new LifecycleException("Could not connect to server IP address", e);
        } catch (IOException e) {
            throw new LifecycleException("Could not open DatagramChannel", e);
        }
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Send DNS message data to external server and pass response back to caller.
     * @param payload Query
     * @return
     */
    @Override
    public DnsPayload processDnsPayload(DnsPayload payload) {
        try {
            channel.write(payload.getMessageData());
            ByteBuffer response = ByteBuffer.allocateDirect(DnsMessageOverlay.MAX_UDP_DNS_LENGTH);
            channel.receive(response);
            response.limit(response.position());
            response.position(0);
            payload.setMessageData(response.slice());
            return payload;
        } catch (IOException e) {
            e.printStackTrace();
        }

        DnsResponseBuilder builder = new DnsResponseBuilder(payload.getMessageData());
        builder.setResponseCode(DnsResponseCode.SERVER_FAILURE)
                .setRecursionAvailable(true);
        payload.setMessageData(builder.build());
        return payload;
    }

    public void stop() throws LifecycleException {
        if (!running) {
            throw new LifecycleException("Engine is not running");
        }

        running = false;
        try {
            channel.close();
        } catch (IOException e) {
            throw new LifecycleException("Error closing DatagramChannel", e);
        }
    }

}