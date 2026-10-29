---
layout: page
title: User Guide
---

AB4 is a **desktop contact manager for NUS students, optimized for use through a Command Line Interface (CLI)** while retaining the benefits of a Graphical User Interface (GUI). It helps you keep contact details, tags, and notes locally and retrieve contacts by name, tag, or note.

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

   * `favourite 1` : Adds the first displayed contact to Favourites, linked to its original.

   * `tab favourites` : Switches to Favourites. Use `tab contacts` to return to Contacts.

   * `clear` : Deletes all contacts in the active section.

   * `undo` : Reverses the latest change to Contacts or Favourites.

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

AB4 has two contact sections: **Contacts** and **Favourites**. Contacts added through `favourite` remain linked across the two sections. The application starts on Contacts. The label above the list shows the active section, and the footer shows its data file.

Format: `tab contacts` or `tab favourites`

* Switch sections by typing a command; the section label is not a clickable tab.
* Commands use the displayed contacts in the active section. Edits to linked contacts update both sections, and deleting or clearing Contacts also removes matching favourites. `undo` reverses the latest change across either section. You can use `add` to create a contact directly in Favourites; that contact has no linked original in Contacts.
* Each section keeps its own search results and display order during the current session. Switching back restores the view left by `find`, `filter`, or `recent`, including an empty result. Selecting the section that is already active keeps its view. A successful `undo` resets searches, tag filters, and display order in both sections, showing all contacts in addition order.
* Use `list` to show all contacts in the active section in addition order, or `recent` to show them newest first. Restarting resets both section views and starts on Contacts; searches and display order are not saved.
* Switching sections does not save or modify contact data. Success shows `Showing Contacts.` or `Showing Favourites.`
* Use the exact lowercase names `contacts` and `favourites`. A missing, unknown, or extra section produces `Invalid command format!` followed by the `tab` usage, without switching sections.

### Marking a contact as a favourite: `favourite`

Adds a linked contact from Contacts to Favourites, including its name, phone number, email, address, tags, and note.

Format: `favourite INDEX`

* Run this command in Contacts. `INDEX` refers to the currently displayed list, including search results.
* Supply exactly one positive ASCII integer without leading zeros, such as `1` or `12`. Signs, decimals, names, and extra arguments are rejected. Surrounding spaces and tabs are allowed.
* Success appends the linked contact to Favourites and shows `Added to Favourites: NAME (index INDEX).` Contacts remains selected, its search stays active, and the input clears.
* A yellow star appears beside a contact in Contacts when a favourite shares its phone number or email address, including a contact added directly in Favourites. Stars are hidden in the Favourites section and refresh after successful commands.
* Editing either linked contact updates its details in both sections. Deleting the original from Contacts also removes its favourite. Clearing Contacts removes favourites linked to the cleared contacts.
* Deleting or clearing Favourites leaves Contacts unchanged. Groups remain specific to each section; renaming or deleting a linked contact updates its existing group memberships in each affected section.
* A contact with the same phone number or email address cannot appear twice in Favourites. Contacts may share the same name. Repeating the command does not update or overwrite the existing favourite.
* To remove a favourite, run `tab favourites` and then `delete INDEX`. You can run `favourite` on the original again after removal.

For example, run `tab contacts`, then `find Betsy`, then `favourite 1` to favourite the first matching contact. Run `tab favourites`; if a retained search hides Betsy, run `list`. Then use Betsy's displayed index in `edit INDEX note/Met in class`. The new note also appears on the original contact in Contacts.

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

Contacts may have the same name as long as their phone numbers and email addresses differ. `add` checks the complete active collection, including contacts hidden by a current search or filter, and rejects a contact if either identity field is already used. No contact is added or saved on a conflict.

| Conflict | Message |
| --- | --- |
| Phone number matches one existing contact | `A contact with the same phone number already exists: NAME` |
| Email address matches one existing contact | `A contact with the same email address already exists: NAME` |
| Both fields match the same existing contact | `A contact with the same phone number and email address already exists: NAME` |
| Phone and email match two different existing contacts | The phone and email messages above are shown on separate lines, naming the respective contacts. |

