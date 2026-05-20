package app;

import io.javalin.Javalin;
import purchaser.PurchaserController;
import purchaser.PurchaserDAO;

public class PurchasersServer {

    public static void main(String[] args) {
        int port = port();

        var dao = new PurchaserDAO();
        var controller = new PurchaserController(dao);

        Javalin.create()
            .get("/", ctx -> ctx.result("Purchasers server is running"))
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
}