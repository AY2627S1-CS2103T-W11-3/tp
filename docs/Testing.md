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

Run the undo storage, command integration, and parser tests with:

```bash
./gradlew test --tests '*UndoSnapshotStorageTest' --tests '*UndoableCommandIntegrationTest' --tests '*AddressBookParserTest'
```

* `UndoSnapshotStorageTest` covers full snapshots, non-consuming reads, the five-entry boundary, failed first saves, unreadable or missing backups, history consumption, and session isolation.
* `UndoableCommandIntegrationTest` exercises add, edit, delete, clear, and undo through the parser, logic, model, and real JSON storage. It checks successful restoration, unchanged or rejected commands, failure preservation, retry, and cleanup.
* `AddressBookParserTest` checks argument-free lowercase `undo`, surrounding whitespace, and rejected arguments or capitalization. Its history stub rejects access during parsing, ensuring parsing does not read or consume history.

These tests use equivalence partitions and meaningful boundaries rather than repeating equivalent inputs at every layer. For interactive checks, follow [Undoing contact changes](DeveloperGuide.md#undoing-contact-changes) in the manual testing appendix. Run `./gradlew check coverage` for the full suite, Checkstyle, and coverage reports.
