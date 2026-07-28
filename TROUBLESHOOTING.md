# Troubleshooting — `cv.igrp.framework.auth`

## Spring Boot DevTools + `PermissionsRegistry.Permission` — SpEL `EL1004E`

**Symptom.** When running a consumer project with `spring-boot-devtools` on the classpath, `@PreAuthorize("@igrpAuthorization.checkPermission(T(Permission).X)")` throws:

```
org.springframework.expression.spel.SpelEvaluationException: EL1004E: Method call:
Method checkPermission(cv.igrp.framework.auth.generated.PermissionsRegistry$Permission)
cannot be found on type cv.igrp.framework.auth.core.security.IgrpAuthorizationService
```

even though the method exists.

**Cause.** `core-spring-boot` ships a placeholder `cv.igrp.framework.auth.generated.PermissionsRegistry` (no constants) under the same fully qualified name that the annotation processor (`PermissionSourceGenerator`) generates in the consumer project's `target/generated-sources`. In a single-classloader runtime the generated version wins on the classpath and everything works.

DevTools splits the classpath across two classloaders:

- **The base classloader** loads dependency jars — `IgrpAuthorizationService` and the placeholder `Permission` come from `core-spring-boot`.
- **The restart classloader** loads `target/classes` — the generated `Permission` with its constants lives here.

Two `Permission` classes with the same FQN but different classloaders are incompatible types. SpEL sees the argument as a different type than the method parameter and reports `EL1004E`.

**Workaround (consumer side).** Disable the restart classloader before `SpringApplication.run(...)`:

```java
public static void main(String[] args) {
    System.setProperty("spring.devtools.restart.enabled", "false");
    SpringApplication.run(MyApplication.class, args);
}
```

Note: the equivalent `spring.devtools.restart.enabled=false` in `application.properties` is read too late to take effect — set it as a system property in `main`.

**Planned fix (framework side).** Stop shipping the placeholder `Permission` class inside the jar. It exists today so the framework compiles without the consumer's generated sources; the alternative is to move the base type into a separate FQN and have the annotation processor generate against it. Tracked as follow-up.
