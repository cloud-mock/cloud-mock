# Design the state store interface

**Type:** design

## Summary

CloudMock is currently stateless — every response is a template. A module receives an AWS SDK call and returns a
well-formed response, but nothing is remembered between calls. The state store is what changes that. It is a shared,
core-managed key-value backend that modules read from and write to so that one call can affect the response to a
later call.

When a user sends a message to SQS, the module writes it to the store. When the user receives a message, the module
reads from the store and returns what was actually sent. When the user deletes a message, the module removes it from
the store. The module is the bridge between the AWS SDK protocol and the store — it knows how to translate AWS API
calls into store reads and writes. The store has no AWS knowledge at all.

Because the store is shared and owned by the core, the admin REST API, the CLI, and the management console can all
read from it directly. Whatever a user's application just sent to SQS is immediately visible in the console without
any extra wiring. A single `reset()` clears everything across all services.

This issue is a design review only. The goal is to define the store interface and confirm it can be injected into
the existing SPI without breaking changes. No implementation is required.

## Data model

The store is a key-value backend scoped by service ID. Each module writes its data under its own service prefix:

- `sqs/queues/my-queue/messages/{id}`
- `s3/buckets/my-bucket/objects/{key}`
- `secrets/my-secret`

If a module is not loaded, there are simply no entries under its service prefix. The store has no concept of which
services exist — data appears when a module writes it and disappears when reset is called.

## Acceptance criteria

- [ ] The store's role as the live data backend for modules is clearly documented
- [ ] The `StateStore` interface is sketched covering at minimum: `put`, `get`, `list`, and `clear` scoped by
  service ID
- [ ] The store injection point is designed into the SPI in whatever way is cleanest — breaking changes are acceptable since CloudMock is not yet published
- [ ] The review confirms the store interface has no WireMock types on either side — it must be usable by module
  code, the admin REST API, and the console equally
- [ ] The key naming convention is proposed and documented
- [ ] Any SPI adjustments needed before the interface is too widely adopted to change are flagged with a
  recommended action
- [ ] No implementation code is merged as part of this issue

## Dependencies

- 0002 (SPI contract — any adjustments must be evaluated against the frozen interface)
- 0003 (core engine — state store lifecycle would be managed here)

## Notes

- The store is owned and lifecycle-managed by the core. Modules do not create or destroy it.
- The store is injected into each module at registration time alongside the StubRegistrar.
- Modules scope all their data by service ID. The store does not enforce this — it is a convention that
  module authors follow.
- The admin REST API, CLI, and console are all read consumers of the same store. They never write directly —
  all writes go through module code triggered by AWS SDK calls.
- Consider whether the store should be an in-memory map (fast, simple, lost on restart) or pluggable with a
  persistence backend for longer-running local development sessions.
