package client;

public class ServiceUrls {

    public final String property;
    public final String purchasers;
    public final String analytics;

    public ServiceUrls(String property, String purchasers, String analytics) {
        this.property = property;
        this.purchasers = purchasers;
        this.analytics = analytics;
    }

    public static ServiceUrls fromEnv() {
        return new ServiceUrls(
            env("PROPERTY_URL",   "http://localhost:7071"),
            env("PURCHASERS_URL", "http://localhost:7072"),
            env("ANALYTICS_URL",  "http://localhost:7073")
        );
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}