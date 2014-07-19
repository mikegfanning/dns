package org.code_revue.dns;

import org.code_revue.dns.server.DnsServer;
import org.code_revue.dns.server.connector.DatagramConnector;
import org.code_revue.dns.server.engine.*;
import org.code_revue.dns.server.exception.LifecycleException;
import org.code_revue.dns.server.resolver.LocalhostResolver;
import org.code_revue.dns.util.ThreadUtils;

import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Simple application that wires together the necessary components for a {@link org.code_revue.dns.server.DnsServer} and
 * starts them. The main thread reads commands from standard in and can be used to manage the server (well, sort of).
 *
 * @author Mike Fanning
 */
public class DnsApp {

    public static void main(String... args) throws LifecycleException, UnknownHostException {

        DatagramConnector connector = new DatagramConnector();
        connector.setPort(53);

        LocalhostResolver resolver = new LocalhostResolver();
        resolver.addException("vimeo.com");

        ResolverRule rule1 = new AddressRegexResolverRule(".*192\\.168.*", resolver);
        ResolverRule rule2 = new AddressRegexResolverRule(".*127\\.0\\.0\\.1.*", resolver);

        ResolverChain chain = new ResolverChain();
        chain.addRule(rule1);
        chain.addRule(rule2);

        StandardEngine engine = new StandardEngine(new byte[] { (byte) 208, (byte) 67, (byte) 222, (byte) 222});
        engine.setResolverChain(chain);

        DnsServer server = new DnsServer();
        server.addConnector(connector);
        server.setEngine(engine);

        System.out.println("Starting engine...");
        engine.start();

        System.out.println("Starting connector...");
        connector.start();

        System.out.println("Starting server...");
        server.start();

        Scanner scanner = new Scanner(System.in);
        String command;
        System.out.print("Enter Command: ");
        while ((command = scanner.nextLine()) != null) {
            if ("exit".equals(command)) {
                System.out.println("Stopping server...");
                server.stop();

                System.out.println("Stopping connector...");
                connector.stop();

                System.out.println("Stopping engine...");
                engine.stop();

                break;
            } else if ("threads".equals(command)) {
                ThreadUtils.printThreadInfo();
            } else {
                System.out.println("Command not found.");
            }
            System.out.print("Enter Command: ");
        }

    }

}
