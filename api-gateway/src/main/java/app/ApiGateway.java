package app;

import client.ServiceClient;
import client.ServiceUrls;
import gateway.AnalyticsGateway;
import gateway.ListingGateway;
import gateway.NotifyGateway;
import gateway.PurchaserGateway;
import gateway.SaleGateway;
import io.javalin.Javalin;

public class ApiGateway {

    public static void main(String[] args) {
        int port = port();
        ServiceUrls urls = ServiceUrls.fromEnv();
        ServiceClient client = new ServiceClient();

        SaleGateway sale = new SaleGateway(client, urls);
        ListingGateway listing = new ListingGateway(client, urls);
        PurchaserGateway purchaser = new PurchaserGateway(client, urls);
        AnalyticsGateway analytics = new AnalyticsGateway(client, urls);
        NotifyGateway notify = new NotifyGateway(client, urls);

        Javalin.create()
            .get("/", ctx -> ctx.result("API Gateway is running"))

            // Sale (orchestrates Property + fire-and-forget Analytics)
            .get("/sale", sale::getAll)
            .post("/sale", sale::create)
            .get("/sale/{propertyID}", sale::getByPropertyId)
            .get("/sale/postcode/{postcode}", sale::getByPostcode)

            // Listing (orchestrates Property + fire-and-forget Analytics)
            .get("/listing", listing::getAll)
            .post("/listing", listing::create)
            .get("/listing/{propertyID}", listing::getByProperty)

            // Purchaser (passthrough to Purchasers)
            .post("/purchaser", purchaser::register)
            .get("/purchaser/{id}", purchaser::get)
            .post("/purchaser/{id}/preference", purchaser::addPreference)
            .get("/purchaser/{id}/preference", purchaser::getPreferences)
            .delete("/purchaser/{purchaserId}/preference/{id}", purchaser::deletePreference)

            // Notify (scatter-gather across Purchasers + Property)
            .get("/notify", notify::notify)

            // Analytics (passthrough + scatter-gather for property details)
            .get("/analytics", analytics::getAll)
            .get("/analytics/top/property", analytics::topProperties)
            .get("/analytics/top/postcode", analytics::topPostcodes)

            .start(port);

        System.out.println("API Gateway listening on " + port);
        System.out.println("  property   -> " + urls.property);
        System.out.println("  purchasers -> " + urls.purchasers);
        System.out.println("  analytics  -> " + urls.analytics);
    }

    private static int port() {
        String p = System.getenv("GATEWAY_PORT");
        return (p == null || p.isBlank()) ? 7070 : Integer.parseInt(p);
    }
}