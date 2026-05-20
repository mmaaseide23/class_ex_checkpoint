package client;

public class ServiceException extends RuntimeException {
    public final int statusCode;
    public final String body;

    public ServiceException(String url, int statusCode, String body) {
        super("call to " + url + " returned " + statusCode + ": " + body);
        this.statusCode = statusCode;
        this.body = body;
    }
}