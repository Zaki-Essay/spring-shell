package com.example.nativecliapp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseInfo {
    private String productName;
    private String productVersion;
    private String driverName;
    private String driverVersion;
    private String url;
    private String username;
    private boolean supportsTransactions;
}
