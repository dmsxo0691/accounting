package com.accounting.service;

import com.accounting.model.CategoryRule;
import com.accounting.model.CompanyRule;
import com.accounting.model.RuleSet;
import com.accounting.model.Transaction;
import com.accounting.model.entity.CategorizedTransaction;
import com.accounting.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountingService {

    private final TransactionRepository transactionRepository;

    public AccountingService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // 자동분류 및 저장
    public List<CategorizedTransaction> processTransactionsWithRules(
            MultipartFile transactionsFile,
            MultipartFile rulesFile) throws IOException {

        // 1. rules.json 파싱
        ObjectMapper mapper = new ObjectMapper();
        RuleSet ruleSet = mapper.readValue(rulesFile.getInputStream(), RuleSet.class);

        // 2. transactions.csv 파싱
        List<Transaction> transactionList = parseTransactionList(transactionsFile.getInputStream());

        // 3. 자동 분류
        List<CategorizedTransaction> categorizedList = parseAndCategorize(ruleSet, transactionList);

        // 4. DB 저장
        return transactionRepository.saveAll(categorizedList);
    }

    // 특정 회사 거래내역 조회
    public List<CategorizedTransaction> getRecordsByCompanyId(String companyId) {
        return transactionRepository.findByCompanyId(companyId);
    }

    private List<CategorizedTransaction> parseAndCategorize(RuleSet ruleSet, List<Transaction> transactions) {
        List<CategorizedTransaction> result = new ArrayList<>();

        for (Transaction tx : transactions) {
            CategorizedTransaction ct = buildCategorizedTransaction(tx);

            ruleSet.getCompanies().stream()
                    .flatMap(company -> company.getCategories().stream()
                            .flatMap(category -> category.getKeywords().stream()
                                    .filter(keyword -> tx.getDescription().contains(keyword))
                                    .map(keyword -> new Object[]{company, category})
                            )
                    )
                    .findFirst()
                    .ifPresentOrElse(
                            match -> {
                                CompanyRule company = (CompanyRule) match[0];
                                CategoryRule category = (CategoryRule) match[1];
                                ct.setCompanyId(company.getCompanyId());
                                ct.setCategoryId(category.getCategoryId());
                                ct.setCategoryName(category.getCategoryName());
                                ct.setClassified(true);
                            },
                            () -> {
                                ct.setCompanyId(null);
                                ct.setCategoryId("미분류");
                                ct.setCategoryName("미분류");
                                ct.setClassified(false);
                            }
                    );

            result.add(ct);
        }

        return result;
    }

    // 공통 빌더
    private CategorizedTransaction buildCategorizedTransaction(Transaction tx) {
        CategorizedTransaction ct = new CategorizedTransaction();
        ct.setTransactionDate(tx.getTimestamp());
        ct.setTransactionDescription(tx.getDescription());
        ct.setDepositAmount(tx.getIncome());
        ct.setWithdrawAmount(tx.getOutcome());
        ct.setBalanceAmount(tx.getBalance());
        ct.setTransactionPlace(tx.getLocation());
        return ct;
    }

    public List<Transaction> parseTransactionList(InputStream csvStream) throws IOException {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;  // 헤더 스킵
                    continue;
                }

                String[] cols = line.split(",", -1); // 빈값도 배열에 포함시키기 위해 -1 사용

                if (cols.length < 6) continue;  // 컬럼 부족 시 무시

                String timestamp = cols[0].trim();
                String description = cols[1].trim();
                long income = Long.parseLong(cols[2].trim());
                long outcome = Long.parseLong(cols[3].trim());
                long balance = Long.parseLong(cols[4].trim());
                String location = cols[5].trim();

                Transaction tx = new Transaction(timestamp, income, outcome, balance, description, location);
                transactions.add(tx);
            }
        }

        return transactions;
    }
}