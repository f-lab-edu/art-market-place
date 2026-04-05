# art-market-place

This repository has been reset to a single-module Spring Boot monolith rooted at `/Users/administrator/Documents/projects/art-market-place`.

## Current state

- `gateway` is no longer part of the build.
- The root `pom.xml` is now the single application module.
- A new entry point exists at `src/main/java/com/woobeee/artmarketplace/ArtMarketPlaceApplication.java`.
- Legacy module directories remain only as build-output placeholders because the original source tree is not present.

## Next migration step

When source code is restored, move package trees into:

- `src/main/java/com/woobeee/artmarketplace/auth`
- `src/main/java/com/woobeee/artmarketplace/market`
- `src/test/java/com/woobeee/artmarketplace/...`

Then delete the legacy `auth`, `market`, and `gateway` directories.
