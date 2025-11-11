# IGRP Platform Permify Adapter

The **IGRP Platform Permify Adapter** is the implementation of the `AuthorizationCore` interface from the [Authorization Core](https://github.com/your-org/authorization-core) module. 
It leverages the [Permify](https://permify.co) Java SDK to perform fine-grained permission checks and relationship management, 
allowing authorization logic to be externalized from application business code.

## Table of Contents
- [IGRP Platform Permify Adapter](#igrp-platform-permify-adapter)
    - [Table of Contents](#table-of-contents)
    - [Getting Started](#getting-started)
      - [Prerequisites](#prerequisites)
      - [Adding the Dependency](#adding-the-dependency)
      - [Configuration](#configuration)
      - [Usage](#usage)
    - [Error Handling](#error-handling)
    - [Permify Schema Overview](#permify-schema-overview)
    - [Local Development & Contribution](#local-development-&-contribution)
      - [Cloning the Repository](#cloning-the-repository)
      - [Building](#building)
      - [Testing](#testing)
      - [Direct Permify Server API Examples](#direct-permify-server-api-examples)
      - [Running Permify Locally with Docker](#running-permify-locally-with-docker)
    - [Further Resources] 

## Getting Started

### Installation

```bash
git clone http://git.nosi.cv/igrp-3_0/identity-access-management-libraries/igrp-platform-iam-permify-java.git
cd igrp-platform-permify-adapter
```

### Prerequisites
+ Java Development Kit (JDK) 21 or later
+ Maven 3.6.3 or later
+ A running Permify server instance (e.g., at http://localhost:3476)
  + Version: 1.3.9
+ Permify JDK:
  + https://github.com/Permify/permify-java

### Adding the Dependency
To build the project and run in a local development profile:
```bash 
mvn clean install
```
The adapter is packaged as a library (not a standalone application), and is meant to be imported as a dependency in projects requiring integration with Permify.
### Configuration
Configuration
To enable the PermifyAdapter in the application, its required to provide the necessary configuration properties for connecting to the Permify server. 
These properties are typically defined in ```src/main/resources/application.properties``` ```(or application.yml)``` 
and can often be overridden by environment variables.

**1. Application Properties:**
Add the following properties to the application properties file:
```properties
# Permify Server Configuration
permify.server.url=${PERMIFY_SERVER_URL:http://localhost:3476}
permify.api.key=${PERMIFY_API_KEY_URL:} # Optional: When Permify server its configured with authentication API key
```
### Usage

The IGRP Platform Permify Adapter is packaged as a Maven library and is intended to be used as a dependency in your Spring Boot application or any other 
Java project that requires integration with Permify.

#### 1. Adding the Dependency

To include this adapter in your project, add the following to your pom.xml:

```XML
<dependency>
    <groupId>com.your-org.igrp</groupId> 
    <artifactId>igrp-platform-permify-adapter</artifactId>
    <version>1.0.0-SNAPSHOT</version> 
</dependency>
```
#### 2. Integrating with AuthorizationCore

The adapter implements the AuthorizationCore interface, providing a standardized way to interact with a Zanzibar-like system. If you are using Spring Boot, 
the adapter's configuration will typically autoconfigure the AuthorizationCore bean, allowing you to inject it directly into your services or components.

#### Available Methods:
The following methods from the AuthorizationCore interface are implemented, leveraging the Permify SDK:

+ ```CompletableFuture<PermissionCheckResponse> check(PermissionCheckRequest checkRequest)```:
Checks if a subject has a specific permission (relation) on a resource. This represents the core authorization check: "Can a subject perform a relation on a resource?".

+ ```CompletableFuture<Void> createRelationships(List<RelationshipTuple> relationshipTuples)```:
Batch inserts one or more relationships (tuples) into Permify. This is efficient for creating multiple connections at once.

+ ```CompletableFuture<Void> deleteRelationship(RelationshipTuple relationshipTupleToDelete)```:
Deletes a single, specific relationship (tuple) from Permify. The provided relationship tuple will be precisely matched and removed.

## Permify Schema Overview
```
entity user {}

entity application {
    relation creator @user
    relation viewer @user
    relation updater @user
    relation deleter @user

    relation resource_creator @user
    relation resource_viewer @user
    relation resource_updater @user
    relation resource_deleter @user

    permission read = creator or viewer
    permission update = creator or updater
    permission delete = creator
}

entity resource {
    relation application @application
    relation creator @user

    relation resource_item_creator @user
    relation resource_item_viewer @user
    relation resource_item_updater @user
    relation resource_item_deleter @user

    permission create = application.resource_creator or application.creator
    permission read = application.resource_viewer or creator
    permission update = application.resource_updater or creator
    permission delete = application.resource_deleter or creator
}

entity resource_item {
    relation resource @resource
    relation creator @user

    permission create = resource.resource_item_creator or resource.creator
    permission read = resource.resource_item_viewer or creator
    permission update = resource.resource_item_updater or creator
    permission delete = resource.resource_item_deleter or creator
}
```

## Direct Permify Server API Examples
This section provides examples for directly interacting with the Permify server's using tools like Postman. 
This is useful for **testing, debugging, or understanding the underlying Permify API** that this adapter communicates with.

Prerequisites:
+ A running Permify server instance (e.g., at http://localhost:3476).
+ Postman or a similar API client.

General Request Details:
+ Method: All operations typically use POST.
+ Content-Type: application/json

<hr>

### Schema Operations
Permify schemas define your authorization model.

#### Write Schema
Use this to upload or update your authorization schema. Verification happens automatically during this process.
+ Endpoint: POST http://localhost:3476/v1/tenants/default/schemas/write
+ Request Body (JSON):
```JSON
{
  "schema": "entity user {}\n\nentity application {\n    relation creator @user\n    relation viewer @user\n    relation updater @user\n    relation deleter @user\n\n    relation resource_creator @user\n    relation resource_viewer @user\n    relation resource_updater @user\n    relation resource_deleter @user\n\n    permission read = creator or viewer\n    permission update = creator or updater\n    permission delete = creator\n}\n\nentity resource {\n    relation application @application\n    relation creator @user\n\n    relation resource_item_creator @user\n    relation resource_item_viewer @user\n    relation resource_item_updater @user\n    relation resource_item_deleter @user\n\n    permission create = application.resource_creator or application.creator\n    permission read = application.resource_viewer or creator\n    permission update = application.resource_updater or creator\n    permission delete = application.resource_deleter or creator\n}\n\nentity resource_item {\n    relation resource @resource\n    relation creator @user\n\n    permission create = resource.resource_item_creator or resource.creator\n    permission read = resource.resource_item_viewer or creator\n    permission update = resource.resource_item_updater or creator\n    permission delete = resource.resource_item_deleter or creator\n}"
}
```
+ Expected Success Response (JSON):
  + 200 OK 
```JSON
{
    "schema_version": "d0s9ts6ron1c73ejkrc0"
}
```

#### Read Schema
Retrieves the currently active authorization schema.
+ Endpoint: POST http://localhost:3476/v1/tenants/default/schemas/read
+ Request Body (JSON):
```JSON
{
  "metadata": {}
}
```
+ Expected Success Response (JSON):
    + 200 OK
```JSON
{
  "schema": {
    "entity_definitions": {
      "application": {
        "name": "application",
        "relations": {
          "creator": {
            "name": "creator",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "deleter": {
            "name": "deleter",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "resource_creator": {
            "name": "resource_creator",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "resource_deleter": {
            "name": "resource_deleter",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "resource_updater": {
            "name": "resource_updater",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "resource_viewer": {
            "name": "resource_viewer",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "updater": {
            "name": "updater",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "viewer": {
            "name": "viewer",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          }
        },
        "permissions": {
          "delete": {
            "name": "delete",
            "child": {
              "leaf": {
                "computed_user_set": {
                  "relation": "creator"
                }
              }
            }
          },
          "read": {
            "name": "read",
            "child": {
              "rewrite": {
                "rewrite_operation": "OPERATION_UNION",
                "children": [
                  {
                    "leaf": {
                      "computed_user_set": {
                        "relation": "creator"
                      }
                    }
                  },
                  {
                    "leaf": {
                      "computed_user_set": {
                        "relation": "viewer"
                      }
                    }
                  }
                ]
              }
            }
          },
          "update": {
            "name": "update",
            "child": {
              "rewrite": {
                "rewrite_operation": "OPERATION_UNION",
                "children": [
                  {
                    "leaf": {
                      "computed_user_set": {
                        "relation": "creator"
                      }
                    }
                  },
                  {
                    "leaf": {
                      "computed_user_set": {
                        "relation": "updater"
                      }
                    }
                  }
                ]
              }
            }
          }
        },
        "attributes": {},
        "references": {
          "creator": "REFERENCE_RELATION",
          "delete": "REFERENCE_PERMISSION",
          "deleter": "REFERENCE_RELATION",
          "read": "REFERENCE_PERMISSION",
          "resource_creator": "REFERENCE_RELATION",
          "resource_deleter": "REFERENCE_RELATION",
          "resource_updater": "REFERENCE_RELATION",
          "resource_viewer": "REFERENCE_RELATION",
          "update": "REFERENCE_PERMISSION",
          "updater": "REFERENCE_RELATION",
          "viewer": "REFERENCE_RELATION"
        }
      },
      "resource": {
        "name": "resource",
        "relations": {
          "application": {
            "name": "application",
            "relation_references": [
              {
                "type": "application",
                "relation": ""
              }
            ]
          },
          "creator": {
            "name": "creator",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "resource_item_creator": {
            "name": "resource_item_creator",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "resource_item_deleter": {
            "name": "resource_item_deleter",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "resource_item_updater": {
            "name": "resource_item_updater",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "resource_item_viewer": {
            "name": "resource_item_viewer",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          }
        },
        "permissions": {
          "create": {
            "name": "create",
            "child": {
              "rewrite": {
                "rewrite_operation": "OPERATION_UNION",
                "children": [
                  {
                    "leaf": {
                      "tuple_to_user_set": {
                        "tupleSet": {
                          "relation": "application"
                        },
                        "computed": {
                          "relation": "resource_creator"
                        }
                      }
                    }
                  },
                  {
                    "leaf": {
                      "tuple_to_user_set": {
                        "tupleSet": {
                          "relation": "application"
                        },
                        "computed": {
                          "relation": "creator"
                        }
                      }
                    }
                  }
                ]
              }
            }
          },
          "delete": {
            "name": "delete",
            "child": {
              "rewrite": {
                "rewrite_operation": "OPERATION_UNION",
                "children": [
                  {
                    "leaf": {
                      "tuple_to_user_set": {
                        "tupleSet": {
                          "relation": "application"
                        },
                        "computed": {
                          "relation": "resource_deleter"
                        }
                      }
                    }
                  },
                  {
                    "leaf": {
                      "computed_user_set": {
                        "relation": "creator"
                      }
                    }
                  }
                ]
              }
            }
          },
          "read": {
            "name": "read",
            "child": {
              "rewrite": {
                "rewrite_operation": "OPERATION_UNION",
                "children": [
                  {
                    "leaf": {
                      "tuple_to_user_set": {
                        "tupleSet": {
                          "relation": "application"
                        },
                        "computed": {
                          "relation": "resource_viewer"
                        }
                      }
                    }
                  },
                  {
                    "leaf": {
                      "computed_user_set": {
                        "relation": "creator"
                      }
                    }
                  }
                ]
              }
            }
          },
          "update": {
            "name": "update",
            "child": {
              "rewrite": {
                "rewrite_operation": "OPERATION_UNION",
                "children": [
                  {
                    "leaf": {
                      "tuple_to_user_set": {
                        "tupleSet": {
                          "relation": "application"
                        },
                        "computed": {
                          "relation": "resource_updater"
                        }
                      }
                    }
                  },
                  {
                    "leaf": {
                      "computed_user_set": {
                        "relation": "creator"
                      }
                    }
                  }
                ]
              }
            }
          }
        },
        "attributes": {},
        "references": {
          "application": "REFERENCE_RELATION",
          "create": "REFERENCE_PERMISSION",
          "creator": "REFERENCE_RELATION",
          "delete": "REFERENCE_PERMISSION",
          "read": "REFERENCE_PERMISSION",
          "resource_item_creator": "REFERENCE_RELATION",
          "resource_item_deleter": "REFERENCE_RELATION",
          "resource_item_updater": "REFERENCE_RELATION",
          "resource_item_viewer": "REFERENCE_RELATION",
          "update": "REFERENCE_PERMISSION"
        }
      },
      "resource_item": {
        "name": "resource_item",
        "relations": {
          "creator": {
            "name": "creator",
            "relation_references": [
              {
                "type": "user",
                "relation": ""
              }
            ]
          },
          "resource": {
            "name": "resource",
            "relation_references": [
              {
                "type": "resource",
                "relation": ""
              }
            ]
          }
        },
        "permissions": {
          "create": {
            "name": "create",
            "child": {
              "rewrite": {
                "rewrite_operation": "OPERATION_UNION",
                "children": [
                  {
                    "leaf": {
                      "tuple_to_user_set": {
                        "tupleSet": {
                          "relation": "resource"
                        },
                        "computed": {
                          "relation": "resource_item_creator"
                        }
                      }
                    }
                  },
                  {
                    "leaf": {
                      "tuple_to_user_set": {
                        "tupleSet": {
                          "relation": "resource"
                        },
                        "computed": {
                          "relation": "creator"
                        }
                      }
                    }
                  }
                ]
              }
            }
          },
          "delete": {
            "name": "delete",
            "child": {
              "rewrite": {
                "rewrite_operation": "OPERATION_UNION",
                "children": [
                  {
                    "leaf": {
                      "tuple_to_user_set": {
                        "tupleSet": {
                          "relation": "resource"
                        },
                        "computed": {
                          "relation": "resource_item_deleter"
                        }
                      }
                    }
                  },
                  {
                    "leaf": {
                      "computed_user_set": {
                        "relation": "creator"
                      }
                    }
                  }
                ]
              }
            }
          },
          "read": {
            "name": "read",
            "child": {
              "rewrite": {
                "rewrite_operation": "OPERATION_UNION",
                "children": [
                  {
                    "leaf": {
                      "tuple_to_user_set": {
                        "tupleSet": {
                          "relation": "resource"
                        },
                        "computed": {
                          "relation": "resource_item_viewer"
                        }
                      }
                    }
                  },
                  {
                    "leaf": {
                      "computed_user_set": {
                        "relation": "creator"
                      }
                    }
                  }
                ]
              }
            }
          },
          "update": {
            "name": "update",
            "child": {
              "rewrite": {
                "rewrite_operation": "OPERATION_UNION",
                "children": [
                  {
                    "leaf": {
                      "tuple_to_user_set": {
                        "tupleSet": {
                          "relation": "resource"
                        },
                        "computed": {
                          "relation": "resource_item_updater"
                        }
                      }
                    }
                  },
                  {
                    "leaf": {
                      "computed_user_set": {
                        "relation": "creator"
                      }
                    }
                  }
                ]
              }
            }
          }
        },
        "attributes": {},
        "references": {
          "create": "REFERENCE_PERMISSION",
          "creator": "REFERENCE_RELATION",
          "delete": "REFERENCE_PERMISSION",
          "read": "REFERENCE_PERMISSION",
          "resource": "REFERENCE_RELATION",
          "update": "REFERENCE_PERMISSION"
        }
      },
      "user": {
        "name": "user",
        "relations": {},
        "permissions": {},
        "attributes": {},
        "references": {}
      }
    },
    "rule_definitions": {},
    "references": {
      "application": "REFERENCE_ENTITY",
      "resource": "REFERENCE_ENTITY",
      "resource_item": "REFERENCE_ENTITY",
      "user": "REFERENCE_ENTITY"
    }
  }
}
```

### Data Operations
Manage the relationship tuples that define the authorization data.

#### Write Data (Create Relationships)
Creates one or more relationship tuples.
+ Endpoint: POST http://localhost:3476/v1/tenants/default/data/write
+ Request Body (JSON):
```JSON
{
  "metadata": {
    "schema_version": ""
  },
  "tuples": [
    {
      "entity": {
        "type": "application",
        "id": "app1"
      },
      "relation": "creator",
      "subject": {
        "type": "user",
        "id": "bos",
        "relation": ""
      }
    }
  ]
}

```
+ Expected Success Response (JSON):
    + 200 OK
```JSON
{
  "snap_token": "7SdJN4F1Rhg="
}
```

#### Read Data (Lookup Relationships)
Retrieves relationship tuples matching a given filter.
+ Endpoint: POST http://localhost:3476/v1/tenants/default/data/relationships/read
+ Request Body (JSON):
```JSON
{
  "metadata": {
    "schema_version": ""
  },
  "filter": {
    "tuple_filter": {
      "entity": {
        "type": "application",
        "ids": ["app1"]
      },
      "relation": "creator",
      "subject": {
        "type": "user",
        "ids": ["bos"],
        "relation": ""
      }
    }
  }
}

```
+ Expected Success Response (JSON):
    + 200 OK
```JSON
{
  "tuples": [
    {
      "entity": {
        "type": "application",
        "id": "app1"
      },
      "relation": "creator",
      "subject": {
        "type": "user",
        "id": "bos",
        "relation": ""
      }
    }
  ],
  "continuous_token": ""
}
```

#### Delete Data (Delete Relationships)
Deletes relationship tuples that match a filter.
+ Endpoint: POST http://localhost:3476/v1/tenants/default/data/delete
+ Request Body (JSON):
```JSON
{
  "tuple_filter": {
    "entity": {
      "type": "application",
      "ids": ["app1"]
    },
    "relation": "administrator",
    "subject": {
      "type": "user",
      "ids": ["3"]
    }
  },
  "attribute_filter": {}
}
```
+ Expected Success Response (JSON):
    + 200 OK
```JSON
{
    "snap_token": "EwMAAAAAAAA="
}
```

### Permission Operations
Verify authorization decisions based on defined schema and data.

#### Check Permission
Checks if a subject can read a specific permission on a resource.
+ Endpoint: POST http://localhost:3476/v1/tenants/default/permissions/check
+ Request Body (JSON):
```JSON
{
  "metadata": {
    "schema_version": "",
    "depth": 20
  },
  "entity": {
    "type": "application",
    "id": "app1"
  },
  "permission": "read",
  "subject": {
    "type": "user",
    "id": "bos",
    "relation": ""
  }
}

```
+ Expected Success Response (JSON):
```JSON
{
  "can": "CHECK_RESULT_ALLOWED",
  "metadata": {
    "check_count": 3
  }
}
```

### Running Permify Locally with Docker
For local development and testing, you can quickly spin up a Permify server instance using Docker Compose. This ensures you have a consistent and isolated environment to test the adapter's functionality.

**Prerequisites**:
+ **Docker Desktop**: Ensure you have Docker Desktop (which includes Docker Compose) installed and running on your machine.

**Setup**:
The ```docker-compose.yaml``` file, which defines the Permify service, is located in the root of this project. It is pre-configured to use the official permify/permify:v1.3.9 Docker image and to map its default API port (3476) to your host. 
It also sets up a persistent volume for your schema and relationship data.

**Usage:**
1. **Start Permify:**
Open your terminal or command prompt, navigate to the root directory of this project (where docker-compose.yaml is located), and execute the following command:
```Bash
docker compose up -d
```
This command pulls the Permify Docker image (if not already present) and starts the Permify server in the background (-d for detached mode).

2. **Verify Permify is Running:**
To check if the container is running:
```Bash
docker ps
```
You should see a container named ```permify-server``` listed as ```Up```.
To confirm the Permify API is responsive, you can access its health endpoint in your browser or with curl:
```Bash
curl http://localhost:3476/healthz
```

You should receive a successful response 
```JSON
{"status":"SERVING"}
```

3. **Stop Permify:**
When you are finished with your development session, you can stop and remove the Permify container and associated resources using:
```Bash
doker compose down
```
This command will stop the container and remove the network defined in docker-compose.yaml. The persistent permify_data volume will retain your schema and relationship data for future sessions.

