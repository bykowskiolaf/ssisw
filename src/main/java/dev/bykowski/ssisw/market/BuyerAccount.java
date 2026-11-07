package dev.bykowski.ssisw.market;

public class BuyerAccount {

    private final String buyerName;
    private long budgetPln;

    public BuyerAccount(String buyerName, long budgetPln) {
        this.buyerName = buyerName;
        this.budgetPln = budgetPln;
    }

    public boolean canAfford(long amountPln) {
        return amountPln <= budgetPln;
    }

    public void pay(long amountPln) {
        if (!canAfford(amountPln)) {
            throw new IllegalStateException("Purchase exceeds the buyer budget");
        }
        budgetPln -= amountPln;
    }

    public void refund(long amountPln) {
        if (amountPln < 0) {
            throw new IllegalArgumentException("Refund cannot be negative");
        }
        budgetPln += amountPln;
    }

    public String buyerName() {
        return buyerName;
    }

    public long budgetPln() {
        return budgetPln;
    }
}
