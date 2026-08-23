package com.allochub.domain.investment;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InvestmentRepository extends JpaRepository<Investment, String> {

    @Query("SELECT COALESCE(SUM(i.investmentAmount), 0) FROM Investment i")
    int sumInvestmentAmount();

    @EntityGraph(attributePaths = {"investorInvestments", "investorInvestments.investor"})
    List<Investment> findByCompanyNameContainingIgnoreCaseOrderByInvestmentDateDesc(String companyName);
}
