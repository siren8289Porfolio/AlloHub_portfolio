package com.allochub.domain.distribution;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DistributionRequest(
        @JsonProperty("investment_id") String investmentIdSnake,
        @JsonProperty("investmentId") String investmentIdCamel,
        @JsonProperty("distribution_type") String distributionTypeSnake,
        @JsonProperty("distributionType") String distributionTypeCamel,
        @JsonProperty("distribution_amount") Integer distributionAmountSnake,
        @JsonProperty("distributionAmount") Integer distributionAmountCamel) {

    public String investmentId() {
        if (investmentIdCamel != null && !investmentIdCamel.isBlank()) return investmentIdCamel.trim();
        if (investmentIdSnake != null && !investmentIdSnake.isBlank()) return investmentIdSnake.trim();
        return "";
    }

    public String distributionType() {
        if (distributionTypeCamel != null && !distributionTypeCamel.isBlank()) {
            return distributionTypeCamel.trim();
        }
        if (distributionTypeSnake != null && !distributionTypeSnake.isBlank()) {
            return distributionTypeSnake.trim();
        }
        return "";
    }

    public int distributionAmount() {
        Integer v = distributionAmountCamel != null ? distributionAmountCamel : distributionAmountSnake;
        return v != null ? v : 0;
    }
}
