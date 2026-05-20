package app;

import io.javalin.Javalin;
import listing.ListingController;
import listing.ListingDAO;
import sale.SaleController;
import sale.SaleDAO;

public class PropertyServer {

    public static void main(String[] args) {
        int port = port();

        var saleDAO = new SaleDAO();
        var saleController = new SaleController(saleDAO);

        var listingDAO = new ListingDAO();
        var listingController = new ListingController(listingDAO);

        Javalin.create()
            .get("/", ctx -> ctx.result("Property server is running"))
            // Sale
            .get("/sale", saleController::getAllSales)
            .post("/sale", saleController::createSale)
            .get("/sale/{propertyID}", ctx ->
                saleController.getSaleByPropertyID(ctx, ctx.pathParam("propertyID")))
            .get("/sale/postcode/{postcode}", ctx ->
                saleController.findSalesByPostCode(ctx, ctx.pathParam("postcode")))
            // Listing
            .get("/listing", listingController::getAllListings)
            .post("/listing", listingController::createListing)
            .get("/listing/{propertyID}", ctx ->
                listingController.getListingsByProperty(ctx, ctx.pathParam("propertyID")))
            .get("/listing/postcode/{postcode}", ctx ->
                listingController.getListingsByPostCode(ctx, ctx.pathParam("postcode")))
            .start(port);

        System.out.println("Property server listening on " + port);
    }

    private static int port() {
        String p = System.getenv("PROPERTY_PORT");
        return (p == null || p.isBlank()) ? 7071 : Integer.parseInt(p);
    }
}