package com.foodiefleet.app.controller;

import com.foodiefleet.app.TestDatabase;
import com.foodiefleet.app.TestcontainersConfiguration;
import com.foodiefleet.app.cart.CartView;
import com.foodiefleet.app.model.MenuItem;
import com.foodiefleet.app.model.Order;
import com.foodiefleet.app.model.OrderItem;
import com.foodiefleet.app.model.OrderStatus;
import com.foodiefleet.app.model.Restaurant;
import com.foodiefleet.app.model.Role;
import com.foodiefleet.app.model.User;
import com.foodiefleet.app.repository.MenuItemRepository;
import com.foodiefleet.app.repository.OrderRepository;
import com.foodiefleet.app.repository.RestaurantRepository;
import com.foodiefleet.app.repository.UserRepository;
import com.foodiefleet.app.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CustomerOrderingIntegrationTest {

    private static final String ALICE = "alice@example.com";
    private static final String BOB = "bob@example.com";
    private static final String OWNER_A = "owner.a@example.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RestaurantRepository restaurantRepository;
    @Autowired
    private MenuItemRepository menuItemRepository;
    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private EmailService emailService;

    // One HTTP session per test so the session-scoped cart carries across requests
    private MockHttpSession session;

    private User alice;
    private User bob;
    private Restaurant pastaPlace;
    private MenuItem carbonara;
    private MenuItem risotto;
    private MenuItem burger;
    private MenuItem sushiRoll;

    @BeforeEach
    void setUp() {
        TestDatabase.truncateAll(jdbcTemplate);
        session = new MockHttpSession();

        alice = saveUser(ALICE, "Alice", Role.CUSTOMER);
        bob = saveUser(BOB, "Bob", Role.CUSTOMER);
        User ownerA = saveUser(OWNER_A, "Owner A", Role.RESTAURANT_OWNER);
        User ownerB = saveUser("owner.b@example.com", "Owner B", Role.RESTAURANT_OWNER);
        User ownerC = saveUser("owner.c@example.com", "Owner C", Role.RESTAURANT_OWNER);

        pastaPlace = saveRestaurant(ownerA, "Pasta Place", true);
        Restaurant burgerBarn = saveRestaurant(ownerB, "Burger Barn", true);
        Restaurant sushiSpot = saveRestaurant(ownerC, "Sushi Spot", false);

        carbonara = saveMenuItem(pastaPlace, "Carbonara", "12.50", true);
        risotto = saveMenuItem(pastaPlace, "Truffle Risotto", "18.00", false);
        burger = saveMenuItem(burgerBarn, "Cheeseburger", "9.00", true);
        sushiRoll = saveMenuItem(sushiSpot, "Salmon Roll", "7.00", true);
    }

    // --- Browsing ------------------------------------------------------------

    @Test
    void browse_listsOpenRestaurantsOnly() throws Exception {
        mockMvc.perform(get("/customer/home").with(customer(ALICE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pasta Place")))
                .andExpect(content().string(containsString("Burger Barn")))
                .andExpect(content().string(not(containsString("Sushi Spot"))));
    }

    @Test
    void search_filtersByNameIgnoringCase() throws Exception {
        mockMvc.perform(get("/customer/home").param("q", "PASTA").with(customer(ALICE)))
                .andExpect(model().attribute("restaurants", hasSize(1)))
                .andExpect(content().string(containsString("Pasta Place")))
                .andExpect(content().string(not(containsString("Burger Barn"))));
    }

    @Test
    void restaurantPage_showsOnlyAvailableItems() throws Exception {
        mockMvc.perform(get("/customer/restaurants/{id}", pastaPlace.getId()).with(customer(ALICE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Carbonara")))
                .andExpect(content().string(not(containsString("Truffle Risotto"))));
    }

    // --- Cart ----------------------------------------------------------------

    @Test
    void addToCart_cartShowsServerSideTotal() throws Exception {
        addToCart(carbonara, 2)
                .andExpect(redirectedUrl("/customer/restaurants/" + pastaPlace.getId()))
                .andExpect(flash().attribute("success", containsString("2 × Carbonara")));

        CartView cart = viewCart();
        assertThat(cart.itemCount()).isEqualTo(2);
        assertThat(cart.total()).isEqualByComparingTo("25.00");
    }

    @Test
    void addUnavailableItem_isRefused() throws Exception {
        addToCart(risotto, 1).andExpect(flash().attribute("error", containsString("not available")));
        assertThat(viewCart().isEmpty()).isTrue();
    }

    @Test
    void addFromClosedRestaurant_isRefused() throws Exception {
        addToCart(sushiRoll, 1).andExpect(flash().attribute("error", containsString("closed")));
        assertThat(viewCart().isEmpty()).isTrue();
    }

    @Test
    void addFromSecondRestaurant_isRefused() throws Exception {
        addToCart(carbonara, 1);
        addToCart(burger, 1).andExpect(flash().attribute("error", containsString("Pasta Place")));

        CartView cart = viewCart();
        assertThat(cart.restaurant().getName()).isEqualTo("Pasta Place");
        assertThat(cart.lines()).extracting(line -> line.item().getName()).containsExactly("Carbonara");
    }

    @Test
    void addMoreThanMaxQuantity_isRefused() throws Exception {
        addToCart(carbonara, 21).andExpect(flash().attribute("error", containsString("between 1 and 20")));
        assertThat(viewCart().isEmpty()).isTrue();
    }

    @Test
    void updateAndRemoveCartLines() throws Exception {
        addToCart(carbonara, 2);

        mockMvc.perform(post("/customer/cart/update").session(session).with(customer(ALICE)).with(csrf())
                        .param("menuItemId", carbonara.getId().toString()).param("quantity", "5"))
                .andExpect(redirectedUrl("/customer/cart"));
        assertThat(viewCart().itemCount()).isEqualTo(5);

        mockMvc.perform(post("/customer/cart/update").session(session).with(customer(ALICE)).with(csrf())
                .param("menuItemId", carbonara.getId().toString()).param("quantity", "0"));
        assertThat(viewCart().isEmpty()).isTrue();
    }

    // --- Checkout ------------------------------------------------------------

    @Test
    void checkout_createsOrderAtCurrentDatabasePrices_clearsCart_andSendsConfirmation() throws Exception {
        addToCart(carbonara, 2);
        // The owner raises the price after the item went into the cart: checkout must charge the new price
        carbonara.setPrice(new BigDecimal("13.00"));
        menuItemRepository.save(carbonara);

        MvcResult result = checkout("8 Orchard Lane")
                .andExpect(redirectedUrlPattern("/customer/orders/*"))
                .andExpect(flash().attribute("success", containsString("Order placed")))
                .andReturn();

        Long orderId = Long.valueOf(result.getResponse().getRedirectedUrl().replaceAll("\\D+", ""));
        transactionTemplate.executeWithoutResult(tx -> {
            Order order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
            assertThat(order.getCustomer().getEmail()).isEqualTo(ALICE);
            assertThat(order.getRestaurant().getName()).isEqualTo("Pasta Place");
            assertThat(order.getDeliveryAddress()).isEqualTo("8 Orchard Lane");
            assertThat(order.getTotalPrice()).isEqualByComparingTo("26.00");
            assertThat(order.getItems()).singleElement().satisfies(item -> {
                assertThat(item.getQuantity()).isEqualTo(2);
                assertThat(item.getPriceAtPurchase()).isEqualByComparingTo("13.00");
            });
        });
        assertThat(viewCart().isEmpty()).isTrue();
        verify(emailService, timeout(2000)).sendOrderConfirmation(argThat(event ->
                event.orderId().equals(orderId) && event.total().compareTo(new BigDecimal("26.00")) == 0
                        && event.customerEmail().equals(ALICE)));
    }

    @Test
    void checkout_withBlankAddress_showsFieldError() throws Exception {
        addToCart(carbonara, 1);

        checkout("  ")
                .andExpect(status().isOk())
                .andExpect(view().name("customer/cart"))
                .andExpect(model().attributeHasFieldErrors("createOrderRequest", "deliveryAddress"));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void checkout_whenItemBecameUnavailable_isRefused() throws Exception {
        addToCart(carbonara, 1);
        carbonara.setAvailable(false);
        menuItemRepository.save(carbonara);

        checkout("8 Orchard Lane")
                .andExpect(redirectedUrl("/customer/cart"))
                .andExpect(flash().attribute("error", containsString("no longer available")));

        assertThat(orderRepository.count()).isZero();
        verify(emailService, never()).sendOrderConfirmation(any());
    }

    @Test
    void checkout_whenRestaurantClosed_isRefused() throws Exception {
        addToCart(carbonara, 1);
        pastaPlace.setActive(false);
        restaurantRepository.save(pastaPlace);

        checkout("8 Orchard Lane").andExpect(flash().attribute("error", containsString("closed")));
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void checkout_withEmptyCart_isRefused() throws Exception {
        checkout("8 Orchard Lane")
                .andExpect(redirectedUrl("/customer/cart"))
                .andExpect(flash().attribute("error", containsString("empty")));
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void cartPage_prefillsLastDeliveryAddress() throws Exception {
        saveOrder(alice, OrderStatus.DELIVERED, "221 Baker Street");

        mockMvc.perform(get("/customer/cart").session(session).with(customer(ALICE)))
                .andExpect(model().attribute("createOrderRequest",
                        org.hamcrest.Matchers.hasProperty("deliveryAddress", org.hamcrest.Matchers.is("221 Baker Street"))));
    }

    // --- Orders --------------------------------------------------------------

    @Test
    void myOrders_showsOnlyOwnOrders() throws Exception {
        Order alicesOrder = saveOrder(alice, OrderStatus.PLACED, "Alice Street");
        saveOrder(bob, OrderStatus.PLACED, "Bob Street");

        mockMvc.perform(get("/customer/orders").with(customer(ALICE)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("orders", hasSize(1)))
                .andExpect(content().string(containsString("/customer/orders/" + alicesOrder.getId())));
    }

    @Test
    void otherCustomersOrder_isNotFound() throws Exception {
        Order bobsOrder = saveOrder(bob, OrderStatus.PLACED, "Bob Street");

        mockMvc.perform(get("/customer/orders/{id}", bobsOrder.getId()).with(customer(ALICE)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/customer/orders/{id}/cancel", bobsOrder.getId()).with(customer(ALICE)).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(orderRepository.findById(bobsOrder.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void cancel_placedOrder_cancelsIt() throws Exception {
        Order order = saveOrder(alice, OrderStatus.PLACED, "Alice Street");

        mockMvc.perform(post("/customer/orders/{id}/cancel", order.getId()).with(customer(ALICE)).with(csrf()))
                .andExpect(redirectedUrl("/customer/orders/" + order.getId()))
                .andExpect(flash().attributeExists("success"));

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_acceptedOrder_isRefused() throws Exception {
        Order order = saveOrder(alice, OrderStatus.ACCEPTED, "Alice Street");

        mockMvc.perform(post("/customer/orders/{id}/cancel", order.getId()).with(customer(ALICE)).with(csrf()))
                .andExpect(flash().attribute("error", containsString("can no longer be cancelled")));

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    // --- Acceptance: customer orders, owner prepares, customer sees progress ---

    @Test
    void acceptance_customerOrders_ownerPrepares_customerSeesReady() throws Exception {
        addToCart(carbonara, 1);
        MvcResult result = checkout("8 Orchard Lane").andReturn();
        String orderUrl = result.getResponse().getRedirectedUrl();
        Long orderId = Long.valueOf(orderUrl.replaceAll("\\D+", ""));

        mockMvc.perform(get("/restaurant/dashboard").with(user(OWNER_A).roles("RESTAURANT_OWNER")))
                .andExpect(content().string(containsString("Order #<span>" + orderId + "</span>")));

        for (OrderStatus next : List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP)) {
            mockMvc.perform(post("/restaurant/orders/{id}/status", orderId)
                            .with(user(OWNER_A).roles("RESTAURANT_OWNER")).with(csrf())
                            .param("status", next.name()))
                    .andExpect(flash().attributeExists("success"));
        }

        mockMvc.perform(get(orderUrl).with(customer(ALICE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ready, waiting for a driver")))
                .andExpect(content().string(not(containsString("Cancel order"))));
    }

    // --- Helpers -------------------------------------------------------------

    private RequestPostProcessor customer(String email) {
        return user(email).roles("CUSTOMER");
    }

    private org.springframework.test.web.servlet.ResultActions addToCart(MenuItem item, int quantity) throws Exception {
        return mockMvc.perform(post("/customer/cart/add").session(session).with(customer(ALICE)).with(csrf())
                .param("menuItemId", item.getId().toString())
                .param("restaurantId", item.getRestaurant().getId().toString())
                .param("quantity", String.valueOf(quantity)));
    }

    private org.springframework.test.web.servlet.ResultActions checkout(String address) throws Exception {
        return mockMvc.perform(post("/customer/checkout").session(session).with(customer(ALICE)).with(csrf())
                .param("deliveryAddress", address));
    }

    private CartView viewCart() throws Exception {
        MvcResult result = mockMvc.perform(get("/customer/cart").session(session).with(customer(ALICE)))
                .andExpect(status().isOk())
                .andReturn();
        return (CartView) result.getModelAndView().getModel().get("cart");
    }

    private User saveUser(String email, String name, Role role) {
        return userRepository.save(User.builder()
                .email(email).password("not-used").fullName(name).role(role).enabled(true).build());
    }

    private Restaurant saveRestaurant(User owner, String name, boolean active) {
        return restaurantRepository.save(Restaurant.builder()
                .owner(owner).name(name).address("1 Test Street").active(active).build());
    }

    private MenuItem saveMenuItem(Restaurant restaurant, String name, String price, boolean available) {
        return menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant).name(name).price(new BigDecimal(price)).available(available).build());
    }

    private Order saveOrder(User customer, OrderStatus status, String address) {
        Order order = Order.builder()
                .customer(customer).restaurant(pastaPlace).status(status)
                .deliveryAddress(address).totalPrice(carbonara.getPrice()).build();
        order.getItems().add(OrderItem.builder()
                .order(order).menuItem(carbonara).quantity(1).priceAtPurchase(carbonara.getPrice()).build());
        return orderRepository.save(order);
    }
}
