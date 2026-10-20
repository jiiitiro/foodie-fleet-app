package com.foodiefleet.app.controller;

import com.foodiefleet.app.TestDatabase;
import com.foodiefleet.app.TestcontainersConfiguration;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// Owner A acts on their own data and tries to touch owner B's (SECURITY.md S2)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class RestaurantControllerIntegrationTest {

    private static final String OWNER_A = "owner.a@example.com";
    private static final String OWNER_B = "owner.b@example.com";
    private static final String NEW_OWNER = "new.owner@example.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
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

    private Restaurant restaurantA;
    private MenuItem itemA;
    private MenuItem itemB;
    private Order orderA;
    private Order orderB;

    @BeforeEach
    void setUp() {
        TestDatabase.truncateAll(jdbcTemplate);

        User ownerA = saveUser(OWNER_A, Role.RESTAURANT_OWNER);
        User ownerB = saveUser(OWNER_B, Role.RESTAURANT_OWNER);
        saveUser(NEW_OWNER, Role.RESTAURANT_OWNER);
        User customer = saveUser("customer@example.com", Role.CUSTOMER);

        restaurantA = saveRestaurant(ownerA, "Pasta Place");
        Restaurant restaurantB = saveRestaurant(ownerB, "Burger Barn");
        itemA = saveMenuItem(restaurantA, "Carbonara", "12.50");
        itemB = saveMenuItem(restaurantB, "Cheeseburger", "9.00");
        orderA = saveOrder(customer, restaurantA, itemA);
        orderB = saveOrder(customer, restaurantB, itemB);
    }

    // --- Dashboard & profile -------------------------------------------------

    @Test
    void ownerWithoutRestaurant_seesSetupPage() throws Exception {
        mockMvc.perform(get("/restaurant/dashboard").with(owner(NEW_OWNER)))
                .andExpect(status().isOk())
                .andExpect(view().name("restaurant/setup"));
    }

    @Test
    void ownerWithoutRestaurant_canCreateIt() throws Exception {
        mockMvc.perform(post("/restaurant/profile").with(owner(NEW_OWNER)).with(csrf())
                        .param("name", "Sushi Spot")
                        .param("description", "Fresh rolls")
                        .param("address", "1 Harbour St")
                        .param("active", "true"))
                .andExpect(redirectedUrl("/restaurant/dashboard"));

        User newOwner = userRepository.findByEmail(NEW_OWNER).orElseThrow();
        Restaurant created = restaurantRepository.findByOwner(newOwner).orElseThrow();
        assertThat(created.getName()).isEqualTo("Sushi Spot");
        assertThat(created.getDescription()).isEqualTo("Fresh rolls");
        assertThat(created.getActive()).isTrue();
    }

    @Test
    void invalidProfile_showsErrorsOnSetupPage() throws Exception {
        mockMvc.perform(post("/restaurant/profile").with(owner(NEW_OWNER)).with(csrf())
                        .param("name", "")
                        .param("address", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("restaurant/setup"))
                .andExpect(model().attributeHasFieldErrors("restaurantRequest", "name", "address"));

        assertThat(restaurantRepository.count()).isEqualTo(2);
    }

    @Test
    void dashboard_showsOnlyOwnMenuAndOrders() throws Exception {
        mockMvc.perform(get("/restaurant/dashboard").with(owner(OWNER_A)))
                .andExpect(status().isOk())
                .andExpect(view().name("restaurant/dashboard"))
                .andExpect(content().string(containsString("Pasta Place")))
                .andExpect(content().string(containsString("Carbonara")))
                .andExpect(content().string(not(containsString("Cheeseburger"))))
                .andExpect(content().string(not(containsString("Burger Barn"))));
    }

    @Test
    void toggleOpen_closesAndReopensRestaurant() throws Exception {
        mockMvc.perform(post("/restaurant/toggle-open").with(owner(OWNER_A)).with(csrf()))
                .andExpect(redirectedUrl("/restaurant/dashboard"));
        assertThat(restaurantRepository.findById(restaurantA.getId()).orElseThrow().getActive()).isFalse();

        mockMvc.perform(post("/restaurant/toggle-open").with(owner(OWNER_A)).with(csrf()));
        assertThat(restaurantRepository.findById(restaurantA.getId()).orElseThrow().getActive()).isTrue();
    }

    // --- Menu ----------------------------------------------------------------

    @Test
    void addMenuItem_isAddedToOwnRestaurant() throws Exception {
        mockMvc.perform(post("/restaurant/menu/add").with(owner(OWNER_A)).with(csrf())
                        .param("name", "Lasagne")
                        .param("price", "14.00")
                        .param("available", "true"))
                .andExpect(redirectedUrl("/restaurant/dashboard"));

        assertThat(menuItemRepository.findByRestaurantOrderByNameAsc(restaurantA))
                .extracting(MenuItem::getName)
                .containsExactly("Carbonara", "Lasagne");
    }

    @Test
    void addMenuItem_withInvalidPrice_showsErrors() throws Exception {
        mockMvc.perform(post("/restaurant/menu/add").with(owner(OWNER_A)).with(csrf())
                        .param("name", "Free Bread")
                        .param("price", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("restaurant/dashboard"))
                .andExpect(model().attributeHasFieldErrors("menuItemRequest", "price"));

        assertThat(menuItemRepository.count()).isEqualTo(2);
    }

    @Test
    void editPage_forOwnMenuItem_isPrefilled() throws Exception {
        mockMvc.perform(get("/restaurant/menu/edit/{id}", itemA.getId()).with(owner(OWNER_A)))
                .andExpect(status().isOk())
                .andExpect(view().name("restaurant/menu-edit"))
                .andExpect(content().string(containsString("value=\"Carbonara\"")));
    }

    @Test
    void editOwnMenuItem_updatesIt() throws Exception {
        mockMvc.perform(post("/restaurant/menu/edit/{id}", itemA.getId()).with(owner(OWNER_A)).with(csrf())
                        .param("name", "Carbonara Deluxe")
                        .param("price", "15.00")
                        .param("available", "true"))
                .andExpect(redirectedUrl("/restaurant/dashboard"));

        MenuItem updated = menuItemRepository.findById(itemA.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Carbonara Deluxe");
        assertThat(updated.getPrice()).isEqualByComparingTo("15.00");
    }

    @Test
    void otherOwnersMenuItem_isNotFound_forViewEditToggleAndDelete() throws Exception {
        mockMvc.perform(get("/restaurant/menu/edit/{id}", itemB.getId()).with(owner(OWNER_A)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/restaurant/menu/edit/{id}", itemB.getId()).with(owner(OWNER_A)).with(csrf())
                        .param("name", "Hacked")
                        .param("price", "0.01"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/restaurant/menu/toggle/{id}", itemB.getId()).with(owner(OWNER_A)).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/restaurant/menu/delete/{id}", itemB.getId()).with(owner(OWNER_A)).with(csrf()))
                .andExpect(status().isNotFound());

        MenuItem untouched = menuItemRepository.findById(itemB.getId()).orElseThrow();
        assertThat(untouched.getName()).isEqualTo("Cheeseburger");
        assertThat(untouched.isAvailable()).isTrue();
    }

    @Test
    void invalidEditOfOtherOwnersItem_isStillNotFound() throws Exception {
        // Validation errors must not leak whether another owner's item exists
        mockMvc.perform(post("/restaurant/menu/edit/{id}", itemB.getId()).with(owner(OWNER_A)).with(csrf())
                        .param("name", "")
                        .param("price", "-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOwnMenuItem_withoutOrders_removesIt() throws Exception {
        MenuItem unused = saveMenuItem(restaurantA, "Tiramisu", "6.00");

        mockMvc.perform(post("/restaurant/menu/delete/{id}", unused.getId()).with(owner(OWNER_A)).with(csrf()))
                .andExpect(redirectedUrl("/restaurant/dashboard"))
                .andExpect(flash().attributeExists("success"));

        assertThat(menuItemRepository.existsById(unused.getId())).isFalse();
    }

    @Test
    void deleteOwnMenuItem_withPastOrders_isRefused() throws Exception {
        mockMvc.perform(post("/restaurant/menu/delete/{id}", itemA.getId()).with(owner(OWNER_A)).with(csrf()))
                .andExpect(redirectedUrl("/restaurant/dashboard"))
                .andExpect(flash().attribute("error", containsString("past orders")));

        assertThat(menuItemRepository.existsById(itemA.getId())).isTrue();
    }

    // --- Orders --------------------------------------------------------------

    @Test
    void ownOrder_movesThroughAllowedStatuses() throws Exception {
        for (OrderStatus next : List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP)) {
            mockMvc.perform(post("/restaurant/orders/{id}/status", orderA.getId()).with(owner(OWNER_A)).with(csrf())
                            .param("status", next.name()))
                    .andExpect(redirectedUrl("/restaurant/dashboard"))
                    .andExpect(flash().attributeExists("success"));
            assertThat(orderRepository.findById(orderA.getId()).orElseThrow().getStatus()).isEqualTo(next);
        }
    }

    @Test
    void ownOrder_illegalTransition_isRefused() throws Exception {
        mockMvc.perform(post("/restaurant/orders/{id}/status", orderA.getId()).with(owner(OWNER_A)).with(csrf())
                        .param("status", "DELIVERED"))
                .andExpect(redirectedUrl("/restaurant/dashboard"))
                .andExpect(flash().attribute("error", containsString("can't move from PLACED to DELIVERED")));

        assertThat(orderRepository.findById(orderA.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void otherOwnersOrder_isNotFound() throws Exception {
        mockMvc.perform(post("/restaurant/orders/{id}/status", orderB.getId()).with(owner(OWNER_A)).with(csrf())
                        .param("status", "ACCEPTED"))
                .andExpect(status().isNotFound());

        assertThat(orderRepository.findById(orderB.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    // --- Helpers -------------------------------------------------------------

    private RequestPostProcessor owner(String email) {
        return user(email).roles("RESTAURANT_OWNER");
    }

    private User saveUser(String email, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .password("not-used")
                .fullName("Test " + role)
                .role(role)
                .enabled(true)
                .build());
    }

    private Restaurant saveRestaurant(User owner, String name) {
        return restaurantRepository.save(Restaurant.builder()
                .owner(owner)
                .name(name)
                .address("1 Test Street")
                .build());
    }

    private MenuItem saveMenuItem(Restaurant restaurant, String name, String price) {
        return menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant)
                .name(name)
                .price(new BigDecimal(price))
                .build());
    }

    private Order saveOrder(User customer, Restaurant restaurant, MenuItem item) {
        Order order = Order.builder()
                .customer(customer)
                .restaurant(restaurant)
                .deliveryAddress("2 Customer Road")
                .totalPrice(item.getPrice())
                .build();
        order.getItems().add(OrderItem.builder()
                .order(order)
                .menuItem(item)
                .quantity(1)
                .priceAtPurchase(item.getPrice())
                .build());
        return orderRepository.save(order);
    }
}
