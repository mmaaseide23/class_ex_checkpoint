package app;

import client.ServiceClient;
import client.ServiceUrls;
import gateway.AnalyticsGateway;
import gateway.ListingGateway;
import gateway.NotifyGateway;
import gateway.PurchaserGateway;
import gateway.SaleGateway;
import io.javalin.Javalin;

public class ApiGateway {

    public static void main(String[] args) {
        int port = port();
        ServiceUrls urls = ServiceUrls.fromEnv();
        ServiceClient client = new ServiceClient();

        SaleGateway sale = new SaleGateway(client, urls);
        ListingGateway listing = new ListingGateway(client, urls);
        PurchaserGateway purchaser = new PurchaserGateway(client, urls);
        AnalyticsGateway analytics = new AnalyticsGateway(client, urls);
        NotifyGateway notify = new NotifyGateway(client, urls);

        var app = Javalin.create();
        SwaggerSetup.register(app, "API Gateway", OPENAPI_SPEC);

        app.get("/", ctx -> ctx.result("API Gateway is running"))

            // Sale (orchestrates Property + fire-and-forget Analytics)
            .get("/sale", sale::getAll)
            .post("/sale", sale::create)
            .get("/sale/{propertyID}", sale::getByPropertyId)
            .get("/sale/postcode/{postcode}", sale::getByPostcode)

            // Listing (orchestrates Property + fire-and-forget Analytics)
            .get("/listing", listing::getAll)
            .post("/listing", listing::create)
            .put("/listing/{id}", listing::update)
            .get("/listing/{propertyID}", listing::getByProperty)

            // Purchaser (passthrough to Purchasers)
            .post("/purchaser", purchaser::register)
            .get("/purchaser/{id}", purchaser::get)
            .post("/purchaser/{id}/preference", purchaser::addPreference)
            .get("/purchaser/{id}/preference", purchaser::getPreferences)
            .delete("/purchaser/{purchaserId}/preference/{id}", purchaser::deletePreference)

            // Notify (scatter-gather across Purchasers + Property)
            .get("/notify", notify::notify)

            // Analytics (passthrough + scatter-gather for property details)
            .get("/analytics", analytics::getAll)
            .get("/analytics/top/property", analytics::topProperties)
            .get("/analytics/top/postcode", analytics::topPostcodes)

            .start(port);

        System.out.println("API Gateway listening on " + port);
        System.out.println("  property   -> " + urls.property);
        System.out.println("  purchasers -> " + urls.purchasers);
        System.out.println("  analytics  -> " + urls.analytics);
    }

    private static int port() {
        String p = System.getenv("GATEWAY_PORT");
        return (p == null || p.isBlank()) ? 7070 : Integer.parseInt(p);
    }

    private static final String OPENAPI_SPEC = """
        {
          "openapi": "3.0.3",
          "info": { "title": "API Gateway", "version": "1.0", "description": "Single entry point. Orchestrates Property, Purchasers, and Analytics microservices." },
          "paths": {
            "/sale": {
              "get": { "summary": "Get all sales (orchestrates Property + Analytics)", "parameters": [
                { "name": "minPrice", "in": "query", "schema": { "type": "integer" } },
                { "name": "maxPrice", "in": "query", "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Array of sales" } } },
              "post": { "summary": "Create a sale (orchestrates Property + Analytics)", "requestBody": { "required": true, "content": { "application/json": {
                "example": { "propertyId": 11111111, "postCode": "2830", "address": "99 Demo Street, Bourke", "purchasePrice": 450000, "councilName": "Bourke Shire", "propertyType": "House", "zoning": "Residential" }
              } } }, "responses": { "201": { "description": "Sale created" } } }
            },
            "/sale/{propertyID}": {
              "get": { "summary": "Get sale by property ID", "parameters": [
                { "name": "propertyID", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Sale object" }, "404": { "description": "Not found" } } }
            },
            "/sale/postcode/{postcode}": {
              "get": { "summary": "Get sales by postcode", "parameters": [
                { "name": "postcode", "in": "path", "required": true, "schema": { "type": "string" } }
              ], "responses": { "200": { "description": "Array of sales" } } }
            },
            "/listing": {
              "get": { "summary": "Get all listings", "responses": { "200": { "description": "Array of listings" } } },
              "post": { "summary": "Create a listing (orchestrates Property + Analytics)", "requestBody": { "required": true, "content": { "application/json": {
                "example": { "propertyId": 2021000, "listingDate": "2026-05-21", "price": 3800000 }
              } } }, "responses": { "201": { "description": "Listing created" } } }
            },
            "/listing/{id}": {
              "put": { "summary": "Update listing price or status (fires PRICE_CHANGE / STATUS_CHANGE events)", "parameters": [
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "requestBody": { "required": true, "content": { "application/json": {
                "example": { "status": "Sold" }
              } } }, "responses": { "200": { "description": "Updated" }, "404": { "description": "Not found" } } }
            },
            "/listing/{propertyID}": {
              "get": { "summary": "Get listings by property ID", "parameters": [
                { "name": "propertyID", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Array of listings" } } }
            },
            "/purchaser": {
              "post": { "summary": "Register a purchaser (passthrough)", "requestBody": { "required": true, "content": { "application/json": {
                "example": { "firstName": "Alice", "lastName": "Smith", "email": "alice@example.com", "phone": "555-0100" }
              } } }, "responses": { "201": { "description": "Purchaser created" } } }
            },
            "/purchaser/{id}": {
              "get": { "summary": "Get purchaser by ID (passthrough)", "parameters": [
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Purchaser object" }, "404": { "description": "Not found" } } }
            },
            "/purchaser/{id}/preference": {
              "post": { "summary": "Add preference (passthrough)", "parameters": [
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "requestBody": { "required": true, "content": { "application/json": {
                "example": { "preferenceType": "postcode", "preferenceValue": "2000" }
              } } }, "responses": { "201": { "description": "Preference added" } } },
              "get": { "summary": "Get preferences (passthrough)", "parameters": [
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Array of preferences" } } }
            },
            "/purchaser/{purchaserId}/preference/{id}": {
              "delete": { "summary": "Delete preference (passthrough)", "parameters": [
                { "name": "purchaserId", "in": "path", "required": true, "schema": { "type": "integer" } },
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Deleted" }, "404": { "description": "Not found" } } }
            },
            "/notify": {
              "get": { "summary": "Scatter-gather notifications (Purchasers + Property)", "description": "Finds purchasers with postcode preferences, fetches matching listings, returns stitched results.", "responses": { "200": { "description": "HTML or plain-text notification report" } } }
            },
            "/analytics": {
              "get": { "summary": "Get all access counts (passthrough)", "responses": { "200": { "description": "Array of access counts" } } }
            },
            "/analytics/top/property": {
              "get": { "summary": "Top accessed properties", "parameters": [
                { "name": "limit", "in": "query", "schema": { "type": "integer", "default": 10 } }
              ], "responses": { "200": { "description": "Array of top properties" } } }
            },
            "/analytics/top/postcode": {
              "get": { "summary": "Top accessed postcodes", "parameters": [
                { "name": "limit", "in": "query", "schema": { "type": "integer", "default": 10 } }
              ], "responses": { "200": { "description": "Array of top postcodes" } } }
            }
          }
        }
        """;
}