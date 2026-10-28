package seedu.address.logic.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.commands.GroupCommand;
import seedu.address.logic.commands.GroupCommand.Action;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.group.Group;
import seedu.address.model.person.Name;

/** Parses group creation, deletion, membership changes, and read-only views. */
public class GroupCommandParser implements Parser<GroupCommand> {
    private static final Pattern LIST = Pattern.compile("^\\s*list\\s*$");
    private static final Pattern VIEW = Pattern.compile("^\\s*view\\s+g/(.*?)\\s*$", Pattern.DOTALL);
    private static final Pattern CREATE = Pattern.compile("^\\s*add\\s+n/(.*?)\\s*$", Pattern.DOTALL);
    private static final Pattern DELETE = Pattern.compile("^\\s*delete\\s+g/(.*?)\\s*$", Pattern.DOTALL);
    private static final Pattern MEMBER = Pattern.compile(
            "^\\s*member\\s+g/(.*?)\\s+(add|remove)/(.*?)\\s*$", Pattern.DOTALL);

    @Override
    public GroupCommand parse(String args) throws ParseException {
        if (LIST.matcher(args).matches()) {
            return new GroupCommand(Action.LIST, null, null);
        }
        Matcher viewMatcher = VIEW.matcher(args);
        if (viewMatcher.matches()) {
            return new GroupCommand(Action.VIEW, parseGroupName(viewMatcher.group(1)), null);
        }
        Matcher createMatcher = CREATE.matcher(args);
        if (createMatcher.matches()) {
            return new GroupCommand(Action.CREATE, parseGroupName(createMatcher.group(1)), null);
        }
        Matcher deleteMatcher = DELETE.matcher(args);
        if (deleteMatcher.matches()) {
            return new GroupCommand(Action.DELETE, parseGroupName(deleteMatcher.group(1)), null);
        }
        Matcher memberMatcher = MEMBER.matcher(args);
        if (memberMatcher.matches()) {
            String groupName = parseGroupName(memberMatcher.group(1));
            Name personName = ParserUtil.parseName(memberMatcher.group(3));
            Action action = memberMatcher.group(2).equals("add") ? Action.ADD_MEMBER : Action.REMOVE_MEMBER;
            return new GroupCommand(action, groupName, personName);
        }
        throw new ParseException(GroupCommand.MESSAGE_USAGE);
    }

    private String parseGroupName(String rawName) throws ParseException {
        String name = rawName.strip();
        if (!Group.isValidName(name)) {
            throw new ParseException("Group name must be 1 to 100 characters and contain a letter or digit.");
        }
        return name;
    }
}
