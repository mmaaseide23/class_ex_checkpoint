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
        if (!listings.newListing(listing)) {
            ctx.status(400).json("Failed to add listing");
            return;
        }
        ctx.status(201).json("Listing Created");

        sales.getSaleByPropertyId(String.valueOf(listing.propertyId)).ifPresent(sale -> {
            String status = listing.status != null ? listing.status : "Pending";
            ObjectNode event = baseEvent("NEW_LISTING", listing.propertyId, sale.postCode, sale.address)
                    .put("purchasePrice", listing.price)
                    .put("status", status);
            publish(event);
        });
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

        // One event per kind of change so consumers can format distinct messages.
        Optional<Sale> saleOpt = sales.getSaleByPropertyId(String.valueOf(before.propertyId));
        if (saleOpt.isEmpty()) return;
        Sale sale = saleOpt.get();
        long effectivePrice = priceChanged ? newPrice : before.price;

        if (priceChanged) {
            publish(baseEvent("PRICE_CHANGE", before.propertyId, sale.postCode, sale.address)
                    .put("oldPrice", before.price)
                    .put("purchasePrice", newPrice));
        }
        if (statusChanged) {
            publish(baseEvent("STATUS_CHANGE", before.propertyId, sale.postCode, sale.address)
                    .put("purchasePrice", effectivePrice)
                    .put("oldStatus", before.status)
                    .put("status", newStatus));
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

    private static ObjectNode baseEvent(String action, long propertyId, String postCode, String address) {
        return MAPPER.createObjectNode()
                .put("eventType", "PROPERTY_CHANGED")
                .put("action", action)
                .put("propertyId", propertyId)
                .put("postCode", postCode)
                .put("address", address);
    }

    private static void publish(ObjectNode event) {
        try {
            EventPublisher.publish(RabbitConfig.PROPERTY_CHANGED_KEY, event.toString());
        } catch (Exception e) {
            System.err.println("Event publish failed: " + e.getMessage());
        }
    }
}
