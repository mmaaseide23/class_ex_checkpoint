package propertyListing;

import io.javalin.http.Context;

import java.util.List;

public class PropertyListingController {

    private final PropertyListingDAO listings;

    public PropertyListingController(PropertyListingDAO listings) {
        this.listings = listings;
    }

    // implements POST /listing
    public void createListing(Context ctx) {
        PropertyListing listing = ctx.bodyValidator(PropertyListing.class).get();

        if (listings.newListing(listing)) {
            ctx.result("Listing Created");
            ctx.status(201);
        } else {
            ctx.result("Failed to add listing");
            ctx.status(400);
        }
    }

    // implements GET /listing
    public void getAllListings(Context ctx) {
        List<PropertyListing> all = listings.getAllListings();
        if (all.isEmpty()) {
            ctx.html(errorHtml("No Listings Found"));
            ctx.status(404);
        } else {
            ctx.html(listingsHtml("All Listings", all));
            ctx.status(200);
        }
    }

    // implements GET /listing/{propertyID}
    public void getListingsByProperty(Context ctx, String propertyID) {
        List<PropertyListing> history = listings.getListingsByPropertyId(propertyID);
        if (history.isEmpty()) {
            ctx.html(errorHtml("No listings for property " + propertyID));
            ctx.status(404);
        } else {
            ctx.html(listingsHtml("Listing History for Property " + propertyID, history));
            ctx.status(200);
        }
    }

    private String listingsHtml(String title, List<PropertyListing> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>").append(title).append("</title></head><body>");
        sb.append("<h1>").append(title).append("</h1>");
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
        sb.append("<tr><th>Listing ID</th><th>Property ID</th><th>Listing Date</th><th>Price</th></tr>");
        for (PropertyListing pl : rows) {
            sb.append("<tr>")
              .append("<td>").append(pl.listingID).append("</td>")
              .append("<td>").append(pl.propertyID).append("</td>")
              .append("<td>").append(pl.listingDate).append("</td>")
              .append("<td>").append(pl.price).append("</td>")
              .append("</tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String errorHtml(String message) {
        return "<!DOCTYPE html><html><head><title>Error</title></head><body>"
             + "<h1>Error</h1><p>" + message + "</p></body></html>";
    }
}