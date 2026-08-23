package com.allochub.domain.distribution;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributionRepository extends JpaRepository<Distribution, String> {

    @EntityGraph(attributePaths = {"investment", "details", "details.investor"})
    List<Distribution> findByInvestmentIdInOrderByDistributionDateDesc(Collection<String> investmentIds);
}
