package app;

import io.javalin.Javalin;

/**
 * Registers /swagger (UI) and /openapi (spec) routes on any Javalin server.
 * Uses Swagger UI from CDN — no extra dependencies needed.
 */
public class SwaggerSetup {

    public static void register(Javalin app, String title, String specJson) {
        app.get("/openapi", ctx -> ctx.contentType("application/json").result(specJson));
        app.get("/swagger", ctx -> ctx.html(html(title)));
    }

    private static String html(String title) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <title>%s</title>
              <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
            </head>
            <body>
              <div id="swagger-ui"></div>
              <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
              <script>
                SwaggerUIBundle({ url: '/openapi', dom_id: '#swagger-ui' });
              </script>
            </body>
            </html>
            """.formatted(title);
    }
}
