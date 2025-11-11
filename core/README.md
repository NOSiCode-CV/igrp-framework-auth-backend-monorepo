## IGRP Platform IAM Core Java
The igrp-platform-iam-core repository provides abstracted interfaces and common data models for fine-grained authorization and core identity management.
This enables the IGRP Platform to integrate seamlessly with various third-party identity providers and authorization systems,
offering a flexible and decoupled security architecture.

## Table of Contents

* [Project Overview](#project-overview)
* [Getting Started](#getting-started)
    * [Installation](#installation)
    * [Prerequisites](#prerequisites)
    * [Building](#building)
* [Authorization Core](#authorization-core)
    * [Usage](#usage)
    * [Documentation](#documentation)
        * [Core Class Overview](#core-class-overview)
        * [Interfaces and Contracts](#interfaces-and-contracts)
* [IAM Core (#igrp-platform-iam-core)](#iam-core-igrp-platform-iam-core)
    * [Building](#building-1)
    * [Usage](#usage-1)
    * [Documentation](#documentation-1)
        * [Data Models (#UserIdentity)](#data-models-useridentity)
* [API Documentation (Javadoc)](#api-documentation-javadoc)
* [Contribution](#contribution)
* [License](#license)

## Project Overview
The igrp-platform-iam-core repository centralizes core Identity and Access Management (IAM) functionalities for the IGRP Platform. 
It establishes a unified, decoupled approach to both fine-grained authorization (Zanzibar-like systems) and foundational identity management (applications, users, roles, departments).

This repository defines clear interfaces and common data models, enabling the integration of various IAM and authorization engines without impacting core application logic. 
It features two primary modules:
+ Authorization Core: Defines contracts for relationship-based authorization, allowing precise permission checks and policy management via external systems.
+ IAM Core: Provides an abstract interface (IAdapter) for managing identity entities, standardizing operations across different IAM providers (e.g., Keycloak, WSO2).

## Getting Started
This section guides you through setting up and building the igrp-platform-iam-core project locally.

### Installation

To get a local copy of the project's source code, clone the repository:
```Bash
git clone https://github.com/your-org/igrp-platform-iam-core.git
cd igrp-platform-iam-core
```

### Prerequisites

Before you can build and use the igrp-platform-iam-core library, ensure you have the following installed:
+ Java Development Kit (JDK) 21 or later: The project is built using Java 21 features.
+ Apache Maven 3.6.3 or later: Maven is used for dependency management and building the project.
Building

The igrp-platform-iam-core project defines the core interfaces, exceptions, and data models for IAM and authorization. It contains no executable logic itself. 
Its purpose is to be compiled into a single JAR artifact that can be included as a dependency in other services or specific adapter implementations.

To build the igrp-platform-iam-core JAR:

```Bash
mvn clean install
```

## Authorization Core
The Authorization Core serves as the foundational interface and abstraction layer for authorization within the IGRP Platform. 
It defines the contract for authorization checks through its interfaces and establishes common data models (e.g., for subjects, resources, and relationships).

### Usage
The Authorization Core defines key interfaces (e.g., AuthorizationCore) and common data models (e.g., PermissionCheckRequest, PermissionCheckResponse)
that consuming applications or authorization adapters must interact with.
The primary purpose is to abstract the authorization logic, allowing your application to request authorization decisions by interacting with these interfaces,
without needing to know the underlying implementation details.
A crucial use case for the Authorization Core is to serve as the contract that authorization adapters implement.
These adapters provide the concrete logic for authorization checks by translating the core's abstract requests into calls to specific authorization engines (like Permify, Permit.io, etc.).

The usage of CompletableFuture enables the calling method to choose how to handle the response, whether synchronously or asynchronously. This approach is crucial for library implementations making external API calls, 
as it ensures non-blocking operations and provides flexibility for future integration patterns.

Here's a conceptual example demonstrating how a Permify Adapter would implement the core's AuthorizationService:
```java
public class PermifyAuthorizationAdapter implements AuthorizationCore {

    private final PermifyClient permifyClient;
    private final String tenantId;

    /**
     * Constructs the PermifyAuthorizationAdapter.
     *
     * @param permifyClient An initialized Permify API client.
     * @param tenantId The Permify tenant ID to use for authorization checks.
     */
    public PermifyAuthorizationAdapter(PermifyClient permifyClient, String tenantId) {
        this.permifyClient = permifyClient;
        this.tenantId = tenantId;
    }

    @Override
    public CompletableFuture<PermissionCheckResponse> check(PermissionCheckRequest checkRequest){
        // Adapter implementation to call Permify's API
        // ...
        return CompletableFuture.completedFuture(new PermissionCheckResponse(PermissionCheckResponse.Decision.PERMIT));
    }

    @Override
    public CompletableFuture<Void> createRelationships(List<RelationshipTuple> relationshipTuples) {
        // Adapter implementation
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> deleteRelationship(RelationshipTuple relationshipTupleToDelete) {
        // Adapter implementation
        return CompletableFuture.completedFuture(null);
    }
}
```


### Documentation
#### Core Class Overview
This module defines the foundational data structures and interfaces for a Zanzibar-style authorization system.

##### Interfaces and Contracts

| Class | Description |
|-------|-------------|
| `AuthorizationCore` | Main interface for permission checks and relationship management. Adapters (e.g., Permify) implement this interface. |
| `PermissionCheckRequest` | Encapsulates the input for a permission check: subject, targetResource, and relation. |
| `PermissionCheckResponse` | Encapsulates the output of a permission check: the decision (`PERMIT` or `DENIED`) and an optional reason. |
| `Decision` | Enum representing possible outcomes of a permission check. |

##### Domain Model Entities

| Class               | Description |
|---------------------|-------------|
| `Entity`            | Abstract base class for `Resource` and `Subject`, containing `type` and `id`. |
| `TargetResource`    | Represents the object being accessed (e.g., a document, an application). |
| `Subject`           | Represents the actor trying to perform the action. May contain a `relation` for subject sets. |
| `RelationshipTuple` | Represents a permission relationship: `targetResource#relation@subject`. Used to create or delete access permissions. |


## API Documentation (Javadoc)
Comprehensive Javadoc for all public interfaces and data models within the igrp-platform-iam-core project can be generated locally. 
This documentation is invaluable for understanding the exact contracts and structures.
To generate the Javadoc:
```bash
mvn javadoc:javadoc
```