### Listing, viewing, and managing groups: `group`

Groups belong to the active section (Contacts or Favourites). Create an empty group, then add or remove contacts by name or by their displayed contact index. The contact must already exist in that section. Group names are case sensitive and must be unique within the section.

* `group list` — show all groups in the active section and their member counts, including empty groups.
* `group view g/CS2103T Team` — show the group's members with their phone numbers.
* `group add n/CS2103T Team`
* `group delete g/CS2103T Team`
* `group member g/CS2103T Team add/John Tan`
* `group member g/CS2103T Team remove/John Tan`
* `group member g/CS2103T Team add/i/2` — add the contact at index 2 in the displayed list.
* `group member g/CS2103T Team remove/i/2` — remove that specific contact.

If there are no groups, `group list` reports `No groups exist.`. If a group has no members, `group view` reports `Group NAME has no members.`. Viewing a missing group reports `Group does not exist: NAME`.

Membership is stored by a persistent contact ID. A name can be used only when it identifies one contact in the active section. If several contacts share that name, the command asks you to use a displayed index, such as `add/i/2` or `remove/i/2`. The index follows the current list or search results and starts at 1. `group view` shows each member's name and phone number, and `group list` counts individual members. Renaming a contact keeps its membership; deleting one contact removes only that contact from groups.

Groups and their memberships are saved with the contacts. Listing and viewing groups do not save or change data. Deleting a group removes its memberships but keeps the contacts. Deleting a contact also removes it from its groups.

### Listing all persons: `list`

Shows a list of all persons in the active section.

Format: `list`

### Viewing recently added contacts: `recent`

Shows all contacts with the most recently added contact first.

Format: `recent`

* “Recent” means recently **added**, not recently edited. Editing a contact keeps it at its original position in the Recent ordering.
* While Recent is active, adding a contact keeps Recent active and displays the new contact first. Deleting a contact immediately removes it while preserving the Recent order of the remaining contacts.
* `list`, `find`, and `filter` switch away from Recent ordering to their normal addition-order results. These commands do not change the underlying addition order.
* The underlying addition order is saved and restored after restarting the application. The active Recent view itself is not restored; enter `recent` again to display newest-added-first order.
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
| Index exceeds the displayed list, including a very large positive integer such as `view 2147483648` | `Invalid index. Please select an index shown in the contact list.` |

### Editing a person: `edit`

Edits an existing person in the active section. If the person is linked to a contact in the other section, the edit updates both.

Format: `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]…​ [note/NOTE]`

* Edits the person at the specified `INDEX`. The index refers to the index number shown in the displayed person list. The index **must be a positive integer** 1, 2, 3, …​
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values. A linked edit is rejected if its phone number or email address conflicts with another contact in either affected section; neither contact is changed.
* Editing keeps the current `find` or `filter` results. A contact that no longer matches disappears from that list, and the other matches stay. Use `list` to show every contact again.
* When editing tags, all of the person's existing tags are removed; adding tags is not cumulative.
* To remove all of a person's tags, enter `t/` without a tag after it.
* To replace a note, use `note/NOTE` as the final argument, following the same rules as `add`. Omitting it preserves the existing note. For example: `edit 1 note/Discussed frontend implementation`.
* An edit may keep the contact's own phone number and email address, but its resulting phone number or email address cannot match any other contact in the complete active collection. The contact's name may match another contact's name. A conflict shows `This person already exists in the address book.` and leaves the contact unchanged.

Examples:


* `edit 1 p/91234567 e/johndoe@example.com` Edits the phone number and email address of the 1st person to be `91234567` and `johndoe@example.com` respectively.
* `edit 2 n/Betsy Crower t/` Edits the name of the 2nd person to be `Betsy Crower` and clears all existing tags.

### Locating persons by name, tag, or note: `find`

