package sale;

import app.EventPublisher;
import app.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;

public class SaleController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final SaleDAO sales;

    public SaleController(SaleDAO sales) {
        this.sales = sales;
    }

    public void createSale(Context ctx) {
        Sale sale = ctx.bodyValidator(Sale.class).get();
        if (sales.newSale(sale)) {
            ctx.status(201).json("Sale Created");

            // Emit domain event — property server does not know who consumes it
            try {
                ObjectNode event = MAPPER.createObjectNode()
                        .put("eventType", "PROPERTY_CHANGED")
                        .put("propertyId", sale.propertyId)
                        .put("postCode", sale.postCode)
                        .put("address", sale.address)
                        .put("purchasePrice", sale.purchasePrice)
                        .put("action", "NEW_SALE");
                EventPublisher.publish(RabbitConfig.PROPERTY_CHANGED_KEY, event.toString());
            } catch (Exception e) {
                System.err.println("Event publish failed: " + e.getMessage());
            }
        } else {
            ctx.status(400).json("Failed to add sale");
        }
    }

    public void getAllSales(Context ctx) {
        String minParam = ctx.queryParam("minPrice");
        String maxParam = ctx.queryParam("maxPrice");

        List<Sale> allSales;
        if (minParam != null || maxParam != null) {
            long min = minParam != null ? Long.parseLong(minParam) : Long.MIN_VALUE;
            long max = maxParam != null ? Long.parseLong(maxParam) : Long.MAX_VALUE;
            allSales = sales.getSalesByPriceRange(min, max);
        } else {
            allSales = sales.getAllSales();
        }

        ctx.status(200).json(allSales);
    }

    public void getSaleByPropertyID(Context ctx, String id) {
        try {
            Optional<Sale> sale = sales.getSaleByPropertyId(id);
            if (sale.isPresent()) {
                ctx.status(200).json(sale.get());
            } else {
                ctx.status(404).json("Sale not found");
            }
        } catch (NumberFormatException e) {
            ctx.status(400).json("Invalid property ID: must be numeric");
        }
    }

    public void findSalesByPostCode(Context ctx, String postCode) {
        ctx.status(200).json(sales.getSalesByPostCode(postCode));
    }
}