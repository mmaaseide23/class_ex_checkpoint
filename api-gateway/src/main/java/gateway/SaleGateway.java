package gateway;

import app.HtmlUtil;
import client.ServiceClient;
import client.ServiceException;
import client.ServiceUrls;
import com.fasterxml.jackson.core.type.TypeReference;
import gateway.dto.SaleDto;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates sale lookups:
 *  - calls Property to get the data
 *  - fires async to Analytics to record the access (this is the code that USED to live
 *    inside SaleController as accessCounts.increment(...). It moved out because the
 *    Property server is forbidden from touching analytics data.)
 */
public class SaleGateway {

    private final ServiceClient client;
    private final ServiceUrls urls;

    public SaleGateway(ServiceClient client, ServiceUrls urls) {
        this.client = client;
        this.urls = urls;
    }

    public void getByPropertyId(Context ctx) {
        String propertyId = ctx.pathParam("propertyID");
        try {
            SaleDto sale = client.getJson(urls.property + "/sale/" + propertyId, SaleDto.class);
            recordAccessAsync("property", propertyId);
            ctx.status(200).html(saleHtml("Sale " + propertyId, List.of(sale)));
        } catch (ServiceException e) {
            ctx.status(e.statusCode).html(HtmlUtil.errorPage("Sale not found"));
        }
    }

    public void getAll(Context ctx) {
        StringBuilder qs = new StringBuilder();
        if (ctx.queryParam("minPrice") != null) {
            qs.append("minPrice=").append(ctx.queryParam("minPrice"));
        }
        if (ctx.queryParam("maxPrice") != null) {
            if (qs.length() > 0) qs.append('&');
            qs.append("maxPrice=").append(ctx.queryParam("maxPrice"));
        }
        String url = urls.property + "/sale" + (qs.length() > 0 ? "?" + qs : "");
        try {
            List<SaleDto> all = client.getJsonList(url, new TypeReference<>() {});
            ctx.status(200).html(saleHtml("All Sales", all));
        } catch (ServiceException e) {
            ctx.status(e.statusCode).html(HtmlUtil.errorPage("No sales found"));
        }
    }

    public void getByPostcode(Context ctx) {
        String postcode = ctx.pathParam("postcode");
        try {
            List<SaleDto> sales = client.getJsonList(
                urls.property + "/sale/postcode/" + postcode,
                new TypeReference<>() {});
            recordAccessAsync("postcode", postcode);
            ctx.status(200).html(saleHtml("Sales in Postcode " + postcode, sales));
        } catch (ServiceException e) {
            ctx.status(e.statusCode).html(HtmlUtil.errorPage("No sales for postcode " + postcode));
        }
    }

    public void create(Context ctx) {
        Map<String, Object> body = parseBody(ctx);
        var res = client.postJson(urls.property + "/sale", body);
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

    private String saleHtml(String title, List<SaleDto> rows) {
        StringBuilder sb = new StringBuilder(HtmlUtil.pageHeader(title));
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
        sb.append("<tr><th>ID</th><th>Council</th><th>Price</th><th>Address</th>")
          .append("<th>Postcode</th><th>Type</th><th>Area</th><th>Contract Date</th>")
          .append("<th>Zoning</th><th>Purpose</th></tr>");
        for (SaleDto s : rows) {
            sb.append("<tr>")
              .append("<td>").append(s.propertyId).append("</td>")
              .append("<td>").append(HtmlUtil.escape(s.councilName)).append("</td>")
              .append("<td>").append(s.purchasePrice).append("</td>")
              .append("<td>").append(HtmlUtil.escape(s.address)).append("</td>")
              .append("<td>").append(HtmlUtil.escape(s.postCode)).append("</td>")
              .append("<td>").append(HtmlUtil.escape(s.propertyType)).append("</td>")
              .append("<td>").append(s.area).append("</td>")
              .append("<td>").append(s.contractDate).append("</td>")
              .append("<td>").append(HtmlUtil.escape(s.zoning)).append("</td>")
              .append("<td>").append(HtmlUtil.escape(s.primaryPurpose)).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>").append(HtmlUtil.pageFooter());
        return sb.toString();
    }
}