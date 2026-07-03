package com.allochub.lib;

import static org.junit.jupiter.api.Assertions.*;
import com.allochub.lib.AllocationCalculator.AllocationResult;
import com.allochub.lib.AllocationCalculator.RatioItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class AllocationCalculatorTest {

    @Test
    void allocatesRemainderToLastInvestor() {
        List<AllocationResult> results = AllocationCalculator.allocateByRatio(
                100000,
                List.of(new RatioItem("a", 33.3), new RatioItem("b", 33.3), new RatioItem("c", 33.4)));

        int sum = results.stream().mapToInt(AllocationResult::amount).sum();
        assertEquals(100000, sum);
        assertEquals(100000 - results.get(0).amount() - results.get(1).amount(), results.get(2).amount());
    }

    @Test
    void returnsEmptyForZeroAmount() {
        assertTrue(AllocationCalculator.allocateByRatio(0, List.of(new RatioItem("a", 50))).isEmpty());
    }
}
