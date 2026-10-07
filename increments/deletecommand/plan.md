# F4: Delete a Contact — Checkpoint Plan

## Status and workflow

Checkpoint 1 approved by the user. Checkpoint 2 implemented and awaiting review; checkpoints 3–5 are pending. Implement one checkpoint at a time, provide validation results and a drafted commit message, then stop for user review. The user creates commits. Start each subsequent checkpoint only after an explicit green light. Record review feedback and completion here.

## Contract

`delete INDEX` permanently removes one contact selected by its position in the currently displayed list, including filtered results. Only lowercase `delete` is supported. Accept ASCII digits matching `[1-9][0-9]*`, with surrounding spaces/tabs and spaces/tabs separating keyword and argument. Reject multiple arguments and whitespace inside an index.

Validate in order: argument count → index format → empty displayed list → index range. Arbitrarily large valid integers must reach list validation without overflow.

| Condition | Exact response |
| --- | --- |
| Missing argument | `Missing contact index. Usage: delete INDEX` |
| Multiple arguments | `Expected exactly one contact index. Usage: delete INDEX` |
| Invalid format | `Invalid contact index. Enter a positive whole number without leading zeros. Usage: delete INDEX` |
| Empty displayed list | `No contacts are displayed. Nothing to delete.` |
| Out of range | `Contact index out of range. Choose an index from 1 to N.` |
| Save failure | `Could not save changes. Contact was not deleted. Please try again.` |
| Success | `Deleted contact: NAME (index INDEX).` |

Substitute the displayed count for N, the stored name for NAME, and the pre-deletion displayed position for INDEX.

Success preserves the active filter, remaining order, all other contacts and their tags/notes, renumbers displayed entries, clears input, and persists across restart. Deleting the last displayed contact shows `No contacts to display.` in the list area. Clear details only when they belong to the deleted contact. Repeated commands use the updated list. Failures preserve memory, display, details, saved data, and entered input, and never report success. No undo, confirmation, bulk deletion, or name-based deletion.

## Current implementation and dependencies

* `DeleteCommandParser` delegates to shared integer parsing and uses a generic error. Keep F4-specific validation from changing other commands' contracts.
* `DeleteCommand` already selects from the filtered list and removes from the full model, but its messages differ.
* `LogicManager` mutates the live model before saving. JSON storage writes to the destination directly. Memory rollback alone cannot satisfy the saved-data guarantee.
* `CommandBox` already clears on success and retains input on exceptions. `PersonListPanel` numbers cards using list positions but has no specified empty-state label.
* `Person` has contact fields and tags, but no notes field. The UI has no separate contact-details panel. These are explicit integration dependencies: resolve ownership before the UI checkpoint; do not silently mark their acceptance criteria complete or invent shared feature APIs.
* Shared creation rules currently constrain duplicates. Preserve those rules. Use displayed-record selection, never name-based deletion; reassess exact duplicate identity if shared creation later permits it.

## Checkpoint 1: Argument validation and safe index representation

Status: approved by the user. Dependencies: none.

Implement exact count/format errors, spaces/tabs handling, and lowercase routing. Carry a valid oversized index to command execution without narrowing overflow; retain compatibility with existing command callers where practical. Empty-list validation must precede range validation even for oversized inputs.

Validation: missing input; multiple valid/invalid tokens; names; zero; negatives; plus signs; leading zeros; decimals; scientific notation; non-ASCII digits; internal spaces/tabs; valid surrounding spacing; integer boundaries and extremely long integers. Check other parser behavior for regressions.

Draft subject: `Validate delete command arguments`

## Checkpoint 2: Displayed-list deletion semantics

Status: implemented, awaiting user review. Depends on checkpoint 1.

Empty-list and dynamic range errors were completed in checkpoint 1 to validate oversized inputs end to end. Implement exact success output. Delete only the selected displayed record from the full collection. Preserve the filter, remaining order, and unrelated data. Keep validation failures mutation-free.

Validation: first/middle/last selections, filtered results, contacts outside results, shared tags, same-name records where creation rules permit them, repeated commands, empty-list precedence, oversized indices, and pre-deletion index/name in output. Save-failure guarantees remain pending checkpoint 3.

Draft subject: `Align deletion with displayed contact indices`

## Checkpoint 3: Preserve state when saving fails

