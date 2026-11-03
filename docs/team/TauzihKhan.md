---
layout: page
title: Tauzih Khan's Project Portfolio Page
---

### Project: AB4

AB4 is an offline contact manager for NUS students. Users manage contacts through typed commands in a JavaFX GUI. It is written in Java and builds on AddressBook-Level3.

Given below are my contributions to the project.

* **New Feature**: Enhanced contact deletion ([#47](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/47)).
  * What it does: deletes a contact using its index in the displayed list.
  * Justification: helps users remove unwanted contacts without losing their search results.
  * Highlights: validates indices and preserves contact data when saving fails.

* **New Feature**: Added undo ([#79](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/79), [#126](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/126)).
  * What it does: reverses the latest five changes, with history retained across restarts.
  * Justification: lets users recover from accidental changes.
  * Highlights: supports contacts, favourites, and groups, with shared history and recovery after interrupted saves.

* **New Feature**: Added favourites ([#80](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/80), [#126](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/126)).
  * What it does: lets users favourite contacts and switch between Contacts and Favourites.
  * Justification: provides convenient access to important contacts.
  * Highlights: keeps linked contact details synchronised and preserves each section's search and ordering.

* **Code contributed**: [Commits](https://github.com/AY2627S1-CS2103T-W11-3/tp/commits?author=TauzihKhan).

* **Enhancements to existing features**:
  * Added a resizable contact details panel alongside command feedback ([#145](https://github.com/AY2627S1-CS2103T-W11-3/tp/issues/145)).
  * Added regression tests for deletion, undo, linked favourites, and save failures.

* **Documentation**:
  * Updated the User Guide and Developer Guide for delete, undo, and favourites.
  * Updated UML diagrams for these features ([#107](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/107)).

* **Community**:
  * Reported bugs involving autocomplete, group membership, and inconsistent messages ([#97](https://github.com/AY2627S1-CS2103T-W11-3/tp/issues/97), [#98](https://github.com/AY2627S1-CS2103T-W11-3/tp/issues/98), [#100](https://github.com/AY2627S1-CS2103T-W11-3/tp/issues/100)).
