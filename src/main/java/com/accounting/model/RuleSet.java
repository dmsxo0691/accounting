package com.accounting.model;

import lombok.Data;

import java.util.List;

@Data
public class RuleSet {
    private List<CompanyRule> companies;
}