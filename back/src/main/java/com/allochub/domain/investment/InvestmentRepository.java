package com.allochub.domain.investment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InvestmentRepository extends JpaRepository<Investment, String> {

    @Query("SELECT COALESCE(SUM(i.investmentAmount), 0) FROM Investment i")
    int sumInvestmentAmount();
}
