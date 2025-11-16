package org.gribforyou.client.model;

import lombok.Data;

import java.util.List;

@Data
public class Method {
    private Long id;
    private String methodName;
    private double price;
    private boolean isPrivate;
    private Long serviceModelID;
    private List<Argument> arguments;
}
