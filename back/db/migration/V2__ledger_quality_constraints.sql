ALTER TABLE investors
    ADD CONSTRAINT chk_investors_investment_amount_positive CHECK (investment_amount > 0),
    ADD CONSTRAINT chk_investors_allocation_ratio_range CHECK (allocation_ratio > 0 AND allocation_ratio <= 100),
    ADD CONSTRAINT chk_investors_cumulative_distribution_non_negative CHECK (cumulative_distribution >= 0);

ALTER TABLE investments
    ADD CONSTRAINT chk_investments_investment_amount_positive CHECK (investment_amount > 0);

ALTER TABLE investor_investments
    ADD CONSTRAINT chk_investor_investments_allocated_amount_non_negative CHECK (allocated_amount >= 0);

ALTER TABLE distributions
    ADD CONSTRAINT chk_distributions_distribution_amount_positive CHECK (distribution_amount > 0);

ALTER TABLE distribution_details
    ADD CONSTRAINT chk_distribution_details_distributed_amount_non_negative CHECK (distributed_amount >= 0);
