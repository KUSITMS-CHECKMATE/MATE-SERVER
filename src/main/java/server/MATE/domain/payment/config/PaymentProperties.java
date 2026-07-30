package server.MATE.domain.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(BankMaintenance bankMaintenance) {

    public PaymentProperties {
        if (bankMaintenance == null) {
            bankMaintenance = new BankMaintenance(false, null, null);
        }
    }

    public record BankMaintenance(boolean enabled, LocalTime start, LocalTime end) {

        public boolean isUnderMaintenance(LocalTime now) {
            if (!enabled || start == null || end == null) return false;
            if (start.equals(end)) return false;
            if (start.isBefore(end)) {
                return !now.isBefore(start) && now.isBefore(end);
            }
            // crosses midnight
            return !now.isBefore(start) || now.isBefore(end);
        }
    }
}
