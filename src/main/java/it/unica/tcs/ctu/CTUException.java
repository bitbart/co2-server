package it.unica.tcs.ctu;

public class CTUException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CTUException() {
        super();
    }

    public CTUException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CTUException(String message, Throwable cause) {
        super(message, cause);
    }

    public CTUException(String message) {
        super(message);
    }

    public CTUException(Throwable cause) {
        super(cause);
    }
}