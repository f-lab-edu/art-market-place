# AGENTS

This repository uses Codex-oriented documentation and execution plans.

## Purpose

- Define how coding agents should work in this codebase.
- Point agents to product, architecture, security, and plan documents.
- Keep implementation work aligned with active execution plans.

## Primary Reading Order

1. `ARCHITECTURE.md`
2. `docs/PLANS.md`
3. Relevant file in `docs/product-specs/`
4. Relevant file in `docs/design-docs/`
5. Relevant file in `docs/references/`

## Working Rules

- Prefer updating docs when behavior or architecture changes.
- Keep active work tracked under `docs/exec-plans/active/`.
- Move completed plans into `docs/exec-plans/completed/`.
- Record follow-up cleanup in `docs/exec-plans/tech-debt-tracker.md`.
