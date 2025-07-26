package com.accounting.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorizedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사업체 ID (거래가 속한 회사 구분용 식별자)
    private String companyId;

    // 거래가 발생한 날짜 (예: "2023-07-25")
    private String transactionDate;

    // 거래 내역 설명 (예: "스타벅스 결제", "월급 입금" 등)
    private String transactionDescription;

    // 입금 금액 (있으면 값, 없으면 null 또는 0)
    private Long depositAmount;

    // 출금 금액 (있으면 값, 없으면 null 또는 0)
    private Long withdrawAmount;

    // 거래 후 계좌 잔액
    private Long balanceAmount;

    // 거래가 발생한 장소 (예: 가맹점 이름, 위치 등)
    private String transactionPlace;

    // 분류된 계정과목 ID (규칙에 의해 자동 분류된 카테고리 식별자)
    private String categoryId;

    // 분류된 계정과목 이름 (예: "식비", "교통비", "급여" 등)
    private String categoryName;

    private boolean classified; // 분류 성공 여부

}