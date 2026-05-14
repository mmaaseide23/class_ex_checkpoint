package propertyListing;

import app.HtmlUtil;
import io.javalin.http.Context;

import java.util.List;

public class PropertyListingController {

    private final PropertyListingDAO listings;

    public PropertyListingController(PropertyListingDAO listings) {
        this.listings = listings;
    }

    public void createListing(Context ctx) {
        PropertyListing listing = ctx.bodyValidator(PropertyListing.class).get();

        if (listings.newListing(listing)) {
            ctx.status(201).result("Listing Created");
        } else {
            ctx.status(400).result("Failed to add listing");
        }
    }

    public void getAllListings(Context ctx) {
        List<PropertyListing> all = listings.getAllListings();
        if (all.isEmpty()) {
            ctx.status(404).html(HtmlUtil.errorPage("No Listings Found"));
        } else {
            ctx.status(200).html(listingTableHtml("All Listings", all));
        }
    }

    public void getListingsByProperty(Context ctx, String propertyId) {
        List<PropertyListing> history = listings.getListingsByPropertyId(propertyId);
        if (history.isEmpty()) {
            ctx.status(404).html(HtmlUtil.errorPage("No listings for property " + propertyId));
        } else {
            ctx.status(200).html(listingTableHtml("Listing History for Property " + propertyId, history));
        }
    }

    private String listingTableHtml(String title, List<PropertyListing> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtil.pageHeader(title));
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
        sb.append("<tr><th>ID</th><th>Property ID</th><th>Listing Date</th><th>Price</th></tr>");
        for (PropertyListing pl : rows) {
            sb.append("<tr>")
              .append("<td>").append(pl.id).append("</td>")
              .append("<td>").append(pl.propertyId).append("</td>")
              .append("<td>").append(pl.listingDate).append("</td>")
              .append("<td>").append(pl.price).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>");
        sb.append(HtmlUtil.pageFooter());
        return sb.toString();
    }
}
