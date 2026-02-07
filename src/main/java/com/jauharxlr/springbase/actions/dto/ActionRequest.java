package com.jauharxlr.springbase.actions.dto;

import lombok.Data;
import java.util.List;

@Data
public class ActionRequest {
    private List<ActionOperation> operations;
}
