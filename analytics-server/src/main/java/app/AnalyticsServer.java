package app;

import analytics.AccessCountDAO;
import analytics.AnalyticsController;
import io.javalin.Javalin;

public class AnalyticsServer {

    public static void main(String[] args) {
        int port = port();

        var dao = new AccessCountDAO();
        var controller = new AnalyticsController(dao);

        var app = Javalin.create();
        SwaggerSetup.register(app, "Analytics Server", OPENAPI_SPEC);

        app.get("/", ctx -> ctx.result("Analytics server is running"))
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

    private static final String OPENAPI_SPEC = """
        {
          "openapi": "3.0.3",
          "info": { "title": "Analytics Server", "version": "1.0", "description": "Tracks property and postcode access counts. Fires property.hot events to RabbitMQ." },
          "paths": {
            "/analytics": {
              "get": { "summary": "Get all access counts", "responses": { "200": { "description": "Array of access count records" } } }
            },
            "/analytics/top/property": {
              "get": { "summary": "Get top accessed properties", "parameters": [
                { "name": "limit", "in": "query", "schema": { "type": "integer", "default": 10 }, "description": "Max results to return" }
              ], "responses": { "200": { "description": "Array of top property access counts" } } }
            },
            "/analytics/top/postcode": {
              "get": { "summary": "Get top accessed postcodes", "parameters": [
                { "name": "limit", "in": "query", "schema": { "type": "integer", "default": 10 }, "description": "Max results to return" }
              ], "responses": { "200": { "description": "Array of top postcode access counts" } } }
            },
            "/analytics/access": {
              "post": { "summary": "Record an access event (fires property.hot if type is 'property')", "requestBody": { "required": true, "content": { "application/json": { "schema": { "$ref": "#/components/schemas/AccessEvent" },
                "example": { "type": "property", "value": "11111111" }
              } } }, "responses": { "204": { "description": "Access recorded" }, "400": { "description": "type and value are required" } } }
            }
          },
          "components": { "schemas": {
            "AccessEvent": { "type": "object", "properties": {
              "type": { "type": "string", "example": "property" },
              "value": { "type": "string", "example": "11111111" }
            } }
          } }
        }
        """;
}