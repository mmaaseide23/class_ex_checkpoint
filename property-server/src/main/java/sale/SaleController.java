package sale;

import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;

public class SaleController {

    private final SaleDAO sales;

    public SaleController(SaleDAO sales) {
        this.sales = sales;
    }

    public void createSale(Context ctx) {
        Sale sale = ctx.bodyValidator(Sale.class).get();
        if (sales.newSale(sale)) {
            ctx.status(201).json("Sale Created");
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

        if (allSales.isEmpty()) {
            ctx.status(404).json("No Sales Found");
        } else {
            ctx.status(200).json(allSales);
        }
    }

    public void getSaleByPropertyID(Context ctx, String id) {
        Optional<Sale> sale = sales.getSaleByPropertyId(id);
        if (sale.isPresent()) {
            ctx.status(200).json(sale.get());
        } else {
            ctx.status(404).json("Sale not found");
        }
    }

    public void findSalesByPostCode(Context ctx, String postCode) {
        List<Sale> result = sales.getSalesByPostCode(postCode);
        if (result.isEmpty()) {
            ctx.status(404).json("No sales for postcode found");
        } else {
            ctx.status(200).json(result);
        }
    }
}