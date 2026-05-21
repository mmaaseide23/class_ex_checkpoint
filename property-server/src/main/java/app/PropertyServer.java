package app;

import io.javalin.Javalin;
import listing.ListingController;
import listing.ListingDAO;
import sale.SaleController;
import sale.SaleDAO;

public class PropertyServer {

    public static void main(String[] args) {
        int port = port();

        var saleDAO = new SaleDAO();
        var saleController = new SaleController(saleDAO);

        var listingDAO = new ListingDAO();
        var listingController = new ListingController(listingDAO, saleDAO);

        var app = Javalin.create();
        SwaggerSetup.register(app, "Property Server", OPENAPI_SPEC);

        app.get("/", ctx -> ctx.result("Property server is running"))
            // Sale
            .get("/sale", saleController::getAllSales)
            .post("/sale", saleController::createSale)
            .get("/sale/{propertyID}", ctx ->
                saleController.getSaleByPropertyID(ctx, ctx.pathParam("propertyID")))
            .get("/sale/postcode/{postcode}", ctx ->
                saleController.findSalesByPostCode(ctx, ctx.pathParam("postcode")))
            // Listing
            .get("/listing", listingController::getAllListings)
            .post("/listing", listingController::createListing)
            .get("/listing/{propertyID}", ctx ->
                listingController.getListingsByProperty(ctx, ctx.pathParam("propertyID")))
            .get("/listing/postcode/{postcode}", ctx ->
                listingController.getListingsByPostCode(ctx, ctx.pathParam("postcode")))
            .patch("/listing/{id}/price", ctx ->
                listingController.updatePrice(ctx, Integer.parseInt(ctx.pathParam("id"))))
            .patch("/listing/{id}/status", ctx ->
                listingController.updateStatus(ctx, Integer.parseInt(ctx.pathParam("id"))))
            .start(port);

        System.out.println("Property server listening on " + port);
    }

    private static int port() {
        String p = System.getenv("PROPERTY_PORT");
        return (p == null || p.isBlank()) ? 7071 : Integer.parseInt(p);
    }

    private static final String OPENAPI_SPEC = """
        {
          "openapi": "3.0.3",
          "info": { "title": "Property Server", "version": "1.0", "description": "Owns sales and listings data. Publishes property.changed events to RabbitMQ." },
          "paths": {
            "/sale": {
              "get": { "summary": "Get all sales", "description": "Supports optional ?minPrice=X&maxPrice=Y filtering", "parameters": [
                { "name": "minPrice", "in": "query", "schema": { "type": "integer" } },
                { "name": "maxPrice", "in": "query", "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Array of sales" } } },
              "post": { "summary": "Create a sale (fires property.changed event)", "requestBody": { "required": true, "content": { "application/json": { "schema": { "$ref": "#/components/schemas/Sale" },
                "example": { "propertyId": 11111111, "postCode": "2830", "address": "99 Demo Street, Bourke", "purchasePrice": 450000, "councilName": "Bourke Shire", "propertyType": "House", "zoning": "Residential" }
              } } }, "responses": { "201": { "description": "Sale Created" }, "400": { "description": "Failed" } } }
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
              "post": { "summary": "Create a listing (fires property.changed event)", "requestBody": { "required": true, "content": { "application/json": { "schema": { "$ref": "#/components/schemas/Listing" },
                "example": { "propertyId": 2021000, "listingDate": "2026-05-21", "price": 3800000 }
              } } }, "responses": { "201": { "description": "Listing Created" }, "400": { "description": "Failed" } } }
            },
            "/listing/{propertyID}": {
              "get": { "summary": "Get listings by property ID", "parameters": [
                { "name": "propertyID", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "responses": { "200": { "description": "Array of listings" } } }
            },
            "/listing/postcode/{postcode}": {
              "get": { "summary": "Get listings by postcode (JOINs with sales)", "parameters": [
                { "name": "postcode", "in": "path", "required": true, "schema": { "type": "string" } }
              ], "responses": { "200": { "description": "Array of listings with postcode info" } } }
            },
            "/listing/{id}/price": {
              "patch": { "summary": "Update a listing's price (fires property.changed event with action PRICE_CHANGED)", "parameters": [
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "requestBody": { "required": true, "content": { "application/json": {
                "example": { "price": 4200000 }
              } } }, "responses": { "200": { "description": "Updated" }, "400": { "description": "Bad request" }, "404": { "description": "Not found" } } }
            },
            "/listing/{id}/status": {
              "patch": { "summary": "Update a listing's status (fires property.changed event with action STATUS_CHANGED)", "parameters": [
                { "name": "id", "in": "path", "required": true, "schema": { "type": "integer" } }
              ], "requestBody": { "required": true, "content": { "application/json": {
                "example": { "status": "Sold" }
              } } }, "responses": { "200": { "description": "Updated" }, "400": { "description": "Bad request" }, "404": { "description": "Not found" } } }
            }
          },
          "components": { "schemas": {
            "Sale": { "type": "object", "properties": {
              "propertyId": { "type": "integer" }, "postCode": { "type": "string" },
              "address": { "type": "string" }, "purchasePrice": { "type": "integer" },
              "councilName": { "type": "string" }, "propertyType": { "type": "string" },
              "zoning": { "type": "string" }, "primaryPurpose": { "type": "string" }
            } },
            "Listing": { "type": "object", "properties": {
              "propertyId": { "type": "integer" }, "listingDate": { "type": "string", "format": "date" },
              "price": { "type": "integer" }
            } }
          } }
        }
        """;
}