---
layout: page
title: Testing guide
---

* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## Running tests

You can run tests in two ways.

* **Method 1: Using IntelliJ JUnit test runner**
  * To run all tests, right-click on the `src/test/java` folder and choose `Run 'All Tests'`
  * To run a subset of tests, you can right-click on a test package,
    test class, or a test and choose `Run 'ABC'`
* **Method 2: Using Gradle**
  * Open a console and run the command `gradlew clean test` (Mac/Linux: `./gradlew clean test`)

<div markdown="span" class="alert alert-secondary">:link: **Link**: Read [this Gradle Tutorial from the se-edu/guides](https://se-education.org/guides/tutorials/gradle.html) to learn more about using Gradle.
</div>

--------------------------------------------------------------------------------------------------------------------

## Types of tests

This project has three types of tests:

1. *Unit tests* target the lowest-level methods and classes.<br>
   For example: `seedu.address.commons.StringUtilTest`
1. *Integration tests* check how multiple code units work together; the individual units are assumed to work.<br>
   For example: `seedu.address.storage.StorageManagerTest`
1. *Hybrid tests* combine unit and integration testing. These tests check both the individual units and how they work together.<br>
   For example: `seedu.address.logic.LogicManagerTest`

## Testing session undo

Run the focused undo command, storage, and parser tests with:

```bash
./gradlew test --tests '*UndoCommandTest' --tests '*UndoSnapshotStorageTest' --tests '*AddressBookParserTest'
```

* `UndoCommandTest` uses an in-memory history stub to check preparation without mutation, restoration and history consumption on application, empty history, and unreadable snapshots.
* `UndoSnapshotStorageTest` covers full snapshots, non-consuming reads, the five-entry boundary, unchanged data, backup/save failures, unreadable or missing backups, history consumption, session isolation, and cleanup.
* `AddressBookParserTest` checks argument-free lowercase `undo`, surrounding whitespace, and rejected arguments or capitalization. Its history stub rejects access during parsing, ensuring parsing does not read or consume history.

These tests use equivalence partitions and meaningful boundaries rather than repeating equivalent inputs at every layer. Broader command-to-storage integration tests are deferred to the next milestone. For interactive checks, follow [Undoing contact changes](DeveloperGuide.md#undoing-contact-changes) in the manual testing appendix. Run `./gradlew check coverage` for the full suite, Checkstyle, and coverage reports.
