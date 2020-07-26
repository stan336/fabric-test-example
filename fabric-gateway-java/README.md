# Hyperledger Fabric Gateway SDK for Java

The Fabric Gateway SDK allows applications to interact with a Fabric blockchain network.  It provides a simple API to submit transactions to a ledger or query the contents of a ledger with minimal code.

The Gateway SDK implements the Fabric programming model as described in the [Developing Applications](https://hyperledger-fabric.readthedocs.io/en/latest/developapps/developing_applications.html) chapter of the Fabric documentation.

## How to use 
see test

### API documentation
- [2.2](https://hyperledger.github.io/fabric-gateway-java/release-2.2/)

### Maven

Add the following dependency to your project's `pom.xml` file:

```xml
<dependency>
  <groupId>org.hyperledger.fabric</groupId>
  <artifactId>fabric-gateway-java</artifactId>
  <version>2.2.0</version>
</dependency>
```

### Unit tests

All classes and methods have a high coverage (~90%) of unit tests. These are written using the [JUnit](https://junit.org/junit5/),
[AssertJ](https://joel-costigliola.github.io/assertj/) and [Mockito](https://site.mockito.org/) frameworks.

### Scenario tests

Scenario tests are written using the [Cucumber](https://cucumber.io/) BDD framework.
