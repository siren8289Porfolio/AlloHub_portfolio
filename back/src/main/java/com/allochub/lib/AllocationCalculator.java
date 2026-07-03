package com.allochub.lib;

import java.util.ArrayList;
import java.util.List;

public final class AllocationCalculator {

    private AllocationCalculator() {}

    public record RatioItem(String id, double ratio) {}

    public record AllocationResult(String id, int amount) {}

    /** BR-007: remainder goes to last investor */
    public static List<AllocationResult> allocateByRatio(int totalAmount, List<RatioItem> items) {
        if (items.isEmpty() || totalAmount <= 0) {
            return List.of();
        }

        List<AllocationResult> results = new ArrayList<>();
        int allocated = 0;

        for (int i = 0; i < items.size(); i++) {
            RatioItem item = items.get(i);
            boolean isLast = i == items.size() - 1;
            if (isLast) {
                results.add(new AllocationResult(item.id(), totalAmount - allocated));
            } else {
                int amount = (int) Math.floor(totalAmount * (item.ratio() / 100.0));
                results.add(new AllocationResult(item.id(), amount));
                allocated += amount;
            }
        }
        return results;
    }
}
