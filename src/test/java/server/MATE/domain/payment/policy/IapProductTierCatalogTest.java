package server.MATE.domain.payment.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import server.MATE.toss.config.TossIapProperties;

class IapProductTierCatalogTest {

    private final TossIapProperties properties = new TossIapProperties(List.of(
            new TossIapProperties.Tier(30, 200, "sku_30_200", 11000),
            new TossIapProperties.Tier(100, 500, "sku_100_500", 91740)
    ));

    private final IapProductTierCatalog catalog = new IapProductTierCatalog(properties);

    @Test
    void findsMatchingTier() {
        Optional<TossIapProperties.Tier> tier = catalog.find(100, 500);

        assertThat(tier).isPresent();
        assertThat(tier.get().sku()).isEqualTo("sku_100_500");
        assertThat(tier.get().displayAmount()).isEqualTo(91740);
    }

    @Test
    void returnsEmptyWhenCombinationNotConfigured() {
        Optional<TossIapProperties.Tier> tier = catalog.find(13, 520);

        assertThat(tier).isEmpty();
    }

    @Test
    void returnsEmptyWhenTiersListIsEmpty() {
        IapProductTierCatalog emptyCatalog = new IapProductTierCatalog(new TossIapProperties(List.of()));

        assertThat(emptyCatalog.find(30, 200)).isEmpty();
    }
}
