package org.gribforyou.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.gribforyou.client.RegistryClient;
import org.gribforyou.client.model.Method;
import org.gribforyou.client.model.ServiceModel;
import org.gribforyou.server.api.TokenApi;
import org.gribforyou.server.model.TokenCheckRequest;
import org.gribforyou.server.model.TokenCheckResponse;
import org.gribforyou.storage.LocalStorage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ServiceRestController implements TokenApi {

    private final Validator validator;
    private final LocalStorage localStorage;
    private final RegistryClient registryClient;

    @Override
    public ResponseEntity<TokenCheckResponse> tokenCheckPost(TokenCheckRequest tokenCheckRequest) {
        try {
            Set<ConstraintViolation<TokenCheckRequest>> validate = validator.validate(tokenCheckRequest);
            if (!validate.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            UUID token;
            try {
                token = UUID.fromString(tokenCheckRequest.getToken());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(401).build();
            }

            UUID paymentArtifact = localStorage.findPaymentByToken(token);
            if (paymentArtifact == null) {
                return ResponseEntity.status(401).build();
            }

            LocalStorage.PaymentDetails payment = localStorage.getPayment(paymentArtifact);
            if (payment == null || !payment.getIsPaid() || payment.getPaidUntil().isBefore(LocalDate.now())) {
                return ResponseEntity.status(401).build();
            }

            String serviceName = tokenCheckRequest.getServiceName();
            String methodName = tokenCheckRequest.getMethodName();

            if (!serviceName.equals(payment.getServiceName())) {
                return ResponseEntity.status(403).build();
            }

            if (!payment.getMethods().contains(methodName)) {
                return ResponseEntity.status(403).build();
            }

            Optional<ServiceModel> serviceModel = registryClient.getAllServices().stream()
                    .filter(s -> s.getServiceName().equals(serviceName))
                    .findFirst();

            if (serviceModel.isEmpty()) {
                return ResponseEntity.status(403).build();
            }

            boolean methodExists = serviceModel.get().getMethods().stream()
                    .anyMatch(m -> m.getMethodName().equals(methodName));

            if (!methodExists) {
                return ResponseEntity.status(403).build();
            }

            TokenCheckResponse response = new TokenCheckResponse()
                    .allowed(true)
                    .serviceName(serviceName)
                    .methodName(methodName)
                    .paidUntil(payment.getPaidUntil())
                    .message("Access granted");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}