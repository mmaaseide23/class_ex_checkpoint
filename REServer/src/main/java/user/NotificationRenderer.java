package user;

import java.util.List;

public class NotificationRenderer {

    public static String renderHtml(List<UserNotification> toNotify) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
          .append("<title>Purchaser Notifications</title></head><body>")
          .append("<h1>Purchaser Notifications</h1>");
        if (toNotify.isEmpty()) {
            sb.append("<p>No matching listings for any user.</p>");
        }
        for (UserNotification n : toNotify) {
            sb.append("<h2>").append(escape(n.firstName)).append(' ').append(escape(n.lastName)).append("</h2>");
            sb.append("<ul>");
            for (UserNotification.MatchingListing ml : n.listings) {
                sb.append("<li>Property ").append(ml.propertyId)
                  .append(" — $").append(ml.price).append("</li>");
            }
            sb.append("</ul>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    public static String renderPlainText(List<UserNotification> toNotify) {
        StringBuilder sb = new StringBuilder();
        for (UserNotification n : toNotify) {
            sb.append(n.firstName).append(' ').append(n.lastName).append('\n');
            for (UserNotification.MatchingListing ml : n.listings) {
                sb.append("  property=").append(ml.propertyId)
                  .append(" price=").append(ml.price).append('\n');
            }
        }
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}