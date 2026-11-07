package dev.bykowski.ssisw.market;

import dev.bykowski.ssisw.car.Car;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NegotiationService {

    public static final double MAXIMUM_DISCOUNT = 0.15;
    public static final int MAXIMUM_ROUNDS = 3;

    public NegotiationResult negotiate(CarOffer offer, long initialOfferTotalPln, long buyerMaximumTotalPln) {
        Objects.requireNonNull(offer, "offer must not be null");
        Car car = Objects.requireNonNull(offer.car(), "car must not be null");
        long catalogTotal = car.totalCostPln();
        long minimumTotal = minimumTotal(car);
        long buyerMaximum = Math.clamp(buyerMaximumTotalPln, 0, catalogTotal);
        long buyerOffer = Math.clamp(initialOfferTotalPln, 0, buyerMaximum);
        List<NegotiationRound> history = new ArrayList<>();

        if (buyerMaximum < minimumTotal || buyerOffer < car.additionalCostPln()) {
            for (int round = 1; round <= MAXIMUM_ROUNDS; round++) {
                history.add(new NegotiationRound(round, buyerOffer, minimumTotal));
                buyerOffer = Math.min(buyerMaximum,
                        buyerOffer + Math.max(1, (buyerMaximum - buyerOffer) / 2));
            }
            return rejected(car, catalogTotal, minimumTotal, history);
        }

        for (int round = 1; round <= MAXIMUM_ROUNDS; round++) {
            if (buyerOffer >= minimumTotal) {
                history.add(new NegotiationRound(round, buyerOffer, buyerOffer));
                return accepted(car, catalogTotal, minimumTotal, buyerOffer, history);
            }

            long sellerCounter = Math.max(minimumTotal, midpoint(buyerOffer, catalogTotal));
            history.add(new NegotiationRound(round, buyerOffer, sellerCounter));
            if (sellerCounter <= buyerMaximum) {
                return accepted(car, catalogTotal, minimumTotal, sellerCounter, history);
            }
            buyerOffer = Math.min(buyerMaximum, midpoint(buyerOffer, sellerCounter));
        }
        return rejected(car, catalogTotal, minimumTotal, history);
    }

    public static long minimumTotal(Car car) {
        return Math.round(car.pricePln() * (1.0 - MAXIMUM_DISCOUNT)) + car.additionalCostPln();
    }

    private long midpoint(long lower, long upper) {
        return lower + (upper - lower) / 2;
    }

    private NegotiationResult accepted(
            Car car,
            long catalogTotal,
            long minimumTotal,
            long agreedTotal,
            List<NegotiationRound> history) {
        return new NegotiationResult(true, car.id(), catalogTotal, minimumTotal, agreedTotal,
                history.size(), List.copyOf(history), "ACCEPTED");
    }

    private NegotiationResult rejected(
            Car car,
            long catalogTotal,
            long minimumTotal,
            List<NegotiationRound> history) {
        return new NegotiationResult(false, car.id(), catalogTotal, minimumTotal, 0,
                history.size(), List.copyOf(history), "REJECTED");
    }
}
