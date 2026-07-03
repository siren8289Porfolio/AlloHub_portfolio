package com.allochub.domain.investor;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investors")
public class Investor {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int investmentAmount;

    @Column(nullable = false)
    private double allocationRatio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestorStatus status = InvestorStatus.active;

    @Column(nullable = false)
    private int cumulativeDistribution = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private String createdBy;

    private Instant updatedAt;
    private String updatedBy;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getInvestmentAmount() {
        return investmentAmount;
    }

    public void setInvestmentAmount(int investmentAmount) {
        this.investmentAmount = investmentAmount;
    }

    public double getAllocationRatio() {
        return allocationRatio;
    }

    public void setAllocationRatio(double allocationRatio) {
        this.allocationRatio = allocationRatio;
    }

    public InvestorStatus getStatus() {
        return status;
    }

    public int getCumulativeDistribution() {
        return cumulativeDistribution;
    }

    public void setCumulativeDistribution(int cumulativeDistribution) {
        this.cumulativeDistribution = cumulativeDistribution;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
