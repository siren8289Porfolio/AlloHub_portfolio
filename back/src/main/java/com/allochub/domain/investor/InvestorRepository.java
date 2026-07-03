package com.allochub.domain.investor;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InvestorRepository extends JpaRepository<Investor, String> {

    @Query("SELECT COALESCE(SUM(i.investmentAmount), 0) FROM Investor i")
    int sumInvestmentAmount();

    @Query("SELECT COALESCE(SUM(i.allocationRatio), 0) FROM Investor i WHERE (:excludeId IS NULL OR i.id <> :excludeId)")
    double sumAllocationRatioExcluding(String excludeId);

    default double sumAllocationRatio() {
        return sumAllocationRatioExcluding(null);
    }

    List<Investor> findAllByOrderByCreatedAtAsc();

    Optional<Investor> findByName(String name);
}
