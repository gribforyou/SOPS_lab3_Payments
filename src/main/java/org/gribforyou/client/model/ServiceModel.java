package org.gribforyou.client.model;

import lombok.Data;

import java.util.List;

@Data
public class ServiceModel {
    private Long id;
    private String serviceName;
    private List<Method> methods;
}
