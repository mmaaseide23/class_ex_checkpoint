package app;

public class HtmlUtil {

    public static String errorPage(String message) {
        return "<!DOCTYPE html><html><head><title>Error</title></head><body>"
             + "<h1>Error</h1><p>" + escape(message) + "</p></body></html>";
    }

    public static String pageHeader(String title) {
        return "<!DOCTYPE html><html><head><title>" + escape(title) + "</title></head><body>"
             + "<h1>" + escape(title) + "</h1>";
    }

    public static String pageFooter() {
        return "</body></html>";
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}