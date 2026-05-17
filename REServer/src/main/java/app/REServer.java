package app;

import analytics.AccessCountDAO;
import analytics.AnalyticsController;
import io.javalin.Javalin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import listing.ListingDAO;
import listing.ListingController;
import sale.SaleDAO;
import sale.SaleController;
import user.UserDAO;
import user.UserController;

public class REServer {

    public static void main(String[] args) {
        var accessCountDAO = new AccessCountDAO();

        var saleDAO = new SaleDAO();
        var saleHandler = new SaleController(saleDAO, accessCountDAO);

        var listingDAO = new ListingDAO();
        var listingHandler = new ListingController(listingDAO, accessCountDAO);

        var userDAO = new UserDAO();
        var userHandler = new UserController(userDAO);

        var analyticsHandler = new AnalyticsController(accessCountDAO);

        var app = Javalin.create(config -> {
            config.registerPlugin(new OpenApiPlugin(openApiConfig -> {
                openApiConfig.withDefinitionConfiguration((version, definition) -> {
                    definition.withInfo(info -> {
                        info.setTitle("Real Estate API");
                        info.setVersion("1.0.0");
                        info.setDescription("NSW Real Estate listing and notification service");
                    });
                });
            }));
            config.registerPlugin(new SwaggerPlugin());
        })
        .get("/", ctx -> ctx.result("Real Estate server is running"))
        .start(7070);

        app.get("/sale/{propertyID}", ctx -> {
            saleHandler.getSaleByPropertyID(ctx, ctx.pathParam("propertyID"));
        });
        app.get("/sale", ctx -> {
            saleHandler.getAllSales(ctx);
        });
        app.post("/sale", ctx -> {
            saleHandler.createSale(ctx);
        });
        app.get("/sale/postcode/{postcode}", ctx -> {
            saleHandler.findSalesByPostCode(ctx, ctx.pathParam("postcode"));
        });

        app.post("/listing", ctx -> {
            listingHandler.createListing(ctx);
        });
        app.get("/listing", ctx -> {
            listingHandler.getAllListings(ctx);
        });
        app.get("/listing/{propertyID}", ctx -> {
            listingHandler.getListingsByProperty(ctx, ctx.pathParam("propertyID"));
        });

        app.get("/user/notify", ctx -> {
            userHandler.notify(ctx);
        });
        app.post("/user", ctx -> {
            userHandler.register(ctx);
        });
        app.get("/user/{id}", ctx -> {
            userHandler.getUser(ctx, ctx.pathParam("id"));
        });
        app.post("/user/{id}/preference", ctx -> {
            userHandler.addPreference(ctx, ctx.pathParam("id"));
        });
        app.get("/user/{id}/preference", ctx -> {
            userHandler.getPreferences(ctx, ctx.pathParam("id"));
        });
        app.delete("/user/{userId}/preference/{id}", ctx -> {
            userHandler.deletePreference(ctx, ctx.pathParam("id"));
        });

        app.get("/analytics", ctx -> {
            analyticsHandler.getAll(ctx);
        });
        app.get("/analytics/top/property", ctx -> {
            analyticsHandler.getTopProperties(ctx);
        });
        app.get("/analytics/top/postcode", ctx -> {
            analyticsHandler.getTopPostcodes(ctx);
        });
    }
}
