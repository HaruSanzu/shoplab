package com.shoplab.tests.ui.steps;

import com.shoplab.tests.api.client.CartClient;
import com.shoplab.tests.api.client.OrdersClient;
import com.shoplab.tests.api.model.OrderModels.Order;
import com.shoplab.tests.data.TestData;
import com.shoplab.tests.ui.context.UiContext;
import com.shoplab.tests.ui.pages.ConfirmModal;
import com.shoplab.tests.ui.pages.OrdersPage;
import com.shoplab.tests.ui.support.ProductCatalog;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps for the order history page. */
public class OrderSteps {

    private final UiContext ui;

    public OrderSteps(UiContext ui) {
        this.ui = ui;
    }

    private OrdersPage ordersPage() {
        return new OrdersPage(ui.driver());
    }

    /** ARRANGE THROUGH THE API: the order already exists, the scenario tests only the history page. */
    @Given("I have placed an order for {int} {string}")
    public void placedOrder(int quantity, String productTitle) {
        String token = ui.customer().token();
        new CartClient(token).addItem(ProductCatalog.idOf(productTitle), quantity).then().statusCode(200);
        Order order = new OrdersClient(token).place(TestData.cardOrder())
                .then().statusCode(201)
                .extract().as(Order.class);
        ui.setOrderNumber(order.orderNumber());
    }

    @When("I open my orders")
    public void openOrders() {
        ordersPage().open();
    }

    @When("I cancel my order")
    public void cancelMyOrder() {
        ordersPage().clickCancel(ui.orderNumber());
    }

    @When("I confirm the cancellation")
    public void confirmCancellation() {
        ordersPage().confirmInModal();
    }

    @Then("I see {int} order(s)")
    public void seeOrders(int count) {
        assertThat(ordersPage().numberOfOrders()).isEqualTo(count);
    }

    @Then("my order has the status {string}")
    public void orderStatus(String status) {
        assertThat(ordersPage().statusOf(ui.orderNumber())).isEqualTo(status);
    }

    @Then("my order total is {string}")
    public void orderTotal(String total) {
        assertThat(ordersPage().totalOf(ui.orderNumber())).isEqualTo(total);
    }

    @Then("my order can no longer be cancelled")
    public void cannotCancel() {
        assertThat(ordersPage().canCancel(ui.orderNumber())).isFalse();
    }

    @Then("the confirmation dialog asks {string} for my order")
    public void dialogForMyOrder(String title) {
        ConfirmModal modal = new ConfirmModal(ui.driver()).waitUntilOpen();
        assertThat(modal.title()).isEqualTo(title);
        assertThat(modal.text()).isEqualTo("Cancel order " + ui.orderNumber() + "?");
    }

    @Then("I see the empty order history {string}")
    public void emptyHistory(String title) {
        assertThat(ordersPage().waitUntilLoaded().noOrdersTitle()).isEqualTo(title);
    }
}
