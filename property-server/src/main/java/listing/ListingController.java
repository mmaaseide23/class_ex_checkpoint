package listing;

import app.EventPublisher;
import app.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.Context;
import sale.SaleDAO;

import java.util.List;

public class ListingController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ListingDAO listings;
    private final SaleDAO sales;

    public ListingController(ListingDAO listings, SaleDAO sales) {
        this.listings = listings;
        this.sales = sales;
    }

    public void createListing(Context ctx) {
        Listing listing = ctx.bodyValidator(Listing.class).get();
        if (listings.newListing(listing)) {
            ctx.status(201).json("Listing Created");

            // Look up the postcode from the sales table and emit event
            try {
                sales.getSaleByPropertyId(String.valueOf(listing.propertyId)).ifPresent(sale -> {
                    try {
                        ObjectNode event = MAPPER.createObjectNode()
                                .put("eventType", "PROPERTY_CHANGED")
                                .put("propertyId", listing.propertyId)
                                .put("postCode", sale.postCode)
                                .put("address", sale.address)
                                .put("purchasePrice", listing.price)
                                .put("action", "NEW_LISTING");
                        EventPublisher.publish(RabbitConfig.PROPERTY_CHANGED_KEY, event.toString());
                    } catch (Exception e) {
                        System.err.println("Event publish failed: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                System.err.println("Event lookup failed: " + e.getMessage());
            }
        } else {
            ctx.status(400).json("Failed to add listing");
        }
    }

    public void getAllListings(Context ctx) {
        ctx.status(200).json(listings.getAllListings());
    }

    public void getListingsByProperty(Context ctx, String propertyId) {
        ctx.status(200).json(listings.getListingsByPropertyId(propertyId));
    }

    public void getListingsByPostCode(Context ctx, String postCode) {
        List<ListingWithPostcode> matches = listings.getListingsByPostCode(postCode);
        ctx.status(200).json(matches);
    }
}