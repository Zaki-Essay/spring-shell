package com.example.nativecliapp.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDefinition {
    @NotBlank
    private String name;

    @NotBlank
    private String type;

    @Builder.Default
    private int size = 0;

    @Builder.Default
    private boolean nullable = true;

    private String defaultValue;

    @Builder.Default
    private boolean primaryKey = false;

    @Builder.Default
    private boolean unique = false;

    @Builder.Default
    private boolean autoIncrement = false;

    private String remarks;
}
