package server.MATE.domain.payment.policy;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.toss.config.TossIapProperties;

@Component
@RequiredArgsConstructor
public class IapProductTierCatalog {

    private final TossIapProperties tossIapProperties;

    public Optional<TossIapProperties.Tier> find(int goalPpl, int reward) {
        return tossIapProperties.tiers().stream()
                .filter(tier -> tier.goalPpl() == goalPpl && tier.reward() == reward)
                .findFirst();
    }
}
