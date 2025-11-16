package org.gribforyou.client.model;

import lombok.Data;

@Data
public class Argument {
    private Long id;
    private int argumentNumber;
    private String argumentName;
    private String argumentType;
    private boolean isRequired;
    private Long methodID;
}
