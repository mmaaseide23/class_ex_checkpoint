package app;

public class HtmlUtil {

    public static String errorPage(String message) {
        return "<!DOCTYPE html><html><head><title>Error</title></head><body>"
             + "<h1>Error</h1><p>" + message + "</p></body></html>";
    }

    public static String pageHeader(String title) {
        return "<!DOCTYPE html><html><head><title>" + title + "</title></head><body>"
             + "<h1>" + title + "</h1>";
    }

    public static String pageFooter() {
        return "</body></html>";
    }
}
