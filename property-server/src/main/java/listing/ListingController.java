package listing;

import app.EventPublisher;
import app.RabbitConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.Context;
import sale.Sale;
import sale.SaleDAO;

import java.util.List;
import java.util.Optional;

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
                    publishChange("NEW_LISTING", listing.propertyId, sale.postCode, sale.address,
                            listing.price, listing.status != null ? listing.status : "Pending");
                });
            } catch (Exception e) {
                System.err.println("Event lookup failed: " + e.getMessage());
            }
        } else {
            ctx.status(400).json("Failed to add listing");
        }
    }

    public void updateListing(Context ctx, String idParam) {
        int id;
        try {
            id = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            ctx.status(400).json("Invalid listing id");
            return;
        }

        Optional<Listing> existing = listings.getListingById(id);
        if (existing.isEmpty()) {
            ctx.status(404).json("Listing not found");
            return;
        }
        Listing before = existing.get();

        JsonNode body;
        try {
            body = MAPPER.readTree(ctx.body());
        } catch (Exception e) {
            ctx.status(400).json("Invalid JSON body");
            return;
        }

        Long newPrice = body.hasNonNull("price") ? body.get("price").asLong() : null;
        String newStatus = body.hasNonNull("status") ? body.get("status").asText() : null;

        if (newPrice == null && newStatus == null) {
            ctx.status(400).json("Provide at least one of: price, status");
            return;
        }
        if (newStatus != null && !newStatus.equals("Pending") && !newStatus.equals("Sold")) {
            ctx.status(400).json("Status must be 'Pending' or 'Sold'");
            return;
        }

        boolean priceChanged = newPrice != null && newPrice != before.price;
        boolean statusChanged = newStatus != null && !newStatus.equals(before.status);

        if (!priceChanged && !statusChanged) {
            ctx.status(200).json("No changes");
            return;
        }

        if (!listings.updateListing(id, priceChanged ? newPrice : null,
                statusChanged ? newStatus : null)) {
            ctx.status(500).json("Update failed");
            return;
        }

        ctx.status(200).json("Listing updated");

        // Fire one event per type of change so consumers can format distinct messages.
        Optional<Sale> saleOpt = sales.getSaleByPropertyId(String.valueOf(before.propertyId));
        if (saleOpt.isEmpty()) {
            System.err.println("Could not resolve postcode for property " + before.propertyId);
            return;
        }
        Sale sale = saleOpt.get();

        if (priceChanged) {
            publishPriceChange(before.propertyId, sale.postCode, sale.address,
                    before.price, newPrice);
        }
        if (statusChanged) {
            publishStatusChange(before.propertyId, sale.postCode, sale.address,
                    before.status, newStatus, priceChanged ? newPrice : before.price);
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

    private void publishChange(String action, long propertyId, String postCode,
                                String address, long price, String status) {
        try {
            ObjectNode event = MAPPER.createObjectNode()
                    .put("eventType", "PROPERTY_CHANGED")
                    .put("propertyId", propertyId)
                    .put("postCode", postCode)
                    .put("address", address)
                    .put("purchasePrice", price)
                    .put("status", status)
                    .put("action", action);
            EventPublisher.publish(RabbitConfig.PROPERTY_CHANGED_KEY, event.toString());
        } catch (Exception e) {
            System.err.println("Event publish failed: " + e.getMessage());
        }
    }

    private void publishPriceChange(long propertyId, String postCode, String address,
                                     long oldPrice, long newPrice) {
        try {
            ObjectNode event = MAPPER.createObjectNode()
                    .put("eventType", "PROPERTY_CHANGED")
                    .put("propertyId", propertyId)
                    .put("postCode", postCode)
                    .put("address", address)
                    .put("oldPrice", oldPrice)
                    .put("purchasePrice", newPrice)
                    .put("action", "PRICE_CHANGE");
            EventPublisher.publish(RabbitConfig.PROPERTY_CHANGED_KEY, event.toString());
        } catch (Exception e) {
            System.err.println("Event publish failed: " + e.getMessage());
        }
    }

    private void publishStatusChange(long propertyId, String postCode, String address,
                                      String oldStatus, String newStatus, long price) {
        try {
            ObjectNode event = MAPPER.createObjectNode()
                    .put("eventType", "PROPERTY_CHANGED")
                    .put("propertyId", propertyId)
                    .put("postCode", postCode)
                    .put("address", address)
                    .put("purchasePrice", price)
                    .put("oldStatus", oldStatus)
                    .put("status", newStatus)
                    .put("action", "STATUS_CHANGE");
            EventPublisher.publish(RabbitConfig.PROPERTY_CHANGED_KEY, event.toString());
        } catch (Exception e) {
            System.err.println("Event publish failed: " + e.getMessage());
        }
    }
}
