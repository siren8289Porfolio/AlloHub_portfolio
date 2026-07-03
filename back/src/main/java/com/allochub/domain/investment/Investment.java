package com.allochub.domain.investment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "investments")
public class Investment {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private int investmentAmount;

    @Column(nullable = false)
    private Instant investmentDate = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestmentStatus status = InvestmentStatus.active;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private String createdBy;

    @OneToMany(mappedBy = "investment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvestorInvestment> investorInvestments = new ArrayList<>();

    public String getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public int getInvestmentAmount() {
        return investmentAmount;
    }

    public void setInvestmentAmount(int investmentAmount) {
        this.investmentAmount = investmentAmount;
    }

    public List<InvestorInvestment> getInvestorInvestments() {
        return investorInvestments;
    }

    public Instant getInvestmentDate() {
        return investmentDate;
    }

    public InvestmentStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
