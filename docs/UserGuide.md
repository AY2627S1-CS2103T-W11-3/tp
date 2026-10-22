---
layout: page
title: User Guide
---

AB4 is a **desktop contact manager for NUS students, optimized for use through a Command Line Interface (CLI)** while retaining the benefits of a Graphical User Interface (GUI). It helps you keep contact details, tags, and notes locally and retrieve contacts by name or tag.

* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br> **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download `addressbook.jar` from the [AB4 releases page](https://github.com/AY2627S1-CS2103T-W11-3/tp/releases).

1. Copy the file to the folder you want to use as the _home folder_ for AB4.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar addressbook.jar`.<br> A GUI similar to the one below should appear in a few seconds. Note how the app contains some sample data.<br>
   ![Ui](images/Ui.jpg)

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to open the help window.<br>
   While typing the command word, you can press **Tab** to autocomplete it. If several commands share the same prefix, press Tab again to cycle through them. Tab does not autocomplete arguments.<br>
   Some example commands you can try:

   * `list` : Lists all contacts in the active section.

   * `recent` : Lists all contacts with the most recently added contact first.

   * `add n/John Doe p/98765432 e/johnd@example.com a/John street, block 123, #01-01` : Adds a contact named `John Doe` to the Address Book.

   * `delete 3` : Deletes the 3rd contact shown in the current list.

   * `favourite 1` : Copies the first displayed contact into Favourites.

   * `tab favourites` : Switches to Favourites. Use `tab contacts` to return to Contacts.

   * `clear` : Deletes all contacts in the active section.

   * `undo` : Restores Contacts before its most recent change in this session.

   * `exit` : Exits the app.

   Press the Up arrow key to fill the command box with an earlier command. Press it repeatedly to move backward through
   commands entered during the current launch, and press the Down arrow key to move forward toward newer commands.
   Command history is cleared whenever AB4 is launched.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

## Features

<div markdown="block" class="alert alert-info">

**:information_source: Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br> For example, in `add n/NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br> For example, `n/NAME [t/TAG]` can be used as `n/John Doe t/friend` or as `n/John Doe`.

* Items followed by `…`​ can appear zero or more times.<br> For example, `[t/TAG]…​` may be omitted, or written as `t/friend` or `t/friend t/family`.

* Prefixed fields in `add` and `edit` can be in any order, except that `note/NOTE` must be last. For indexed commands such as `edit`, put `INDEX` before the fields.<br> For example, `add p/91234567 n/John Doe e/john@example.com a/NUS note/Met in class` is valid.

* Extraneous parameters for `help`, `list`, `exit`, and `clear` are ignored.<br> For example, `help 123` is interpreted as `help`. The `undo` and `recent` commands reject extra parameters.

* Press **Tab** in the command box to autocomplete the command word (the first word only). Matching is case-sensitive. If more than one command matches the typed prefix, Tab cycles through the matches in alphabetical order. After you type a space and start arguments, Tab no longer changes the text.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</div>

### Autocompleting a command word

While the caret is in the command word, press **Tab** to complete it to a known command.

* Matching uses a case-sensitive prefix. For example, `li` + Tab becomes `list`.
* If several commands match, Tab cycles through them. For example, `fi` + Tab cycles between `filter` and `find`.
* Tab does not autocomplete arguments, tags, or prefixes such as `n/` or `t/`.
* Enter still runs whatever text is currently in the command box.

### Switching contact sections: `tab`

AB4 has two independent collections: **Contacts** and **Favourites**. The application starts on Contacts. The label above the list shows the active section, and the footer shows its data file.

Format: `tab contacts` or `tab favourites`

* Switch sections by typing a command; the section label is not a clickable tab.
* `add`, `edit`, `delete`, `clear`, `find`, `filter`, `list`, `recent`, and `view` operate only on the active section. You can also use `add` to create a contact directly in Favourites.
* Switching to a different section shows all contacts there. Returning to a section resets its previous search. Selecting the section that is already active keeps its current search.
* Switching sections does not save or modify contact data. Success shows `Showing Contacts.` or `Showing Favourites.`
* Use the exact lowercase names `contacts` and `favourites`. A missing, unknown, or extra section produces `Invalid command format!` followed by the `tab` usage, without switching sections.

### Marking a contact as a favourite: `favourite`

Copies a contact from Contacts into Favourites, including its phone number, email, address, tags, and note.

Format: `favourite INDEX`

* Run this command in Contacts. `INDEX` refers to the currently displayed list, including search results.
* Supply exactly one positive ASCII integer without leading zeros, such as `1` or `12`. Signs, decimals, names, and extra arguments are rejected. Surrounding spaces and tabs are allowed.
* Success appends the copy to Favourites and shows `Added to Favourites: NAME (index INDEX).` Contacts remains selected, its search stays active, and the input clears.
* The copy is independent. Later edits, deletions, and clearing affect only the selected collection. A favourite can remain after its original contact is deleted.
* A contact with the same phone number or email address cannot appear twice in Favourites. Contacts may share the same name. Repeating the command does not update or overwrite the existing favourite.
* To remove a favourite, run `tab favourites` and then `delete INDEX`. You can favourite the original again after removing its copy.

For example, run `tab contacts`, then `find Betsy`, then `favourite 1` to copy the first matching contact. Run `tab favourites` to see the saved copy.

Validation follows the table order. `N` is the number of displayed contacts. Failures retain the entered command and leave both collections unchanged.

| Condition | Message |
| --- | --- |
| Missing, extra, or malformed index, such as `favourite`, `favourite 1 2`, or `favourite 01` | `Invalid command format!` followed by the `favourite` usage |
| Active section is Favourites | `You are already in Favourites. Switch to Contacts to favourite a contact.` |
| No displayed contacts | `No contacts are displayed. Nothing to favourite.` |
| Index exceeds the displayed count, including an extremely large valid integer | `Contact index out of range. Choose an index from 1 to N.` |
| The phone number or email address already exists in Favourites | `This contact is already in Favourites.` |
| Saving fails | `Could not save changes. No changes were applied. Please try again.` |

### Viewing help: `help`

Shows a message explaining how to access the help page.

![help message](images/helpMessage.png)

Format: `help`

### Adding a person: `add`

Adds a person to the active section.

Format: `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]…​ [note/NOTE]`

<div markdown="span" class="alert alert-primary">:bulb: **Tip:**
A person can have any number of tags, including zero.
</div>

The optional `note/` prefix is case-insensitive and must be the final prefix in the command. Notes are trimmed and must contain 1–1000 printable Unicode characters, with no line breaks or control characters.

Examples:

* `add n/John Doe p/98765432 e/johnd@example.com a/John street, block 123, #01-01`
* `add n/Betsy Crowe t/friend e/betsycrowe@example.com a/Newgate Prison p/1234567 t/criminal`
* `add n/John Doe p/98765432 e/johnd@example.com a/John street note/Known through a dating app`

