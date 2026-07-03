package com.allochub.domain.investment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvestmentRequest(
        @JsonProperty("company_name") String companyNameSnake,
        @JsonProperty("companyName") String companyNameCamel,
        @JsonProperty("investment_amount") Integer investmentAmountSnake,
        @JsonProperty("investmentAmount") Integer investmentAmountCamel) {

    public String companyName() {
        if (companyNameCamel != null && !companyNameCamel.isBlank()) return companyNameCamel.trim();
        if (companyNameSnake != null && !companyNameSnake.isBlank()) return companyNameSnake.trim();
        return "";
    }

    public int investmentAmount() {
        Integer v = investmentAmountCamel != null ? investmentAmountCamel : investmentAmountSnake;
        return v != null ? v : 0;
    }
}
