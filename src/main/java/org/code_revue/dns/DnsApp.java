package org.code_revue.dns;

import org.apache.catalina.LifecycleException;
import org.code_revue.dns.server.DnsServer;
import org.code_revue.dns.server.connector.DatagramConnector;
import org.code_revue.dns.server.engine.*;
import org.code_revue.dns.server.resolver.LocalhostResolver;
import org.code_revue.dns.servlet.RedirectServlet;
import org.code_revue.dns.servlet.TomcatServer;
import org.code_revue.dns.util.ThreadUtils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Simple application that wires together the necessary components for a {@link org.code_revue.dns.server.DnsServer} and
 * starts them. Also starts an embedded Tomcat server which redirects all HTTP reqeusts to a pre-defined URL. The main
 * thread reads commands from standard in and can be used to manage the server (well, sort of).
 *
 * @author Mike Fanning
 */
public class DnsApp {

    public static void main(String... args) throws IOException, LifecycleException {

        RedirectServlet servlet = new RedirectServlet("http://picard.ytmnd.com/", "lel");
        final TomcatServer httpServer = new TomcatServer();
        httpServer.setServlet(servlet);

        DatagramConnector connector = new DatagramConnector();
        connector.setPort(53);

        LocalhostResolver resolver = new LocalhostResolver();
        resolver.addException("ytmnd.com");

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

        httpServer.start();
        engine.start();
        connector.start();
        server.start();

        Scanner scanner = new Scanner(System.in);
        String command;
        System.out.print("Enter Command: ");
        while ((command = scanner.nextLine()) != null) {
            if ("exit".equals(command)) {
                server.stop();
                connector.stop();
                engine.stop();
                httpServer.stop();
                break;
            } else if ("threads".equals(command)) {
                ThreadUtils.printThreadInfo();
            } else if ("connectors".equals(command)) {
                System.out.println("Connectors");
                System.out.println("----------------------------");
                System.out.println(connector.getClass().getSimpleName() + "\tPort:\t" + connector.getPort() +
                        "\tReceive Count:\t" + connector.getReceiveCount() + "\tSend Count:\t" +
                        connector.getSendCount());
            } else if ("engine".equals(command)) {
                System.out.println("Engine Info");
                System.out.println("----------------------------");
                System.out.println("Payloads Processed:\t" + engine.getPayloadsProcessed());
                System.out.println("Processing Errors:\t" + engine.getProcessingErrors());
            } else if ("resolver chain".equals(command)) {
                System.out.println("Resolver Chain");
                System.out.println("----------------------------");
                int index = 0;
                for (ResolverRule rule: chain.getResolverRules()) {
                    if (AddressRegexResolverRule.class == rule.getClass()) {
                        AddressRegexResolverRule castRule = (AddressRegexResolverRule) rule;
                        System.out.println(index + ":\t" + rule.getClass().getSimpleName() + "\tRegex:\t" +
                                castRule.getRegex() + "\t->\t" + rule.getResolver().getClass().getSimpleName());
                    } else {
                        System.out.println(index + ":\t" + rule.getClass().getSimpleName() + "\t->\t" +
                                rule.getResolver().getClass().getSimpleName());
                    }
                    index++;
                }
            } else if (command.startsWith("add ")) {
                command = command.replaceFirst("add ", "").trim();
                if (command.startsWith("exception ")) {
                    String domain = command.replaceFirst("exception ", "").trim();
                    resolver.addException(domain);
                } else {
                    System.out.println("Command not found.");
                }
            } else if (command.startsWith("remove ")) {
                command = command.replaceFirst("remove ", "").trim();
                if (command.startsWith("rule ")) {
                    String indexStr = command.replaceFirst("rule ", "").trim();
                    try {
                        int index = Integer.parseInt(indexStr);
                        chain.removeRule(index);
                    } catch (Exception e) {
                        System.out.println("Invalid rule index.");
                    }
                } else {
                    System.out.println("Command not found.");
                }
            } else {
                System.out.println("Command not found.");
            }
            System.out.print("Enter Command: ");
        }

    }

}
