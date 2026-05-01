# Architecture

High-level system architecture for `art-market-place`.

## Current Modules

- `auth`: signup, login, token issuance, token refresh
- `product`: product creation and image upload flows
- `blog`: post, category, comment, and like features
- `_common`: shared request/filter infrastructure
- `front`: frontend application

## Documentation Map

- Product details: `docs/product-specs/`
- Design thinking: `docs/design-docs/`
- Active execution plans: `docs/exec-plans/active/`
- Generated reference artifacts: `docs/generated/`
- Engineering policies: files under `docs/`

## Notes

- Keep this file focused on system boundaries and major data flows.
- Put task-specific implementation detail into execution plans.
