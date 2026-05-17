package com.example.smartBiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveGoodsRequest {

    @NotNull(message = "Actual delivery date is required")
    private LocalDate actualDeliveryDate;

    @Valid
    @NotNull(message = "Received items are required")
    private List<ReceivedItemDto> receivedItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceivedItemDto {
        @NotNull(message = "Line item ID is required")
        private Long lineItemId;

        @NotNull(message = "Quantity received is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        private Integer quantityReceived;

        private String batchNumber;
    }
}
