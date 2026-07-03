package com.allochub.domain.investment;

import com.allochub.domain.investor.Investor;
import jakarta.persistence.*;

@Entity
@Table(
        name = "investor_investments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"investor_id", "investment_id"}))
public class InvestorInvestment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id")
    private Investor investor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id")
    private Investment investment;

    @Column(nullable = false)
    private int allocatedAmount;

    public Investor getInvestor() {
        return investor;
    }

    public void setInvestor(Investor investor) {
        this.investor = investor;
    }

    public Investment getInvestment() {
        return investment;
    }

    public void setInvestment(Investment investment) {
        this.investment = investment;
    }

    public int getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(int allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public String getInvestorId() {
        return investor != null ? investor.getId() : null;
    }
}
