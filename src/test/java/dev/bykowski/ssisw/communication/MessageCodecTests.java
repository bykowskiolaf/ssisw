package dev.bykowski.ssisw.communication;

import dev.bykowski.ssisw.market.SearchCriteria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCodecTests {

    @Test
    void roundTripsAclJsonPayload() {
        MessageCodec codec = new MessageCodec();
        MarketplaceProtocol.SearchRequest request = new MarketplaceProtocol.SearchRequest(
                new SearchCriteria("SUV", "PETROL", 2020, 200_000));

        String json = codec.encode(request);
        MarketplaceProtocol.SearchRequest decoded = codec.decode(
                json, MarketplaceProtocol.SearchRequest.class);

        assertThat(decoded).isEqualTo(request);
        assertThat(json).contains("maximumTotalCostPln");
    }
}
