package org.gribforyou.client;

import org.gribforyou.client.model.ServiceModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "service-register-client", url = "${service-register.url}")
public interface RegistryClient {

    @GetMapping("/allServices")
    List<ServiceModel> getAllServices();
}