### Listing all persons: `list`

Shows a list of all persons in the active section.

Format: `list`

### Viewing recently added contacts: `recent`

Shows all contacts with the most recently added contact first.

Format: `recent`

* Addition order is based on the order in which contacts were added. Editing a contact does not make it recent again.
* Deleted contacts are omitted. Commands such as `find`, `list`, and `view` do not change addition order.
* The ordering is preserved after restarting the application.
* If there are no contacts, the command shows `No contacts available.`
* The command does not accept parameters.

Example: After adding `John Doe` and then `Betsy Crowe`, `recent` displays Betsy before John.

### Viewing a person: `view`

Shows the full details of the person at the specified index. The person is not added, edited, or deleted.

Format: `view INDEX`

* The index refers to the index number shown in the displayed person list. The index **must be a positive integer** 1, 2, 3, …​
* After `find`, the index refers to the filtered list on screen.
* Leading and trailing spaces are ignored, so `view 1` and `view    1` show the same person.
* The result lists name, phone, email, and address. A stored note is included as `Note: NOTE`. A person with no note omits that line. Tags are listed in alphabetical order, separated by commas. A person with no tags shows `Tags: Not specified`.
* The result ends with `Showing details for NAME.`

Examples:

* `view 1` shows the first person in the displayed list.
* `find Betsy` followed by `view 1` shows the first person in the search results.

