package analytics;

import io.javalin.http.Context;

public class AnalyticsController {

    private final AccessCountDAO accessCounts;

    public AnalyticsController(AccessCountDAO accessCounts) {
        this.accessCounts = accessCounts;
    }

    public void getAll(Context ctx) {
        ctx.status(200).json(accessCounts.getAll());
    }

    public void getTopProperties(Context ctx) {
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(10);
        ctx.status(200).json(accessCounts.getTopByType("property", limit));
    }

    public void getTopPostcodes(Context ctx) {
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(10);
        ctx.status(200).json(accessCounts.getTopByType("postcode", limit));
    }

    public void recordAccess(Context ctx) {
        AccessEvent event = ctx.bodyValidator(AccessEvent.class).get();
        if (event.type == null || event.type.isBlank()
                || event.value == null || event.value.isBlank()) {
            ctx.status(400).json("type and value are required");
            return;
        }
        accessCounts.increment(event.type, event.value);
        ctx.status(204);
    }
}