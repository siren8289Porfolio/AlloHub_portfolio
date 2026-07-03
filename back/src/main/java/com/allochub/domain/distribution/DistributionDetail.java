package com.allochub.domain.distribution;

import com.allochub.domain.investor.Investor;
import jakarta.persistence.*;

@Entity
@Table(
        name = "distribution_details",
        uniqueConstraints = @UniqueConstraint(columnNames = {"distribution_id", "investor_id"}))
public class DistributionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_id")
    private Distribution distribution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id")
    private Investor investor;

    @Column(nullable = false)
    private int distributedAmount;

    public Distribution getDistribution() {
        return distribution;
    }

    public void setDistribution(Distribution distribution) {
        this.distribution = distribution;
    }

    public Investor getInvestor() {
        return investor;
    }

    public void setInvestor(Investor investor) {
        this.investor = investor;
    }

    public int getDistributedAmount() {
        return distributedAmount;
    }

    public void setDistributedAmount(int distributedAmount) {
        this.distributedAmount = distributedAmount;
    }
}
