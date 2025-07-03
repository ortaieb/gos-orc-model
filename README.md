# Shared-Model

This library responsible for the protocol between different pods and calls between the platform component and the business-logic processing unit.

At its core a ProtoBuf config files located under `src/main/proto` and it will create a combined interface to beused for both use cases.

## Building and maintaining

### (Optional) create Maven wrapper:

```bash
$ mvn wrapper:wrapper
```

### Build

For local builds you can use the following.
```bash
$ ./mvnw clean install
```

Later we will create a proper deploy to promote the build to a GA artifact registry.

## Make sure before making changes (!!!)
- Currently version management is manual. Update minor version when adding fields or adding new structs/services.
- Protobuf should provide backward compatible interface. Do not change ordinals of existing (used or deprecated) fields.
