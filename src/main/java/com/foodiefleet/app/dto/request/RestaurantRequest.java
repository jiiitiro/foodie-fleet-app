package com.foodiefleet.app.dto.request;

import com.foodiefleet.app.model.Restaurant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String description;

    @NotBlank
    @Size(max = 255)
    private String address;

    @Size(max = 30)
    private String phone;

    // New restaurants start open; the setup form's checkbox is pre-ticked
    @Builder.Default
    private boolean active = true;

    public static RestaurantRequest from(Restaurant restaurant) {
        return RestaurantRequest.builder()
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .address(restaurant.getAddress())
                .phone(restaurant.getPhone())
                .active(Boolean.TRUE.equals(restaurant.getActive()))
                .build();
    }
}