Status: pending. Depends on checkpoint 2.

Design deletion as a staged change: save the candidate collection safely before publishing changes to observable live state. Keep orchestration in logic and file operations in storage. Replace saved data using a temporary file and atomic replacement where supported; fail safely if replacement cannot preserve the original. Avoid a second fallible save after publishing deletion. Review shared storage callers for regressions.

Validation: injected write/replacement failures preserve original file bytes, model contents, filter, order, and observable UI state; absent destination remains absent on failure; exact failure text and no success result. Successful deletion reloads correctly. Account for temporary-file cleanup and filesystem support.

Draft subject: `Preserve contacts when deletion cannot be saved`

## Checkpoint 4: UI behavior and feature integration

Status: pending. Depends on checkpoint 3 and resolution of notes/details-panel ownership.

Add the exact empty-list placeholder. Verify consecutive numbering, input clearing/retention, and result feedback. Integrate with the shared details panel when available: clear deleted-contact details and preserve other details, including on every failure. Verify removal of notes through the shared contact representation when available. Record unresolved dependencies explicitly if those features are still absent.

Validation: delete the last filtered result while other stored contacts remain; delete selected versus other displayed contact; preserve details on failures; verify notes and shared tags; repeat deletion after renumbering. Use focused UI checks and manual scenarios as appropriate.

Draft subject: `Complete delete command UI feedback`

## Checkpoint 5: End-to-end acceptance and documentation

Status: pending. Depends on checkpoints 1–4, including their integration dependencies.

Update user/developer documentation and manual testing instructions to the implemented F4 contract. Exercise the complete command-to-storage flow and check validation precedence across empty and populated filtered lists. Cover restart persistence and save failures. Run relevant tests, Checkstyle, and repository checks; record actual results and outstanding limitations.

Draft subject: `Document and verify contact deletion`

## Review log

* Initial plan: inspected parser, command, model fields, logic save order, JSON storage, and UI. No implementation or tests run yet.

* Checkpoint 1: implemented delete-specific count/format validation and a positive one-based `BigInteger` command index, retaining the existing `Index` constructor. Empty-list and dynamic range errors moved forward from checkpoint 2 to preserve validation order for oversized inputs. Shared index parsing and command routing remain unchanged.
* Validation passed: `./gradlew test --tests '*logic.parser.*' --tests '*DeleteCommandTest' --tests '*LogicManagerTest' checkstyleMain checkstyleTest`; `git diff --check`. Tests include 1,000-digit indices, ASCII-only formats, spaces/tabs, count precedence, lowercase routing, no mutation on range failure, and empty filtered results. No manual GUI checks performed.
* Handoff: awaiting user review and commit. Draft subject: `Validate delete command arguments`. Success formatting, persistence guarantees, and UI integration remain scheduled for later checkpoints.

* Checkpoint 1 review revision: added reusable `ParserUtil.parsePositiveInteger` for ASCII format validation and overflow-safe conversion. `DeleteCommandParser` retains argument-count checks and maps utility errors to F4 usage text. Existing `parseIndex` behavior is preserved. Renamed the empty-model logic test to make its empty-list expectation explicit. The assertion helper itself is unchanged. Parser/delete/logic tests and Checkstyle passed again; `git diff --check` passed. Still awaiting review.

* Checkpoint 1 approved: user explicitly gave the green light to begin checkpoint 2.
* Checkpoint 2 implemented: success output now uses the stored name and pre-deletion displayed index (`Deleted contact: NAME (index INDEX).`). Existing model deletion already retains the filter and remaining order; no model changes were needed. Added first/middle/last, filtered-position, preserved full-contact data/shared tags, and repeated-command regression coverage. Same-name records cannot currently coexist under shared creation rules, so that future integration case remains conditional.
* Checkpoint 2 validation passed: `./gradlew test --tests '*DeleteCommandTest' --tests '*logic.parser.*' --tests '*LogicManagerTest' checkstyleMain checkstyleTest` (85 tests, zero failures/errors); `git diff --check`. No manual GUI validation. Save-failure guarantees remain pending checkpoint 3; notes/details-panel dependencies remain pending checkpoint 4.
* Checkpoint 2 handoff: awaiting review and user commit. Draft subject: `Align deletion with displayed contact indices`.
