package org.code_revue.dns.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger logger = LoggerFactory.getLogger(RedirectServlet.class);

    private final String servletName;

    private String redirectUrl;

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
        logger.debug("Redirecting request {} from {} to {}", req.getRequestURI(), req.getRemoteAddr(), redirectUrl);
        res.sendRedirect(redirectUrl);
    }

    /**
     * Get the redirect URL returned by this servlet.
     * @return Redirect URL
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * Sets the redirect URL returned by this servlet.
     * @param redirectUrl
     */
    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

}
