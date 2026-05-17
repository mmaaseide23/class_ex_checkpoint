package sale;

import analytics.AccessCountDAO;
import app.HtmlUtil;
import io.javalin.http.Context;
import io.javalin.openapi.*;

import java.util.List;
import java.util.Optional;

public class SaleController {

    private final SaleDAO sales;
    private final AccessCountDAO accessCounts;

    public SaleController(SaleDAO sales, AccessCountDAO accessCounts) {
        this.sales = sales;
        this.accessCounts = accessCounts;
    }

    @OpenApi(
        path = "/sale",
        methods = HttpMethod.POST,
        summary = "Create a new sale record",
        operationId = "createSale",
        tags = {"Sale"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Sale.class)),
        responses = {
            @OpenApiResponse(status = "201", description = "Sale created"),
            @OpenApiResponse(status = "400", description = "Failed to add sale")
        }
    )
    public void createSale(Context ctx) {
        Sale sale = ctx.bodyValidator(Sale.class).get();

        if (sales.newSale(sale)) {
            ctx.status(201).result("Sale Created");
        } else {
            ctx.status(400).result("Failed to add sale");
        }
    }

    @OpenApi(
        path = "/sale",
        methods = HttpMethod.GET,
        summary = "Get all sales, optionally filtered by price range",
        operationId = "getAllSales",
        tags = {"Sale"},
        queryParams = {
            @OpenApiParam(name = "minPrice", type = Long.class, description = "Minimum purchase price"),
            @OpenApiParam(name = "maxPrice", type = Long.class, description = "Maximum purchase price")
        },
        responses = {
            @OpenApiResponse(status = "200", description = "Sales returned as HTML table"),
            @OpenApiResponse(status = "404", description = "No sales found")
        }
    )
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
            ctx.status(404).html(HtmlUtil.errorPage("No Sales Found"));
        } else {
            ctx.status(200).html(saleTableHtml("All Sales", allSales));
        }
    }

    @OpenApi(
        path = "/sale/{propertyID}",
        methods = HttpMethod.GET,
        summary = "Get a sale by property ID",
        operationId = "getSaleByPropertyID",
        tags = {"Sale"},
        pathParams = @OpenApiParam(name = "propertyID", type = String.class, description = "The property ID"),
        responses = {
            @OpenApiResponse(status = "200", description = "Sale returned as HTML table"),
            @OpenApiResponse(status = "404", description = "Sale not found")
        }
    )
    public void getSaleByPropertyID(Context ctx, String id) {
        Optional<Sale> sale = sales.getSaleByPropertyId(id);
        if (sale.isPresent()) {
            accessCounts.increment("property", id);
            ctx.status(200).html(saleTableHtml("Sale " + id, List.of(sale.get())));
        } else {
            ctx.status(404).html(HtmlUtil.errorPage("Sale not found"));
        }
    }

    @OpenApi(
        path = "/sale/postcode/{postcode}",
        methods = HttpMethod.GET,
        summary = "Find sales by postcode",
        operationId = "findSalesByPostCode",
        tags = {"Sale"},
        pathParams = @OpenApiParam(name = "postcode", type = String.class, description = "The postcode to search"),
        responses = {
            @OpenApiResponse(status = "200", description = "Sales returned as HTML table"),
            @OpenApiResponse(status = "404", description = "No sales found for postcode")
        }
    )
    public void findSalesByPostCode(Context ctx, String postCode) {
        List<Sale> result = sales.getSalesByPostCode(postCode);
        if (result.isEmpty()) {
            ctx.status(404).html(HtmlUtil.errorPage("No sales for postcode found"));
        } else {
            accessCounts.increment("postcode", postCode);
            ctx.status(200).html(saleTableHtml("Sales in Postcode " + postCode, result));
        }
    }

    private String saleTableHtml(String title, List<Sale> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtil.pageHeader(title));
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
        sb.append("<tr><th>ID</th><th>Council</th><th>Price</th><th>Address</th><th>Postcode</th><th>Type</th><th>Area</th><th>Contract Date</th><th>Zoning</th><th>Purpose</th></tr>");
        for (Sale s : rows) {
            sb.append("<tr>")
              .append("<td>").append(s.propertyId).append("</td>")
              .append("<td>").append(s.councilName).append("</td>")
              .append("<td>").append(s.purchasePrice).append("</td>")
              .append("<td>").append(s.address).append("</td>")
              .append("<td>").append(s.postCode).append("</td>")
              .append("<td>").append(s.propertyType).append("</td>")
              .append("<td>").append(s.area).append("</td>")
              .append("<td>").append(s.contractDate).append("</td>")
              .append("<td>").append(s.zoning).append("</td>")
              .append("<td>").append(s.primaryPurpose).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>");
        sb.append(HtmlUtil.pageFooter());
        return sb.toString();
    }
}
