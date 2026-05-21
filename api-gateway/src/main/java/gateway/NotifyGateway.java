package gateway;

import app.HtmlUtil;
import client.ServiceClient;
import client.ServiceUrls;
import com.fasterxml.jackson.core.type.TypeReference;
import gateway.dto.ListingWithPostcodeDto;
import gateway.dto.PurchaserWithPreferencesDto;
import io.javalin.http.Context;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Cross-context orchestration. In the monolith this was a single 4-table SQL JOIN
 * (users + user_preferences + sales + listings) in UserDAO.getNotificationsForAllUsers().
 * That violates bounded contexts, so it moves here, where:
 *
 *   1. Purchasers server returns purchasers + their postcode prefs (within its context)
 *   2. Property server returns listings by postcode (within its context, joining sales+listings)
 *   3. Gateway stitches the two results in memory.
 *
 * Property lookups are run in parallel (scatter-gather) using HttpClient.sendAsync.
 */
public class NotifyGateway {

    private static final int HTML_LISTING_THRESHOLD = 1000;

    private final ServiceClient client;
    private final ServiceUrls urls;

    public NotifyGateway(ServiceClient client, ServiceUrls urls) {
        this.client = client;
        this.urls = urls;
    }

    public void notify(Context ctx) {
        // Step 1: who wants to hear about what postcodes?
        List<PurchaserWithPreferencesDto> purchasers = client.getJsonList(
            urls.purchasers + "/purchaser/with-postcode-prefs",
            new TypeReference<>() {});

        // Step 2: gather the unique set of postcodes to fetch
        Set<String> uniquePostcodes = purchasers.stream()
            .flatMap(p -> p.postcodes.stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        // Step 3: scatter — fetch listings per postcode in parallel
        Map<String, CompletableFuture<List<ListingWithPostcodeDto>>> futures = new HashMap<>();
        for (String pc : uniquePostcodes) {
            futures.put(pc, CompletableFuture.supplyAsync(() ->
                client.getJsonList(urls.property + "/listing/postcode/" + pc,
                    new TypeReference<>() {})));
        }

        // Step 4: gather — await all and assemble
        Map<String, List<ListingWithPostcodeDto>> byPostcode = new HashMap<>();
        for (Map.Entry<String, CompletableFuture<List<ListingWithPostcodeDto>>> e : futures.entrySet()) {
            try {
                byPostcode.put(e.getKey(), e.getValue().join());
            } catch (Exception ex) {
                byPostcode.put(e.getKey(), List.of());
            }
        }

        // Step 5: stitch purchasers to their matching listings
        List<PurchaserNotification> result = new ArrayList<>();
        int totalListings = 0;
        for (PurchaserWithPreferencesDto p : purchasers) {
            PurchaserNotification n = new PurchaserNotification();
            n.firstName = p.firstName;
            n.lastName = p.lastName;
            n.email = p.email;
            for (String pc : p.postcodes) {
                for (ListingWithPostcodeDto l : byPostcode.getOrDefault(pc, List.of())) {
                    NotificationLine line = new NotificationLine();
                    line.propertyId = l.propertyId;
                    line.price = l.price;
                    n.listings.add(line);
                    totalListings++;
                }
            }
            if (!n.listings.isEmpty()) result.add(n);
        }

        if (totalListings > HTML_LISTING_THRESHOLD) {
            ctx.contentType("text/plain").result(renderPlainText(result));
        } else {
            ctx.html(renderHtml(result));
        }
    }

    private String renderHtml(List<PurchaserNotification> toNotify) {
        StringBuilder sb = new StringBuilder(HtmlUtil.pageHeader("Purchaser Notifications"));
        if (toNotify.isEmpty()) sb.append("<p>No matching listings for any purchaser.</p>");
        for (PurchaserNotification n : toNotify) {
            sb.append("<h2>").append(HtmlUtil.escape(n.firstName))
              .append(' ').append(HtmlUtil.escape(n.lastName)).append("</h2><ul>");
            for (NotificationLine l : n.listings) {
                sb.append("<li>Property ").append(l.propertyId)
                  .append(" — $").append(l.price).append("</li>");
            }
            sb.append("</ul>");
        }
        sb.append(HtmlUtil.pageFooter());
        return sb.toString();
    }

    private String renderPlainText(List<PurchaserNotification> toNotify) {
        StringBuilder sb = new StringBuilder();
        for (PurchaserNotification n : toNotify) {
            sb.append(n.firstName).append(' ').append(n.lastName).append('\n');
            for (NotificationLine l : n.listings) {
                sb.append("  property=").append(l.propertyId)
                  .append(" price=").append(l.price).append('\n');
            }
        }
        return sb.toString();
    }

    private static class PurchaserNotification {
        String firstName;
        String lastName;
        String email;
        List<NotificationLine> listings = new ArrayList<>();
    }

    private static class NotificationLine {
        long propertyId;
        long price;
    }
}