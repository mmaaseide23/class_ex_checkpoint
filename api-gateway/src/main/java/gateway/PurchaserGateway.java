package gateway;

import client.ServiceClient;
import client.ServiceUrls;
import io.javalin.http.Context;

import java.util.Map;

/** Pure passthrough for purchaser CRUD. */
public class PurchaserGateway {

    private final ServiceClient client;
    private final ServiceUrls urls;

    public PurchaserGateway(ServiceClient client, ServiceUrls urls) {
        this.client = client;
        this.urls = urls;
    }

    public void register(Context ctx) {
        Map<String, Object> body = parseBody(ctx);
        var res = client.postJson(urls.purchasers + "/purchaser", body);
        ctx.status(res.statusCode()).contentType("application/json").result(res.body());
    }

    public void get(Context ctx) {
        var dto = client.getJson(
            urls.purchasers + "/purchaser/" + ctx.pathParam("id"), Object.class);
        ctx.status(200).json(dto);
    }

    public void addPreference(Context ctx) {
        Map<String, Object> body = parseBody(ctx);
        var res = client.postJson(
            urls.purchasers + "/purchaser/" + ctx.pathParam("id") + "/preference", body);
        ctx.status(res.statusCode()).contentType("application/json").result(res.body());
    }

    public void getPreferences(Context ctx) {
        var dto = client.getJson(
            urls.purchasers + "/purchaser/" + ctx.pathParam("id") + "/preference",
            Object.class);
        ctx.status(200).json(dto);
    }

    public void deletePreference(Context ctx) {
        var res = client.delete(
            urls.purchasers + "/purchaser/" + ctx.pathParam("purchaserId")
            + "/preference/" + ctx.pathParam("id"));
        ctx.status(res.statusCode()).contentType("application/json").result(res.body());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(Context ctx) {
        try {
            return ServiceClient.mapper().readValue(ctx.body(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid request body", e);
        }
    }
}