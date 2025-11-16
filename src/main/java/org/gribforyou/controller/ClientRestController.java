package org.gribforyou.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.gribforyou.client.RegistryClient;
import org.gribforyou.client.api.BillApi;
import org.gribforyou.client.api.PayApi;
import org.gribforyou.client.api.PaymentApi;
import org.gribforyou.client.api.TokenApi;
import org.gribforyou.client.model.*;
import org.gribforyou.storage.LocalStorage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClientRestController implements BillApi, PayApi, PaymentApi, TokenApi {

    private final RegistryClient registryClient;
    private final Validator validator;
    private final LocalStorage localStorage;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return BillApi.super.getRequest();
    }

    @Override
    public ResponseEntity<GetBillResponse> billPost(GetBillRequest getBillRequest) {
        try {
            Set<ConstraintViolation<GetBillRequest>> validate = validator.validate(getBillRequest);
            if (!validate.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Optional<ServiceModel> service = registryClient.getAllServices().stream()
                    .filter(serviceModel -> serviceModel.getServiceName().equals(getBillRequest.getServiceName()))
                    .findFirst();

            if (service.isEmpty() || !service.get().getMethods().containsAll(getBillRequest.getMethods())) {
                return ResponseEntity.notFound().build();
            }

            double sum = service.get().getMethods().stream()
                    .filter(method -> getBillRequest.getMethods().contains(method.getMethodName()))
                    .mapToDouble(Method::getPrice)
                    .sum();

            long days = ChronoUnit.DAYS.between(getBillRequest.getDateFrom(), getBillRequest.getDateTo());
            if (days <= 0) {
                return ResponseEntity.badRequest().body(null);
            }

            UUID paymentArtifact = UUID.randomUUID();

            localStorage.addPayment(paymentArtifact, new LocalStorage.PaymentDetails(paymentArtifact, null, getBillRequest.getDateTo(),
                    false, getBillRequest.getServiceName(), getBillRequest.getMethods()));

            GetBillResponse response = new GetBillResponse()
                    .serviceName(getBillRequest.getServiceName())
                    .dateFrom(getBillRequest.getDateFrom())
                    .dateTo(getBillRequest.getDateTo())
                    .totalPrice(BigDecimal.valueOf(sum * days))
                    .paymentArtifact(paymentArtifact.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<PaymentInfoResponse> paymentInfoPost(PaymentInfoRequest paymentInfoRequest) {
        try {
            Set<ConstraintViolation<PaymentInfoRequest>> validate = validator.validate(paymentInfoRequest);
            if (!validate.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            UUID paymentArtifact;
            try {
                paymentArtifact = UUID.fromString(paymentInfoRequest.getPaymentArtifact());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }

            LocalStorage.PaymentDetails payment = localStorage.getPayment(paymentArtifact);
            if (payment == null) {
                return ResponseEntity.notFound().build();
            }

            PaymentInfoResponse response = new PaymentInfoResponse()
                    .paidUntil(payment.getPaidUntil())
                    .availableService(payment.getServiceName())
                    .methods(payment.getMethods());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<TokenResponse> tokenResetPost(TokenRequest tokenRequest) {
        try {
            Set<ConstraintViolation<TokenRequest>> validate = validator.validate(tokenRequest);
            if (!validate.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            UUID paymentArtifact;
            try {
                paymentArtifact = UUID.fromString(tokenRequest.getPaymentArtifact());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }

            LocalStorage.PaymentDetails payment = localStorage.getPayment(paymentArtifact);
            if (payment == null || !payment.getIsPaid()) {
                return ResponseEntity.notFound().build();
            }

            UUID newToken = UUID.randomUUID();

            localStorage.addPayment(paymentArtifact, new LocalStorage.PaymentDetails(
                    payment.getPaymentDetail(),
                    newToken,
                    payment.getPaidUntil(),
                    true,
                    payment.getServiceName(),
                    payment.getMethods()
            ));

            TokenResponse response = new TokenResponse()
                    .token(newToken.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<PayResponse> payPost(PayRequest payRequest) {
        try {
            Set<ConstraintViolation<PayRequest>> validate = validator.validate(payRequest);
            if (!validate.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            UUID paymentArtifact;
            try {
                paymentArtifact = UUID.fromString(payRequest.getPaymentArtifact());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }

            LocalStorage.PaymentDetails payment = localStorage.getPayment(paymentArtifact);
            if (payment == null) {
                return ResponseEntity.notFound().build();
            }

            if (payment.getIsPaid()) {
                return ResponseEntity.ok(new PayResponse()
                        .message("Payment already processed")
                        .token(payment.getToken().toString()));
            }

            UUID token = UUID.randomUUID();

            localStorage.addPayment(paymentArtifact, new LocalStorage.PaymentDetails(paymentArtifact, token, payment.getPaidUntil(),
                    true, payment.getServiceName(), payment.getMethods()));

            PayResponse response = new PayResponse()
                    .message("Payment is succeed")
                    .token(token.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}