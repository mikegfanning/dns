package org.code_revue.dns.servlet;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Fanning
 */
public class TomcatServer {

    public static final int DEFAULT_HTTP_PORT = 80;

    private HttpServlet servlet;
    private Tomcat server;
    private int port = DEFAULT_HTTP_PORT;

    private volatile boolean running = false;

    /**
     * Start the embedded Tomcat server.
     */
    public void start() throws LifecycleException {
        if (running) {
            throw new IllegalStateException("Server is already running.");
        }

        server = new Tomcat();
        server.setPort(port);

        File base = new File(System.getProperty("java.io.tmpdir"));
        Context context = server.addContext("/", base.getAbsolutePath());
        Tomcat.addServlet(context, servlet.getServletName(), servlet);
        context.addServletMapping("/", servlet.getServletName());

        server.start();
        running = true;
        server.getServer().await();
    }

    /**
     * Stop the embedded Tomcat server.
     * @throws LifecycleException
     */
    public void stop() throws LifecycleException {
        if (!running) {
            throw new IllegalStateException("Server is not running.");
        }

        running = false;
        server.stop();
    }

    public HttpServlet getServlet() {
        return servlet;
    }

    public void setServlet(HttpServlet servlet) {
        this.servlet = servlet;
    }

    public Tomcat getServer() {
        return server;
    }

    public void setServer(Tomcat server) {
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