| Condition | Message |
| --- | --- |
| Missing index, extra words, or a value that is not a positive integer, such as `view`, `view 1 John`, `view 0`, or `view Alice` | `Invalid command format!` followed by the `view` usage |
| No contacts are displayed | `No contacts available to view.` |
| Index exceeds the displayed list | `Invalid index. Please select an index shown in the contact list.` |

### Editing a person: `edit`

Edits an existing person in the active section.

Format: `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]…​ [note/NOTE]`

* Edits the person at the specified `INDEX`. The index refers to the index number shown in the displayed person list. The index **must be a positive integer** 1, 2, 3, …​
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values.
* When editing tags, all of the person's existing tags are removed; adding tags is not cumulative.
* To remove all of a person's tags, enter `t/` without a tag after it.
* To replace a note, use `note/NOTE` as the final argument, following the same rules as `add`. Omitting it preserves the existing note. For example: `edit 1 note/Discussed frontend implementation`.

Examples:


* `edit 1 p/91234567 e/johndoe@example.com` Edits the phone number and email address of the 1st person to be `91234567` and `johndoe@example.com` respectively.
* `edit 2 n/Betsy Crower t/` Edits the name of the 2nd person to be `Betsy Crower` and clears all existing tags.

### Locating persons by name, tag, or note: `find`

Finds persons in the active section whose names, tags, or notes contain any of the given keywords.


Format: `find KEYWORD [MORE_KEYWORDS]`

* The search is case-insensitive; for example, `hans` matches `Hans`.
* Keyword order does not matter; for example, `Hans Bo` matches `Bo Hans`.


* The search considers names, tags, and notes only.
* Only full words match; for example, `Han` does not match `Hans`.

* Persons matching at least one keyword are returned (an `OR` search); for example, `Hans Bo` returns `Hans Gruber` and `Bo Yang`.

Examples:

* `find John` returns contacts named `john` or `John Doe`, as well as contacts tagged `John`.
* `find CS2103T` returns contacts tagged `CS2103T` (case-insensitive).
* `find alex david` returns `Alex Yeoh`, `David Li`<br>
  ![result for 'find alex david'](images/findAlexDavidResult.png)

### Filtering persons by tag: `filter`

Shows persons who have all of the given tags.

Format: `filter t/TAG [t/TAG]…​`

* Tag matching is case-insensitive; for example, `t/friends` matches `Friends`.
* Only whole tags match; for example, `t/friend` does not match `friends`.
* If more than one tag is given, a person must have every listed tag.
* The index numbers in the result list can be used by commands such as `view`, `edit`, and `delete`.
* Use `list` to show all persons again.

Examples:
* `filter t/friends` returns persons tagged `friends`.
* `filter t/friends t/owesMoney` returns persons who have both tags.

### Deleting a contact: `delete`

