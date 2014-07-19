package org.code_revue.dns.util;

import java.nio.ByteBuffer;
import java.util.StringTokenizer;

/**
 * Utilities for manipulating byte arrays and buffers, particularly with respect to DNS (domain name encoding, etc.).
 *
 * @author Mike Fanning
 */
public class ByteBufferUtils {

    /**
     * Prints a hexadecimal representation of a buffer to standard out.
     *
     * @param buffer
     */
    public static void printByteBuffer(ByteBuffer buffer) {

        System.out.println("Position: " + buffer.position() + " Limit: " + buffer.limit() + " Capacity: " +
                buffer.capacity());

        int count = 0;
        while (count < buffer.capacity()) {
            if (count % 16 == 15) {
                System.out.println(String.format("%02x", buffer.get(count) & 0xff));
            } else if (count % 16 == 7) {
                System.out.print(String.format("%02x", buffer.get(count) & 0xff) + "  ");
            } else {
                System.out.print(String.format("%02x", buffer.get(count) & 0xff) + " ");
            }
            count++;
        }
        if (count % 16 != 15) {
            System.out.println();
        }
    }

    /**
     * Reads a DNS encoded domain name from a buffer and returns it as a string, as described
     * <a href="http://www.tcpipguide.com/free/t_DNSNameNotationandMessageCompressionTechnique.htm">here</a>.
     *
     * @see <a href="http://www.tcpipguide.com/free/t_DNSNameNotationandMessageCompressionTechnique.htm">
     *     http://www.tcpipguide.com/free/t_DNSNameNotationandMessageCompressionTechnique.htm</a>
     * @param buffer
     * @return Domain name
     */
    public static String readDomainName(ByteBuffer buffer) {
        int namePosition = 0;
        byte segmentLength = buffer.get();
        byte[] nameBuffer = new byte[256];
        while (segmentLength != 0) {
            buffer.get(nameBuffer, namePosition, segmentLength);
            namePosition += segmentLength;
            segmentLength = buffer.get();

            if (segmentLength != 0) {
                nameBuffer[namePosition] = '.';
                namePosition++;
            }
        }
        return new String(nameBuffer, 0, namePosition);
    }

    /**
     * Writes a domain name to a buffer using DNS encoding, as described
     * <a href="http://www.tcpipguide.com/free/t_DNSNameNotationandMessageCompressionTechnique.htm">here</a>.
     * @see <a href="http://www.tcpipguide.com/free/t_DNSNameNotationandMessageCompressionTechnique.htm">
     *     http://www.tcpipguide.com/free/t_DNSNameNotationandMessageCompressionTechnique.htm</a>
     * @param domainName
     * @param buffer
     */
    public static void writeDomainName(String domainName, ByteBuffer buffer) {
        StringTokenizer tokenizer = new StringTokenizer(domainName, ".@");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            buffer.put((byte) token.length());
            buffer.put(token.getBytes());
        }
        buffer.put((byte) 0);
    }

    /**
     * Encodes a domain name using DNS encoding, as described
     * <a href="http://www.tcpipguide.com/free/t_DNSNameNotationandMessageCompressionTechnique.htm">here</a> and
     * returns it as a byte array.
     *
     * @param domainName
     * @return Encoded domain name
     */
    public static byte[] encodeDomainName(String domainName) {

        byte[] result = new byte[domainName.length() + 2];
        int position = 0;
        StringTokenizer tokenizer = new StringTokenizer(domainName, ".@");

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            result[position] = (byte) token.length();
            position++;
            for (byte b: token.getBytes()) {
                result[position] = b;
                position++;
            }
        }

        result[position] = (byte) 0;
        return result;
    }

}
