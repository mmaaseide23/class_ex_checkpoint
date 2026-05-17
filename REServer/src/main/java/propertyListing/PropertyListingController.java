package propertyListing;

import analytics.AccessCountDAO;
import app.HtmlUtil;
import io.javalin.http.Context;
import io.javalin.openapi.*;

import java.util.List;

public class PropertyListingController {

    private final PropertyListingDAO listings;
    private final AccessCountDAO accessCounts;

    public PropertyListingController(PropertyListingDAO listings, AccessCountDAO accessCounts) {
        this.listings = listings;
        this.accessCounts = accessCounts;
    }

    @OpenApi(
        path = "/listing",
        methods = HttpMethod.POST,
        summary = "Create a new property listing",
        operationId = "createListing",
        tags = {"Listing"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = PropertyListing.class)),
        responses = {
            @OpenApiResponse(status = "201", description = "Listing created"),
            @OpenApiResponse(status = "400", description = "Failed to add listing")
        }
    )
    public void createListing(Context ctx) {
        PropertyListing listing = ctx.bodyValidator(PropertyListing.class).get();

        if (listings.newListing(listing)) {
            ctx.status(201).result("Listing Created");
        } else {
            ctx.status(400).result("Failed to add listing");
        }
    }

    @OpenApi(
        path = "/listing",
        methods = HttpMethod.GET,
        summary = "Get all listings (max 100)",
        operationId = "getAllListings",
        tags = {"Listing"},
        responses = {
            @OpenApiResponse(status = "200", description = "Listings returned as HTML table"),
            @OpenApiResponse(status = "404", description = "No listings found")
        }
    )
    public void getAllListings(Context ctx) {
        List<PropertyListing> all = listings.getAllListings();
        if (all.isEmpty()) {
            ctx.status(404).html(HtmlUtil.errorPage("No Listings Found"));
        } else {
            ctx.status(200).html(listingTableHtml("All Listings", all));
        }
    }

    @OpenApi(
        path = "/listing/{propertyID}",
        methods = HttpMethod.GET,
        summary = "Get listing history for a property",
        operationId = "getListingsByProperty",
        tags = {"Listing"},
        pathParams = @OpenApiParam(name = "propertyID", type = String.class, description = "The property ID"),
        responses = {
            @OpenApiResponse(status = "200", description = "Listing history returned as HTML table"),
            @OpenApiResponse(status = "404", description = "No listings found for property")
        }
    )
    public void getListingsByProperty(Context ctx, String propertyId) {
        List<PropertyListing> history = listings.getListingsByPropertyId(propertyId);
        if (history.isEmpty()) {
            ctx.status(404).html(HtmlUtil.errorPage("No listings for property " + propertyId));
        } else {
            accessCounts.increment("property", propertyId);
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
