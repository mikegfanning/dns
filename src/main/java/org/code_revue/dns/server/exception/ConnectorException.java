package org.code_revue.dns.server.exception;

/**
 * Represents an exception at the connector level while sending and receiving data. Should probably rethink this whole
 * exception methodology.
 *
 * @author Mike Fanning
 */
public class ConnectorException extends Exception {

    public ConnectorException() {
        super();
    }

    public ConnectorException(String message) {
        super(message);
    }

    public ConnectorException(String message, Throwable t) {
        super(message, t);
    }

    public ConnectorException(Throwable t) {
        super(t);
    }

}
