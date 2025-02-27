package com.example.nativecliapp.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class Product {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private Double price;
}
