# ADR-001 — Independent HazRob Tank project

Date: 2026-10-17
Status: Accepted

## Decision
HazRob Tank is an independent tracked robot project in the HazRob GitHub organization. It has its own Android app, firmware, ChemCore fork/implementation, version history, tests and releases.

The existing four-wheel HazRob project, developed in the SERVOC personal account and HazRob organization, remains separate and is a reference implementation only. Reuse is selective and requires provenance, license/dependency review, compatibility tests and documented changes. No upstream repository changes are authorized by this decision.

Repository: https://github.com/HazRob/HazRob-Tank
Initial planned version: 0.1.0.
