package com.example.nativecliapp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDefinition {
    private String name;
    private String type;
    private int size;
    private boolean nullable;
    private String defaultValue;
    private boolean primaryKey;
}
