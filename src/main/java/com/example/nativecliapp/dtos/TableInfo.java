package com.example.nativecliapp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {
    private String name;
    private String schema;
    private String type;
    private String remarks;
    private String catalog;
    private long rowCount;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
