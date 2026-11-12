package com.shoplab.tests.config;

/**
 * Where the shop runs. Tests never hard-code "http://localhost:8080": they ask this class.
 * <p>
 * The value is looked up in this order (the first one found wins):
 * <ol>
 *   <li>Java system property, e.g. {@code mvn test -Dapi.url=http://other-host:8080}</li>
 *   <li>Environment variable, e.g. {@code $env:SHOPLAB_API_URL = "http://other-host:8080"} (handy in CI)</li>
 *   <li>Default: the local docker compose stack</li>
 * </ol>
 * So the same tests run against a laptop, a CI server or a test environment without code changes.
 */
public final class TestConfig {

    private TestConfig() {
    }

    /** Base URL of the REST API, without a trailing slash. */
    public static String apiUrl() {
        return withoutTrailingSlash(read("api.url", "SHOPLAB_API_URL", "http://localhost:8080"));
    }

    /** How long to wait for the API to become healthy before giving up (seconds). */
    public static int apiWaitSeconds() {
        return Integer.parseInt(read("api.waitSeconds", "SHOPLAB_API_WAIT_SECONDS", "60"));
    }

    private static String read(String systemProperty, String envVariable, String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(envVariable);
        }
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String withoutTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