Finds persons in the active section whose names, tags, or notes match any of the given keywords.


Format: `find KEYWORD [MORE_KEYWORDS]`

* The search is case-insensitive and considers names, tags, and notes only.
* Names use partial matching; for example, `Han` matches `Hans`.
* Tags require an exact tag match; for example, `friend` does not match the tag `friends`.
* Notes are split only at whitespace. A keyword must equal one complete note token, ignoring case. Punctuation remains part of the token: for a note containing `Met at college,`, `find college,` matches but `find college` does not.
* Persons matching at least one keyword in any of those fields are returned (an `OR` search). Keyword order does not matter; for example, `Hans Bo` returns `Hans Gruber` and `Bo Yang`.

Examples:

* `find John` returns contacts named `john` or `John Doe`, as well as contacts with the exact tag or note token `John`.
* `find CS2103T` returns contacts tagged `CS2103T` (case-insensitive).
* `find alex david` returns `Alex Yeoh`, `David Li`<br>
  ![result for 'find alex david'](images/findAlexDavidResult.png)

### Filtering persons by tag: `filter`

Shows persons who have all of the given tags.

Format: `filter t/TAG [t/TAG]…​`

* Tag matching is case-insensitive; for example, `t/friends` matches `Friends`.
* Only whole tags match; for example, `t/friend` does not match `friends`.
* If more than one tag is given, a person must have every listed tag.
* While a filter is active, the heading above the contact list shows `Active filter: TAG`. It remains visible after commands such as `view`. `list`, `find`, and `recent` remove it in the active section. Switching sections shows the destination section's filter; switching back restores the previous filter and its heading.
* A filter that matches nobody shows `No contacts match the specified tag(s).` The contacts remain saved. Use `list` to show them again.
* The index numbers in the result list can be used by commands such as `view`, `edit`, and `delete`.
* Use `list` to show all persons again.

Examples:
* `filter t/friends` returns persons tagged `friends`.
* `filter t/friends t/owesMoney` returns persons who have both tags.

### Deleting a contact: `delete`

