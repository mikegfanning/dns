package org.code_revue.dns.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Simple servlet that will always redirect to the same URL.
 *
 * @author Mike Fanning
 */
public class RedirectServlet extends HttpServlet {

    private final String redirectUrl;
    private final String servletName;

    /**
     * Creates a new servlet that redirects all requests to supplied URL.
     * @param redirectUrl
     * @param servletName
     */
    public RedirectServlet(String redirectUrl, String servletName) {
        this.redirectUrl = redirectUrl;
        this.servletName = servletName;
    }

    @Override
    public String getServletName() {
        return servletName;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
        res.sendRedirect(redirectUrl);
    }

}
