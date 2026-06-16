# Discuss later

These audit items were intentionally not patched in this pass because they need product decisions.

## SEC-1: Hardcoded ZIP bundle password / offline login model

The current bundle password remains in place. A future design should replace it with an offline-compatible identity and key model, for example:

- local user profile name plus passphrase-derived encryption keys;
- optional per-device Android Keystore protection for local-only data;
- portable export keys wrapped by a user passphrase;
- a migration path for older bundles that still use the legacy password.

Any future username-based protocol should not require network connectivity for normal app use.

## EXP-1: Remote image downloads during export

Export behavior was left unchanged. Recommended future choice: either preserve remote URLs as metadata only, or download remote images through the same centralized remote asset policy used by imports.

## NET-3: Private-network / SSRF-style remote image blocking

Private-network host blocking was skipped. If remote image URLs may come from untrusted imports, add a resolver that rejects loopback, link-local, private IPv4 ranges, and private IPv6 ranges before any fetch.

## DEP-2: Dynamic Material Components version

`material = "1.4.+"` was intentionally left unchanged. Recommended future action: pin it to an exact version or remove the dependency if the app does not use Material Components views.
