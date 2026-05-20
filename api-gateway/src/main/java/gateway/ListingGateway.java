package gateway;

import app.HtmlUtil;
import client.ServiceClient;
import client.ServiceException;
import client.ServiceUrls;
import com.fasterxml.jackson.core.type.TypeReference;
import gateway.dto.ListingDto;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListingGateway {

    private final ServiceClient client;
    private final ServiceUrls urls;

    public ListingGateway(ServiceClient client, ServiceUrls urls) {
        this.client = client;
        this.urls = urls;
    }

    public void getAll(Context ctx) {
        try {
            List<ListingDto> all = client.getJsonList(
                urls.property + "/listing", new TypeReference<>() {});
            ctx.status(200).html(listingHtml("All Listings", all));
        } catch (ServiceException e) {
            ctx.status(e.statusCode).html(HtmlUtil.errorPage("No listings found"));
        }
    }

    public void getByProperty(Context ctx) {
        String propertyId = ctx.pathParam("propertyID");
        try {
            List<ListingDto> history = client.getJsonList(
                urls.property + "/listing/" + propertyId, new TypeReference<>() {});
            recordAccessAsync("property", propertyId);
            ctx.status(200).html(listingHtml("Listings for Property " + propertyId, history));
        } catch (ServiceException e) {
            ctx.status(e.statusCode).html(HtmlUtil.errorPage("No listings for property " + propertyId));
        }
    }

    public void create(Context ctx) {
        Map<String, Object> body = parseBody(ctx);
        var res = client.postJson(urls.property + "/listing", body);
        ctx.status(res.statusCode()).result(res.body());
    }

    private void recordAccessAsync(String type, String value) {
        Map<String, String> event = new HashMap<>();
        event.put("type", type);
        event.put("value", value);
        client.postJsonAsync(urls.analytics + "/analytics/access", event);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(Context ctx) {
        try {
            return ServiceClient.mapper().readValue(ctx.body(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid request body", e);
        }
    }

    private String listingHtml(String title, List<ListingDto> rows) {
        StringBuilder sb = new StringBuilder(HtmlUtil.pageHeader(title));
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
        sb.append("<tr><th>ID</th><th>Property ID</th><th>Listing Date</th><th>Price</th></tr>");
        for (ListingDto l : rows) {
            sb.append("<tr>")
              .append("<td>").append(l.id).append("</td>")
              .append("<td>").append(l.propertyId).append("</td>")
              .append("<td>").append(l.listingDate).append("</td>")
              .append("<td>").append(l.price).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>").append(HtmlUtil.pageFooter());
        return sb.toString();
    }
}