Removes one contact and its contact details, tags, and notes from the active section. Deleting an original in Contacts also removes its linked favourite and their group memberships. Deleting a favourite leaves its original in Contacts unchanged. Deletions from either section can be reversed using [undo](#undoing-a-contact-change-undo) while history is available.

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

Clears all contacts and groups from the active section. Clearing Contacts also removes favourites linked to those contacts and their memberships in Favourites groups; contacts created directly in Favourites and the Favourites groups themselves remain. Clearing Favourites leaves Contacts unchanged. The change can be undone while history is available.

Format: `clear`

### Undoing a contact change: `undo`

Reverses the most recent successful `add`, `delete`, `edit`, `clear`, `favourite`, or `group` operation that changed data in either section. Contacts and Favourites share one chronological history. You can run `undo` from either section.

Format: `undo`

* Use lowercase `undo` with no arguments. Surrounding spaces and tabs are accepted.
* Undo restores both sections to their states before the operation, including contact details, tags, notes, contact order, groups, and memberships. Mirrored changes are reversed together. The active section stays selected. Both sections show all contacts in addition order; their previous searches, tag filters, and sorting are reset.
* Up to five operations across both sections can be undone, most recent first. A sixth operation removes the oldest available undo entry. One operation that changes both sections uses one entry.
* Failed commands, identical edits, and clearing a section with no contacts or groups do not consume history. Clearing a section that contains only groups does consume an entry. Commands such as `tab`, `find`, `list`, and `view` leave history unchanged.
* Each successful undo consumes one history entry. Undo itself does not create an entry, and there is no redo command.
* History persists across normal restarts. If either saved collection is changed outside the app, its existing shared undo history is ignored on the next launch. If the saved history cannot be loaded at startup, the app starts without undo history.
* Success clears the input box. Failures retain the command and leave the contact data, displayed list, saved file, and available history unchanged.

For example, in Contacts with at least one contact displayed, execute `delete 1` and then `undo`. The deleted contact returns to its original position, and the full contact list is shown. If you next make another change, undo reverses that new change before reaching earlier history entries. For example, after editing a linked favourite, you can switch to Contacts and run `undo` to reverse that edit in both sections.

| Condition | Message |
| --- | --- |
| Undo succeeds | Feedback identifies the operation, such as `Undid delete of Betsy Crowe.` or `Undid clear of Favourites.`; `Undo successful.` is the fallback |
| No history is available | `No changes to undo.` |
| Extra arguments, such as `undo 1` | `Invalid command format!` followed by the `undo` usage |
| The stored undo history is missing, unreadable, or corrupted when undo tries to read an available entry | `Could not read undo history. Nothing was undone. Please try again.` |
| Saving the restored data fails | `Could not save changes. Nothing was undone. Please try again.` |

For example, entering `undo 1` displays:

```text
Invalid command format!
undo: Undoes the most recent change to Contacts or Favourites.
Example: undo
```

### Exiting the program: `exit`

Exits the program.

Format: `exit`

### Saving the data

AB4 automatically saves the collections and their shared undo history together after a successful `add`, `edit`, `delete`, `clear`, `favourite`, `group`, or `undo`. A collection that failed to load is preserved when a command changes only the other section, including when that change is undone. Commands targeting the failed section can replace its file. Identical edits and clearing a section with no contacts or groups still save without adding undo history, allowing `clear` to replace a corrupted file after startup has opened an empty collection. Both collections, including tags, notes, and groups, and the latest five undo entries persist across restarts. The app starts on Contacts. Missing Contacts data uses sample contacts; a missing favourites file starts with an empty Favourites collection. You do not need to save manually.

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

These paths are fixed; they cannot be changed through `preferences.json`. Both files use the same UTF-8 format and can be edited while the application is closed. The `persons` array contains contacts with `name`, `phone`, `email`, `address`, `tags` (objects with a `tagName`), and an optional `note` string. An omitted or `null` note means no note; older files without notes remain supported. Keep the JSON structure and use the same contact values accepted by the application. Within each file, contacts are duplicates if they share a phone number or email address; contacts may share the same name.

Each saved contact also has an `id` containing a UUID. Linked contacts share the same ID across the two files; preserve these IDs when editing details. IDs must be valid and unique within each file. A contact created directly in Favourites has its own ID and no linked original. Files without IDs can be loaded, but their records receive new IDs independently; matching details alone do not link contacts across files.

<div markdown="span" class="alert alert-warning">:exclamation: **Caution:**
If a file cannot be loaded, AB4 displays a startup error naming the affected collection and file, and starts that collection empty. The other collection loads independently. The error distinguishes a file that cannot be read, corrupted or incorrectly formatted JSON, and invalid contact information (including duplicate contacts). No contacts from an invalid file are loaded. Commands that change only the other section, and undoing those changes, leave the failed file untouched. Back up or repair the original file before changing the affected section; commands targeting it, including `clear` or adding a favourite to failed Favourites, can replace it with new data. If a file cannot be read at all, saving can still fail; restore file access and retry.
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
**Groups** | `group list`, `group view g/GROUP NAME`, `group add n/GROUP NAME`, `group delete g/GROUP NAME`, `group member g/GROUP NAME add/CONTACT NAME`, `group member g/GROUP NAME remove/CONTACT NAME`<br>Use `add/i/INDEX` or `remove/i/INDEX` to select one contact by its displayed index.
**Favourite** | `favourite INDEX`<br> e.g., `favourite 1` (from Contacts)
**Switch section** | `tab contacts` or `tab favourites`
**Filter** | `filter t/TAG [t/TAG]…​`<br> e.g., `filter t/friends t/owesMoney`
**List** | `list`
**Recent** | `recent`
**Undo** | `undo`
**View** | `view INDEX`<br> e.g., `view 1`
**Help** | `help`
**Exit** | `exit`
