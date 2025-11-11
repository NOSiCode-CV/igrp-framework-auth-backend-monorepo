package cv.igrp.framework.auth.core.model;


import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Getter
@Builder
public class IGRPUserRepresentation implements UserIdentity{

  private String username;       // Username of the user
  private String email;          // Email of the user
  private List<String> roles;    // List of roles assigned to the user
  private List<String> applications; // List of applications associated with the user
  private List<String> departments; // List of departments associated with the user

  // New fields added to represent external user identifiers and email verification status
  private String externalId;     // External ID of the user (used for linking to external systems)
  private boolean emailVerified; // Indicates whether the user's email is verified

  private String id;
  private String firstName;
  private String lastName;

  @Getter(lombok.AccessLevel.NONE)
  private boolean enable;

  @Override
  public boolean isEnabled() {
    return enable;
  }

}
