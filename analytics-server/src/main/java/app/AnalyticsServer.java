package app;

import analytics.AccessCountDAO;
import analytics.AnalyticsController;
import io.javalin.Javalin;

public class AnalyticsServer {

    public static void main(String[] args) {
        int port = port();

        var dao = new AccessCountDAO();
        var controller = new AnalyticsController(dao);

        Javalin.create()
            .get("/", ctx -> ctx.result("Analytics server is running"))
            .get("/analytics", controller::getAll)
            .get("/analytics/top/property", controller::getTopProperties)
            .get("/analytics/top/postcode", controller::getTopPostcodes)
            .post("/analytics/access", controller::recordAccess)
            .start(port);

        System.out.println("Analytics server listening on " + port);
    }

    private static int port() {
        String p = System.getenv("ANALYTICS_PORT");
        return (p == null || p.isBlank()) ? 7073 : Integer.parseInt(p);
    }
}