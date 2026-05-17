package analytics;

import io.javalin.http.Context;
import io.javalin.openapi.*;

import java.util.List;

public class AnalyticsController {

    private final AccessCountDAO accessCounts;

    public AnalyticsController(AccessCountDAO accessCounts) {
        this.accessCounts = accessCounts;
    }

    @OpenApi(
        path = "/analytics",
        methods = HttpMethod.GET,
        summary = "Get all access counts",
        operationId = "getAllAccessCounts",
        tags = {"Analytics"},
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = AccessCount[].class), description = "All access counts")
        }
    )
    public void getAll(Context ctx) {
        ctx.status(200).json(accessCounts.getAll());
    }

    @OpenApi(
        path = "/analytics/top/property",
        methods = HttpMethod.GET,
        summary = "Get top viewed properties",
        operationId = "getTopProperties",
        tags = {"Analytics"},
        queryParams = @OpenApiParam(name = "limit", type = Integer.class, description = "Max results (default 10)"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = AccessCount[].class), description = "Top viewed properties")
        }
    )
    public void getTopProperties(Context ctx) {
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(10);
        ctx.status(200).json(accessCounts.getTopByType("property", limit));
    }

    @OpenApi(
        path = "/analytics/top/postcode",
        methods = HttpMethod.GET,
        summary = "Get top searched postcodes",
        operationId = "getTopPostcodes",
        tags = {"Analytics"},
        queryParams = @OpenApiParam(name = "limit", type = Integer.class, description = "Max results (default 10)"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = AccessCount[].class), description = "Top searched postcodes")
        }
    )
    public void getTopPostcodes(Context ctx) {
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(10);
        ctx.status(200).json(accessCounts.getTopByType("postcode", limit));
    }
}
