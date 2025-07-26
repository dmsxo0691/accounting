package com.accounting.repository;

import com.accounting.model.entity.CategorizedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<CategorizedTransaction, Long>{
    List<CategorizedTransaction> findByCompanyId(String companyId);
}