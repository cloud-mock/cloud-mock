# Changelog

All notable changes to CloudStub are recorded here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). All published modules share a single
lockstep version.

## [Unreleased]

## [0.1.0-beta.1] - 2026-06-13

First publication to Maven Central under the `io.github.cloudstub` namespace. The `-beta` qualifier
signals that the SPI and public API may still change.

### Added

- Published artifacts: `cloudstub-core`, `cloudstub-testing`, `cloudstub-junit`, `cloudstub-sqs`,
  `cloudstub-sdk-v1`, `cloudstub-local`, `cloudstub-codegen`.
- Embedded mock engine (`cloudstub-core`) that redirects AWS SDK v2 traffic and discovers service
  modules via the `CloudStubService` SPI.
- JUnit 5 and 6 extension (`cloudstub-junit`) with lifecycle management and throttle/timeout/brownout
  fault injection, aggregated for test use by `cloudstub-testing`.
- Stateful Amazon SQS module (`cloudstub-sqs`) backed by the shared state store.
- Standalone server (`cloudstub-local`) and the Smithy stub generator (`cloudstub-codegen`),
  distributed as runnable JARs.

[unreleased]: https://github.com/cloudstub/cloudstub/compare/v0.1.0-beta.1...HEAD
[0.1.0-beta.1]: https://github.com/cloudstub/cloudstub/releases/tag/v0.1.0-beta.1
