package app;

import io.javalin.Javalin;
import property.PropertyDAO;
import property.PropertyController;
import propertyListing.PropertyListingDAO;
import propertyListing.PropertyListingController;
import user.UserDAO;
import user.UserController;

public class REServer {

    public static void main(String[] args) {
        var propertyDAO = new PropertyDAO();
        var propertyHandler = new PropertyController(propertyDAO);

        var listingDAO = new PropertyListingDAO();
        var listingHandler = new PropertyListingController(listingDAO);

        var userDAO = new UserDAO();
        var userHandler = new UserController(userDAO);

        var app = Javalin.create()
                .get("/", ctx -> ctx.result("Real Estate server is running"))
                .start(7070);

        app.get("/property/{propertyID}", ctx -> {
            propertyHandler.getPropertyByID(ctx, ctx.pathParam("propertyID"));
        });
        app.get("/property", ctx -> {
            propertyHandler.getAllProperties(ctx);
        });
        app.post("/property", ctx -> {
            propertyHandler.createProperty(ctx);
        });
        app.get("/property/postcode/{postcode}", ctx -> {
            propertyHandler.findPropertyByPostCode(ctx, ctx.pathParam("postcode"));
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
    }
}
