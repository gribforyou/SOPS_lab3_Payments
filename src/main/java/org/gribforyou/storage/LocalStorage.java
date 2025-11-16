package org.gribforyou.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class LocalStorage {

    private final Map<UUID, PaymentDetails> payments = new HashMap<>();

    public void addPayment(UUID paymentArtifact, PaymentDetails payment) {
        payments.put(paymentArtifact, payment);
    }

    public PaymentDetails getPayment(UUID paymentArtifact) {
        return payments.get(paymentArtifact);
    }

    public UUID findPaymentByToken(UUID token) {
        return payments.values().stream()
                .filter(payment -> payment.getToken().equals(token))
                .findFirst()
                .map(PaymentDetails::getPaymentDetail)
                .orElse(null);
    }

    @Data
    @AllArgsConstructor
    public static class PaymentDetails {
        private UUID paymentDetail;
        private UUID token;
        private LocalDate paidUntil;
        private Boolean isPaid;
        private String serviceName;
        private List<String> methods;
    }
}