Removes one contact and its contact details, tags, and notes from the active section. A recent deletion from Contacts can be reversed using [undo](#undoing-a-contact-change-undo) while history is available. Favourites deletions cannot be undone yet. Deleting a favourite does not delete its original in Contacts, and deleting an original does not delete its favourite copy.

Format: `delete <INDEX>`

Angle brackets mark the required index; do not type the brackets.

* Use the number beside the contact in the currently displayed list. Search results use their own numbering; the selected contact is removed from the full saved collection.
* Supply exactly one index using ASCII digits, starting with 1–9. Leading zeros, signs, decimals, scientific notation, names, and multiple indices are rejected.
* The keyword must be lowercase `delete`. Surrounding spaces/tabs and multiple spaces/tabs between the keyword and index are allowed; whitespace inside an index is rejected.
* Success shows `Deleted contact: NAME (index INDEX).`, using the stored name and the index before deletion. Input clears, the current search remains active, and remaining contacts retain their order and are renumbered from 1. Other contacts and their shared tags are preserved.
* Deleting the last displayed contact shows `No contacts to display.` in the list area. Repeating the command uses the updated numbering and may delete a different contact.
* Failures retain the entered command and leave contacts and search results unchanged. A failed save preserves the previous file and live contacts. Successful changes persist after restarting.

Examples: `delete 3` removes the third displayed contact; `find Betsy` followed by `delete 1` removes the first search result.

Validation follows the table order. `N` is the number of displayed contacts; even an extremely large valid integer receives the empty-list or range response.

| Condition | Message |
| --- | --- |
| Missing index | `Missing contact index.` |
| More than one argument | `Expected exactly one contact index.` |
| Invalid format, e.g. `0`, `01`, `-1`, `+1`, `abc` | `Invalid contact index. Enter a positive whole number without leading zeros.` |
| No displayed contacts | `No contacts are displayed. Nothing to delete.` |
| Index exceeds displayed count | `Contact index out of range. Choose an index from 1 to N.` |
| Saving fails | `Could not save changes. Contact was not deleted. Please try again.` |

Missing, extra, or malformed indices also show the command description, required parameter, and example on separate lines. For example, entering `delete` displays:

```text
Missing contact index.
delete: Deletes the contact identified by the index number used in the displayed contact list.
Parameters: <INDEX> (must be a positive integer without leading zeros)
Example: delete 1
```

### Clearing all entries: `clear`

Clears all entries from the active section. Clearing Contacts leaves Favourites unchanged, and clearing Favourites leaves Contacts unchanged.

Format: `clear`

### Undoing a contact change: `undo`

Restores Contacts from before its most recent successful `add`, `delete`, `edit`, or `clear` that changed data. Undo is available only in Contacts; use `tab contacts` to switch. Favourites changes, including the `favourite` command, cannot be undone in this milestone and do not consume Contacts history.

Format: `undo`

* Use lowercase `undo` with no arguments. Surrounding spaces and tabs are accepted.
* Undo restores all Contacts fields, tags, notes, and contact order, then shows all Contacts. Favourites remains unchanged. The previous search filter is not restored.
* Up to five Contacts changes can be undone, most recent first. A sixth Contacts change removes the oldest available undo entry. Switching tabs and changing Favourites do not consume this history.
* Failed commands, identical edits, and clearing an already empty address book do not consume history. Commands such as `find`, `list`, and `view` leave it unchanged.
* Each successful undo consumes one history entry. Undo itself does not create an entry, and there is no redo command.
* History lasts only for the current app session. Closing and reopening the app clears undo history; the contact data saved by a successful undo remains saved.
* Success clears the input box. Failures retain the command and leave the contact data, displayed list, saved file, and available history unchanged.

For example, in Contacts with at least one contact displayed, execute `delete 1` and then `undo`. The deleted contact returns to its original position, and the full contact list is shown. If you next make another change, undo reverses that new change before reaching earlier history entries.

| Condition | Message |
| --- | --- |
| Undo succeeds | `Undo successful.` |
| Favourites is active | `Undo is only available in Contacts. Switch using tab contacts.` |
| No Contacts history is available, including after restart | `No changes to undo.` |
| Extra arguments, such as `undo 1` | `Invalid command format!` followed by the `undo` usage |
| A backup is missing, unreadable, or corrupted | `Could not read undo history. Nothing was undone. Please try again.` |
| Saving the restored data fails | `Could not save changes. Nothing was undone. Please try again.` |

For example, entering `undo 1` displays:

```text
Invalid command format!
undo: Undoes the most recent change to Contacts.
Example: undo
```

### Exiting the program: `exit`

Exits the program.

Format: `exit`

### Saving the data

AB4 automatically saves the active section after a successful `add`, `edit`, `delete`, `clear`, or `undo`. A successful `favourite` saves only Favourites. Identical edits and clearing an empty collection still save without adding undo history, allowing `clear` to repair a corrupted file after startup has opened an empty collection. Both collections, including tags and notes, persist across restarts; Contacts undo history resets. The app starts on Contacts. Missing Contacts data uses sample contacts; a missing favourites file starts with an empty Favourites collection. You do not need to save manually.

Changes appear only after saving succeeds. If saving fails, contacts, the displayed list, and the previous data file remain unchanged. The command stays in the input box so you can retry.

For a failed deletion, the message is: `Could not save changes. Contact was not deleted. Please try again.`

For a failed undo save, the message is: `Could not save changes. Nothing was undone. Please try again.`

For failed saves through `add`, `edit`, `clear`, or `favourite`, the message is: `Could not save changes. No changes were applied. Please try again.`

Commands such as `tab`, `find`, `filter`, `list`, `recent`, `view`, `help`, and `exit` do not save contact data and remain available when saving fails. Saving requires a writable data location and a filesystem that supports atomic file replacement.


### Editing the data files

AB4 saves two human-readable JSON files, relative to the directory from which the application is launched:

| Collection | File |
| --- | --- |
| Contacts | `data/addressbook.json` |
| Favourites | `data/favourites.json` |

These paths are fixed; they cannot be changed through `preferences.json`. Both files use the same UTF-8 format and can be edited while the application is closed. The `persons` array contains contacts with `name`, `phone`, `email`, `address`, `tags` (objects with a `tagName`), and an optional `note` string. An omitted or `null` note means no note; older files without notes remain supported. Keep the JSON structure and use the same contact values accepted by the application. Within each file, contacts are duplicates if they share a phone number or email address; contacts may share the same name. The files contain independent contacts, with no links between copies.

<div markdown="span" class="alert alert-warning">:exclamation: **Caution:**
If a file cannot be loaded, AB4 displays a startup error naming the affected collection and file, and starts that collection empty. The other collection loads independently. The error distinguishes a file that cannot be read, corrupted or incorrectly formatted JSON, and invalid contact information (including duplicate contacts). No contacts from an invalid file are loaded. The original file remains on disk until a command successfully saves new data to that collection. Back up or repair the original file before changing that collection; favouriting a contact also writes to the favourites file.
</div>

### Archiving data files `[coming in v2.0]`

_Details coming soon ..._

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Install the app on the other computer. With the app closed on both computers, copy both `data/addressbook.json` and `data/favourites.json` from your previous AB4 home folder into the new home folder's `data` directory. Copy only the files that exist, and back up any destination files before replacing them.

--------------------------------------------------------------------------------------------------------------------

## Known issues

1. **When using multiple screens**, if you move the application to a secondary screen, and later switch to using only the primary screen, the GUI will open off-screen. The remedy is to delete the `preferences.json` file created by the application before running the application again.
1. **If you minimize the Help Window** and then run the `help` command (or use the `Help` menu, or the keyboard shortcut `F1`) again, the original Help Window will remain minimized, and no new Help Window will appear. The remedy is to manually restore the minimized Help Window.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action | Format, Examples
--------|------------------
**Add** | `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]…​ [note/NOTE]` <br> e.g., `add n/James Ho p/22224444 e/jamesho@example.com a/123, Clementi Rd, 1234665 t/friend note/Met at work`
**Clear** | `clear`
**Delete** | `delete <INDEX>`<br> e.g., `delete 3`
**Edit** | `edit INDEX [n/NAME] [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS] [t/TAG]…​ [note/NOTE]`<br> e.g., `edit 2 n/James Lee e/jameslee@example.com`
**Find** | `find KEYWORD [MORE_KEYWORDS]`<br> e.g., `find James Jake`
**Favourite** | `favourite INDEX`<br> e.g., `favourite 1` (from Contacts)
**Switch section** | `tab contacts` or `tab favourites`
**Filter** | `filter t/TAG [t/TAG]…​`<br> e.g., `filter t/friends t/owesMoney`
**List** | `list`
**Recent** | `recent`
**Undo** | `undo`
**View** | `view INDEX`<br> e.g., `view 1`
**Help** | `help`
**Exit** | `exit`
