package com.accounting.controller;

import com.accounting.model.entity.CategorizedTransaction;
import com.accounting.service.AccountingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounting")
public class AccountingController {

    private final AccountingService accountingService;

    public AccountingController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    // 1. 자동 회계 처리 API
    @PostMapping("/process")
    public ResponseEntity<?> processAccounting(
            @RequestParam("transactions") MultipartFile transactions,
            @RequestParam("rules") MultipartFile rules) {

        try {
            List<CategorizedTransaction> processed = accountingService.processTransactionsWithRules(transactions, rules);
            return ResponseEntity.ok(processed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("처리 실패: " + e.getMessage());
        }
    }

    // 2. 사업체별 분류 결과 조회 API
    @GetMapping("/records")
    public ResponseEntity<?> getRecordsByCompanyId(@RequestParam("companyId") String companyId) {
        List<CategorizedTransaction> records = accountingService.getRecordsByCompanyId(companyId);
        return ResponseEntity.ok(records);
    }
}