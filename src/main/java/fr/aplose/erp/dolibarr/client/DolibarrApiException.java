package fr.aplose.erp.dolibarr.client;

public class DolibarrApiException extends RuntimeException {

    public DolibarrApiException(String message) {
        super(message);
    }

    public DolibarrApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
