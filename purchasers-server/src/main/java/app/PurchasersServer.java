package app;

import io.javalin.Javalin;
import purchaser.PurchaserController;
import purchaser.PurchaserDAO;

public class PurchasersServer {

    public static void main(String[] args) {
        int port = port();

        var dao = new PurchaserDAO();
        var controller = new PurchaserController(dao);

        var app = Javalin.create();
        SwaggerSetup.register(app, "Purchasers Server", OPENAPI_SPEC);

        app.get("/", ctx -> ctx.result("Purchasers server is running"))
            .post("/purchaser", controller::register)
            .get("/purchaser/with-postcode-prefs", controller::withPostcodePrefs)
            .get("/purchaser/{id}", ctx ->
                controller.getPurchaser(ctx, ctx.pathParam("id")))
            .post("/purchaser/{id}/preference", ctx ->
                controller.addPreference(ctx, ctx.pathParam("id")))
            .get("/purchaser/{id}/preference", ctx ->
                controller.getPreferences(ctx, ctx.pathParam("id")))
            .delete("/purchaser/{purchaserId}/preference/{id}", ctx ->
                controller.deletePreference(ctx, ctx.pathParam("id")))
            .start(port);

        System.out.println("Purchasers server listening on " + port);
    }

    private static int port() {
        String p = System.getenv("PURCHASERS_PORT");
        return (p == null || p.isBlank()) ? 7072 : Integer.parseInt(p);
    }

    private static final String OPENAPI_SPEC = """
        {
          "openapi": "3.0.3",
          "info": { "title": "Purchasers Server", "version": "1.0", "description": "Manages purchasers and their postcode preferences." },
          "paths": {
            "/purchaser": {
              "post": { "summary": "Register a new purchaser", "requestBody": { "required": true, "content": { "application/json": { "schema": { "$ref": "#/components/schemas/Purchaser" },
                "example": { "firstName": "Alice", "lastName": "Smith", "email": "alice@example.com", "phone": "555-0100" }
              } } }, "responses": { "201": { "description": "Purchaser created" }, "400": { "description": "Validation error or duplicate email" } } }
            },
            "/purchaser/with-postcode-prefs": {
              "get": { "summary": "Get all purchasers that have postcode preferences", "responses": { "200": { "description": "Array of purchasers with their postcode lists" } } }
            },
            "/purchaser/{id}": {
              "get": { "summary": "Get purchaser by ID", "parameters": [
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Purchaser object" }, "404": { "description": "Not found" } } }
            },
            "/purchaser/{id}/preference": {
              "post": { "summary": "Add a preference for a purchaser", "parameters": [
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "requestBody": { "required": true, "content": { "application/json": { "schema": { "$ref": "#/components/schemas/Preference" },
                "example": { "preferenceType": "postcode", "preferenceValue": "2000" }
              } } }, "responses": { "201": { "description": "Preference added" }, "400": { "description": "Validation error or max 5 postcode prefs reached" } } },
              "get": { "summary": "Get all preferences for a purchaser", "parameters": [
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Array of preferences" } } }
            },
            "/purchaser/{purchaserId}/preference/{id}": {
              "delete": { "summary": "Delete a preference by ID", "parameters": [
                { "name": "purchaserId", "in": "path", "required": true, "schema": { "type": "integer" } },
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Preference deleted" }, "404": { "description": "Preference not found" } } }
            }
          },
          "components": { "schemas": {
            "Purchaser": { "type": "object", "properties": {
              "firstName": { "type": "string" }, "lastName": { "type": "string" },
              "email": { "type": "string" }, "phone": { "type": "string" }
            } },
            "Preference": { "type": "object", "properties": {
              "preferenceType": { "type": "string", "example": "postcode" },
              "preferenceValue": { "type": "string", "example": "2000" }
            } }
          } }
        }
        """;
}