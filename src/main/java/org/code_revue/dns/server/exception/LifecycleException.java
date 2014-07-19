package org.code_revue.dns.server.exception;

/**
 * Exception thrown when starting or stopping components of this project. Probably a dumb idea.
 *
 * @author Mike Fanning
 */
public class LifecycleException extends Exception {

    public LifecycleException() {
        super();
    }

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(String message, Throwable t) {
        super(message, t);
    }

}
