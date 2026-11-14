package com.shoplab.tests.ui.support;

import com.shoplab.tests.api.client.ProductsClient;
import com.shoplab.tests.api.model.ProductModels.Product;
import com.shoplab.tests.api.model.ProductModels.ProductPage;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Feature files talk like people: "Selenium in Practice", not "product 7".
 * When a step needs the id (to open #/product/7 or to fill the cart through the API),
 * it asks this class. The list is read from the API once and then kept in memory.
 */
public final class ProductCatalog {

    private static Map<String, Product> byTitle;

    private ProductCatalog() {
    }

    public static long idOf(String title) {
        return product(title).id();
    }

    public static Product product(String title) {
        Product product = all().get(title);
        if (product == null) {
            throw new IllegalArgumentException("No product titled \"" + title + "\". Known titles: " + all().keySet());
        }
        return product;
    }

    private static synchronized Map<String, Product> all() {
        if (byTitle == null) {
            ProductPage page = new ProductsClient().search(Map.of("size", 100))
                    .then().statusCode(200)
                    .extract().as(ProductPage.class);
            byTitle = page.items().stream().collect(Collectors.toMap(Product::title, Function.identity()));
        }
        return byTitle;
    }
}
