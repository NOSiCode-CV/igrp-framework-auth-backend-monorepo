package cv.igrp.framework.auth.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceInfo {
    private String name;
    private String description;
    private List<String> uris;
    private List<String> scopes;
    private String status;
}