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
import java.util.Set;

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

    public void updatePrice(Context ctx, int id) {
        Optional<Listing> existing = listings.getById(id);
        if (existing.isEmpty()) {
            ctx.status(404).json("Listing not found");
            return;
        }
        long newPrice;
        try {
            JsonNode body = MAPPER.readTree(ctx.body());
            newPrice = body.path("price").asLong(-1);
        } catch (Exception e) {
            ctx.status(400).json("Invalid request body");
            return;
        }
        if (newPrice <= 0) {
            ctx.status(400).json("price must be a positive number");
            return;
        }
        Listing listing = existing.get();
        if (listing.price == newPrice) {
            ctx.status(200).json("Price unchanged");
            return;
        }
        long oldPrice = listing.price;
        if (!listings.updatePrice(id, newPrice)) {
            ctx.status(500).json("Failed to update price");
            return;
        }
        ctx.status(200).json("Price updated");
        publishChange(listing.propertyId, "PRICE_CHANGED", event -> event
                .put("oldPrice", oldPrice)
                .put("newPrice", newPrice));
    }

    public void updateStatus(Context ctx, int id) {
        Optional<Listing> existing = listings.getById(id);
        if (existing.isEmpty()) {
            ctx.status(404).json("Listing not found");
            return;
        }
        String newStatus;
        try {
            JsonNode body = MAPPER.readTree(ctx.body());
            newStatus = body.path("status").asText("");
        } catch (Exception e) {
            ctx.status(400).json("Invalid request body");
            return;
        }
        if (!ALLOWED_STATUSES.contains(newStatus)) {
            ctx.status(400).json("status must be one of " + ALLOWED_STATUSES);
            return;
        }
        Listing listing = existing.get();
        if (newStatus.equals(listing.status)) {
            ctx.status(200).json("Status unchanged");
            return;
        }
        String oldStatus = listing.status;
        if (!listings.updateStatus(id, newStatus)) {
            ctx.status(500).json("Failed to update status");
            return;
        }
        ctx.status(200).json("Status updated");
        publishChange(listing.propertyId, "STATUS_CHANGED", event -> event
                .put("price", listing.price)
                .put("oldStatus", oldStatus)
                .put("newStatus", newStatus));
    }

    private static final Set<String> ALLOWED_STATUSES = Set.of("Pending", "Sold");

    private void publishChange(long propertyId, String action,
                               java.util.function.Consumer<ObjectNode> enrich) {
        try {
            Optional<Sale> sale = sales.getSaleByPropertyId(String.valueOf(propertyId));
            if (sale.isEmpty()) {
                System.err.println("Skipping event for unknown property " + propertyId);
                return;
            }
            ObjectNode event = MAPPER.createObjectNode()
                    .put("eventType", "PROPERTY_CHANGED")
                    .put("propertyId", propertyId)
                    .put("postCode", sale.get().postCode)
                    .put("address", sale.get().address)
                    .put("action", action);
            enrich.accept(event);
            EventPublisher.publish(RabbitConfig.PROPERTY_CHANGED_KEY, event.toString());
        } catch (Exception e) {
            System.err.println("Event publish failed: " + e.getMessage());
        }
    }
}