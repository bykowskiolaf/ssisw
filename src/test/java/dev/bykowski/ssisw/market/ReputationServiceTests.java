package dev.bykowski.ssisw.market;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReputationServiceTests {

    @Test
    void calculatesAverageAndRejectsDuplicateTransactionRating() {
        ReputationService service = new ReputationService();
        service.rate("TX-1", "Seller01", 5);
        SellerReputation reputation = service.rate("TX-2", "Seller01", 4);

        assertThat(reputation.averageRating()).isEqualTo(4.5);
        assertThat(reputation.ratingCount()).isEqualTo(2);
        assertThat(service.reputations(List.of("Seller01", "Seller02")).get("Seller02").ratingCount())
                .isZero();
        assertThatThrownBy(() -> service.rate("TX-1", "Seller01", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already");
    }

    @Test
    void validatesRatingRange() {
        ReputationService service = new ReputationService();

        assertThatThrownBy(() -> service.rate("TX-1", "Seller01", 6))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
