---
layout: page
title: Andre Liu's Project Portfolio Page
---

### Project: AddressBook Level 4

AddressBook - Level 4 is a desktop address book application used for teaching Software Engineering principles. The user interacts with it using a CLI, and it has a GUI created with JavaFX. It is written in Java, and has about 10 kLoC.

Given below are my contributions to the project.

* **New Feature**: Added the `view` command that shows the full details of one contact ([\#46](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/46)).
  * What it does: `view INDEX` prints every field of the chosen contact (name, phone, email, address, tags and note) in the result box, and highlights that contact's row in the list.
  * Justification: the contact cards are compact, so a user needs one place to read everything about a person. Highlighting the row shows which card the result belongs to, even after scrolling.
  * Highlights: a list index larger than the integer range is reported as out of range, not as an invalid format ([\#132](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/132)). The highlight is carried to the UI by `CommandResult` instead of the UI inspecting the command text. It is cleared by the next successful command so that a stale index never marks the wrong person (issue [\#149](https://github.com/AY2627S1-CS2103T-W11-3/tp/issues/149)).

* **New Feature**: Added the `filter` command that lists contacts by one or more tags ([\#83](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/83)).
  * What it does: `filter t/TAG [t/TAG]...` shows only contacts that have the given tags.
  * Justification: with many contacts, users want to narrow the list to one context (for example a hackathon team) without remembering names.
  * Highlights: the active filter is shown beside the list so it stays visible after other commands ([\#130](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/130)). Editing a contact keeps the filter ([\#128](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/128)). A filter with no matches and a filter with unexpected arguments give clear, consistent messages ([\#129](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/129), [\#127](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/127)).

* **New Feature**: Added Tab autocomplete for command words ([\#88](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/88)).
  * What it does: pressing Tab while typing the command word completes it. Pressing Tab again cycles through commands that share the prefix.
  * Justification: it speeds up typing for users who prefer the keyboard, and it helps new users discover command names.

* **Code contributed**: [RepoSense link](https://nus-cs2103-ay2627-s1.github.io/tp-dashboard/?search=andreliu1225&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other&since=2026-09-18T00%3A00%3A00&filteredFileName=)

* **Enhancements to existing features**:
  * Fixed the filter, edit and view behaviours listed above, each with tests ([\#127](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/127), [\#128](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/128), [\#129](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/129), [\#130](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/130), [\#132](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/132)).
  * Added tests for `view`, `filter`, `CommandResult` and the list highlight (`HighlightUiTest`).

* **Documentation**:
  * User Guide:
    * Added the sections for `view`, `filter` and Tab autocomplete ([\#56](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/56)).
  * Developer Guide:
    * Updated the user stories, use cases and glossary ([\#68](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/68)).
    * Added the sequence diagram and implementation notes for `view` ([\#91](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/91)).
    * Added a UI mockup of the intended final product ([\#30](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/30)).
  * Team: updated my profile in `AboutUs.md` ([\#13](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/13)).

* **Community**:
  * PRs reviewed for teammate Abhijit, for example: [\#48](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/48), [\#61](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/61), [\#121](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/121), [\#122](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/122), [\#123](https://github.com/AY2627S1-CS2103T-W11-3/tp/pull/123)
  * Reported bugs and enhancement suggestions found during alpha testing for `undo` and `favourite`, for example: [\#102](https://github.com/AY2627S1-CS2103T-W11-3/tp/issues/102), [\#103](https://github.com/AY2627S1-CS2103T-W11-3/tp/issues/103), [\#104](https://github.com/AY2627S1-CS2103T-W11-3/tp/issues/104), [\#105](https://github.com/AY2627S1-CS2103T-W11-3/tp/issues/105), [\#106](https://github.com/AY2627S1-CS2103T-W11-3/tp/issues/106)
