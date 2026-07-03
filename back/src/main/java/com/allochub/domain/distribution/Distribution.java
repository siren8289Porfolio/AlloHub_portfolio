package com.allochub.domain.distribution;

import com.allochub.domain.investment.Investment;
import com.allochub.domain.investor.Investor;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "distributions")
public class Distribution {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id")
    private Investment investment;

    @Column(nullable = false)
    private int distributionAmount;

    @Column(nullable = false)
    private String distributionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DistributionStatus status = DistributionStatus.completed;

    @Column(nullable = false)
    private Instant distributionDate = Instant.now();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private String createdBy;

    @OneToMany(mappedBy = "distribution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DistributionDetail> details = new ArrayList<>();

    public String getId() {
        return id;
    }

    public Investment getInvestment() {
        return investment;
    }

    public void setInvestment(Investment investment) {
        this.investment = investment;
    }

    public int getDistributionAmount() {
        return distributionAmount;
    }

    public void setDistributionAmount(int distributionAmount) {
        this.distributionAmount = distributionAmount;
    }

    public String getDistributionType() {
        return distributionType;
    }

    public void setDistributionType(String distributionType) {
        this.distributionType = distributionType;
    }

    public List<DistributionDetail> getDetails() {
        return details;
    }

    public Instant getDistributionDate() {
        return distributionDate;
    }

    public DistributionStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
