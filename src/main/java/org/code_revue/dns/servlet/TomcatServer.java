package org.code_revue.dns.servlet;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import java.io.File;

/**
 * @author Mike Fanning
 */
public class TomcatServer {

    private final Logger logger = LoggerFactory.getLogger(TomcatServer.class);

    public static final int DEFAULT_HTTP_PORT = 80;

    private HttpServlet servlet;
    private Tomcat server;
    private int port = DEFAULT_HTTP_PORT;

    private volatile boolean running = false;

    /**
     * Start the embedded Tomcat server.
     */
    public void start() throws LifecycleException {

        logger.info("Starting Tomcat Server");

        if (running) {
            throw new IllegalStateException("Server is already running.");
        }

        server = new Tomcat();
        server.setPort(port);
        String workDir = (new File(System.getProperty("java.io.tmpdir"))).getAbsolutePath();
        server.setBaseDir(workDir);

        logger.debug("Adding context");
        Context context = server.addContext("/", workDir + File.separator + "webapp");

        logger.debug("Adding servlet {}", servlet.getServletName());
        Tomcat.addServlet(context, servlet.getServletName(), servlet);
        context.addServletMapping("/", servlet.getServletName());

        server.start();
        running = true;
    }

    /**
     * Stop the embedded Tomcat server.
     * @throws LifecycleException
     */
    public void stop() throws LifecycleException {

        logger.info("Stopping Tomcat Server");

        if (!running) {
            logger.warn("Tomcat Server is already stopped");
        } else {
            running = false;
            server.stop();
        }
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
