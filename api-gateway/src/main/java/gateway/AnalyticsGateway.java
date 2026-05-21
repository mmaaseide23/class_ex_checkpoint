package gateway;

import client.ServiceClient;
import client.ServiceUrls;
import com.fasterxml.jackson.core.type.TypeReference;
import gateway.dto.AccessCountDto;
import gateway.dto.SaleDto;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Analytics-side orchestration. Simple endpoints are passthroughs; the interesting
 * one is GET /analytics/top/property?withDetails=true, which scatter-gathers calls
 * to Property to enrich the analytics rows with addresses.
 */
public class AnalyticsGateway {

    private final ServiceClient client;
    private final ServiceUrls urls;

    public AnalyticsGateway(ServiceClient client, ServiceUrls urls) {
        this.client = client;
        this.urls = urls;
    }

    public void getAll(Context ctx) {
        ctx.status(200).json(client.getJsonList(
            urls.analytics + "/analytics", new TypeReference<List<AccessCountDto>>() {}));
    }

    public void topPostcodes(Context ctx) {
        String url = urls.analytics + "/analytics/top/postcode" + queryString(ctx);
        ctx.status(200).json(client.getJsonList(url, new TypeReference<List<AccessCountDto>>() {}));
    }

    public void topProperties(Context ctx) {
        String url = urls.analytics + "/analytics/top/property" + queryString(ctx);
        List<AccessCountDto> top = client.getJsonList(url, new TypeReference<>() {});

        boolean withDetails = "true".equalsIgnoreCase(ctx.queryParam("withDetails"));
        if (!withDetails) {
            ctx.status(200).json(top);
            return;
        }

        // Scatter: fetch sale details for each property id in parallel
        Map<String, CompletableFuture<SaleDto>> futures = new HashMap<>();
        for (AccessCountDto row : top) {
            String pid = row.accessValue;
            futures.put(pid, CompletableFuture.supplyAsync(() -> {
                try {
                    return client.getJson(urls.property + "/sale/" + pid, SaleDto.class);
                } catch (Exception e) {
                    return null;
                }
            }));
        }

        // Gather + merge
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (AccessCountDto row : top) {
            SaleDto sale = futures.get(row.accessValue).join();
            Map<String, Object> merged = new HashMap<>();
            merged.put("propertyId", row.accessValue);
            merged.put("count", row.count);
            if (sale != null) {
                merged.put("address", sale.address);
                merged.put("postCode", sale.postCode);
                merged.put("purchasePrice", sale.purchasePrice);
            }
            enriched.add(merged);
        }
        ctx.status(200).json(enriched);
    }

    private String queryString(Context ctx) {
        String limit = ctx.queryParam("limit");
        return (limit == null) ? "" : "?limit=" + limit;
    }
}