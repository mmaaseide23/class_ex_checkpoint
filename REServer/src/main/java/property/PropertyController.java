package property;

import app.HtmlUtil;
import io.javalin.http.Context;
import io.javalin.openapi.*;

import java.util.List;
import java.util.Optional;

public class PropertyController {

    private final PropertyDAO properties;

    public PropertyController(PropertyDAO properties) {
        this.properties = properties;
    }

    @OpenApi(
        path = "/property",
        methods = HttpMethod.POST,
        summary = "Create a new property",
        operationId = "createProperty",
        tags = {"Property"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Property.class)),
        responses = {
            @OpenApiResponse(status = "201", description = "Property created"),
            @OpenApiResponse(status = "400", description = "Failed to add property")
        }
    )
    public void createProperty(Context ctx) {
        Property property = ctx.bodyValidator(Property.class).get();

        if (properties.newProperty(property)) {
            ctx.status(201).result("Property Created");
        } else {
            ctx.status(400).result("Failed to add property");
        }
    }

    @OpenApi(
        path = "/property",
        methods = HttpMethod.GET,
        summary = "Get all properties, optionally filtered by price range",
        operationId = "getAllProperties",
        tags = {"Property"},
        queryParams = {
            @OpenApiParam(name = "minPrice", type = Long.class, description = "Minimum purchase price"),
            @OpenApiParam(name = "maxPrice", type = Long.class, description = "Maximum purchase price")
        },
        responses = {
            @OpenApiResponse(status = "200", description = "Properties returned as HTML table"),
            @OpenApiResponse(status = "404", description = "No properties found")
        }
    )
    public void getAllProperties(Context ctx) {
        String minParam = ctx.queryParam("minPrice");
        String maxParam = ctx.queryParam("maxPrice");

        List<Property> allProperties;
        if (minParam != null || maxParam != null) {
            long min = minParam != null ? Long.parseLong(minParam) : Long.MIN_VALUE;
            long max = maxParam != null ? Long.parseLong(maxParam) : Long.MAX_VALUE;
            allProperties = properties.getPropertiesByPriceRange(min, max);
        } else {
            allProperties = properties.getAllProperties();
        }

        if (allProperties.isEmpty()) {
            ctx.status(404).html(HtmlUtil.errorPage("No Properties Found"));
        } else {
            ctx.status(200).html(propertyTableHtml("All Properties", allProperties));
        }
    }

    @OpenApi(
        path = "/property/{propertyID}",
        methods = HttpMethod.GET,
        summary = "Get a property by its property ID",
        operationId = "getPropertyByID",
        tags = {"Property"},
        pathParams = @OpenApiParam(name = "propertyID", type = String.class, description = "The property ID"),
        responses = {
            @OpenApiResponse(status = "200", description = "Property returned as HTML table"),
            @OpenApiResponse(status = "404", description = "Property not found")
        }
    )
    public void getPropertyByID(Context ctx, String id) {
        Optional<Property> property = properties.getPropertyById(id);
        if (property.isPresent()) {
            ctx.status(200).html(propertyTableHtml("Property " + id, List.of(property.get())));
        } else {
            ctx.status(404).html(HtmlUtil.errorPage("Property not found"));
        }
    }

    @OpenApi(
        path = "/property/postcode/{postcode}",
        methods = HttpMethod.GET,
        summary = "Find properties by postcode",
        operationId = "findPropertyByPostCode",
        tags = {"Property"},
        pathParams = @OpenApiParam(name = "postcode", type = String.class, description = "The postcode to search"),
        responses = {
            @OpenApiResponse(status = "200", description = "Properties returned as HTML table"),
            @OpenApiResponse(status = "404", description = "No properties found for postcode")
        }
    )
    public void findPropertyByPostCode(Context ctx, String postCode) {
        List<Property> result = properties.getPropertiesByPostCode(postCode);
        if (result.isEmpty()) {
            ctx.status(404).html(HtmlUtil.errorPage("No properties for postcode found"));
        } else {
            ctx.status(200).html(propertyTableHtml("Properties in Postcode " + postCode, result));
        }
    }

    private String propertyTableHtml(String title, List<Property> props) {
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtil.pageHeader(title));
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
        sb.append("<tr><th>ID</th><th>Council</th><th>Price</th><th>Address</th><th>Postcode</th><th>Type</th><th>Area</th><th>Contract Date</th><th>Zoning</th><th>Purpose</th></tr>");
        for (Property p : props) {
            sb.append("<tr>")
              .append("<td>").append(p.propertyId).append("</td>")
              .append("<td>").append(p.councilName).append("</td>")
              .append("<td>").append(p.purchasePrice).append("</td>")
              .append("<td>").append(p.address).append("</td>")
              .append("<td>").append(p.postCode).append("</td>")
              .append("<td>").append(p.propertyType).append("</td>")
              .append("<td>").append(p.area).append("</td>")
              .append("<td>").append(p.contractDate).append("</td>")
              .append("<td>").append(p.zoning).append("</td>")
              .append("<td>").append(p.primaryPurpose).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>");
        sb.append(HtmlUtil.pageFooter());
        return sb.toString();
    }
}
