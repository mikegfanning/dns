package org.code_revue.dns.server;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * This class contains all of the information about a DNS request or response, namely the address and the binary data
 * for the message. Note that this class is mutable, and the same payload object can be used for both request and
 * response.
 *
 * @author Mike Fanning
 * @see org.code_revue.dns.message.DnsMessageOverlay
 */
public class DnsPayload {

    private SocketAddress remoteAddress;

    private ByteBuffer messageData;

    /**
     * Creates a new payload from the supplied address and message data.
     * @param remoteAddress
     * @param messageData
     */
    public DnsPayload(SocketAddress remoteAddress, ByteBuffer messageData) {
        this.remoteAddress = remoteAddress;
        this.messageData = messageData;
    }

    /**
     * Get the address for this payload.
     * @return
     */
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Set the remote address for this payload.
     * @param remoteAddress
     */
    public void setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    /**
     * Get the binary DNS message data for this payload.
     * @return
     */
    public ByteBuffer getMessageData() {
        return messageData;
    }

    /**
     * Set the binary DNS message data for this payload.
     * @param messageData
     */
    public void setMessageData(ByteBuffer messageData) {
        this.messageData = messageData;
    }

}
