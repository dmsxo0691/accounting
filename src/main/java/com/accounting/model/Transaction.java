package com.accounting.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    private String timestamp;
    private long income;
    private long outcome;
    private long balance;
    private String description;
    private String location;
}
