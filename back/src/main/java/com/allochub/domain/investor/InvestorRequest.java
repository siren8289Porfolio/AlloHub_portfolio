package com.allochub.domain.investor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvestorRequest(
        String name,
        @JsonProperty("investment_amount") Integer investmentAmountSnake,
        @JsonProperty("investmentAmount") Integer investmentAmountCamel,
        @JsonProperty("allocation_ratio") Double allocationRatioSnake,
        @JsonProperty("investment_ratio") Double investmentRatio,
        @JsonProperty("allocationRatio") Double allocationRatioCamel) {

    public String name() {
        return name != null ? name.trim() : "";
    }

    public int investmentAmount() {
        Integer v = investmentAmountCamel != null ? investmentAmountCamel : investmentAmountSnake;
        return v != null ? v : 0;
    }

    public double allocationRatio() {
        if (allocationRatioCamel != null) return allocationRatioCamel;
        if (allocationRatioSnake != null) return allocationRatioSnake;
        if (investmentRatio != null) return investmentRatio;
        return 0;
    }
}
