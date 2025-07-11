package com.example.nativecliapp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo {
    private String name;
    private String type;
    private int size;
    private int decimalDigits;
    private boolean nullable;
    private String defaultValue;
    private int position;
    private String remarks;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private boolean isIndexed;
    private boolean isUnique;
    private boolean isAutoIncrement;
}
