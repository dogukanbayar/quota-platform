package com.saasplatform.quota.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Generic page wrapper")
public record PageResponse<T>(
        @Schema(description = "Page content") List<T> content,
        @Schema(description = "Zero based page index", example = "0") int page,
        @Schema(description = "Page size", example = "10") int size,
        @Schema(description = "Total elements", example = "57") long totalElements,
        @Schema(description = "Total pages", example = "6") int totalPages) {
}
