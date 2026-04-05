package com.stazy.backend.listing.dto;

import com.stazy.backend.common.enums.GenderCategory;
import com.stazy.backend.common.enums.RoomKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ListingUpsertRequest {

    @NotBlank(message = "Room title is required.")
    private String title;

    @NotBlank(message = "Location is required.")
    private String location;

    private String description;

    @NotNull(message = "Monthly rent is required.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly rent must be greater than zero.")
    private BigDecimal rentAmount;

    private RoomKind roomKind = RoomKind.PG;

    @NotNull(message = "Gender category is required.")
    private GenderCategory genderCategory;

    @NotNull(message = "Total capacity is required.")
    private Integer totalCapacity;

    private String amenitiesCsv;

    private MultipartFile[] roomImages;

    private MultipartFile ownerPhoto;

    private MultipartFile ownerLiveVideo;
}
