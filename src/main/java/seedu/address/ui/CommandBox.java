package seedu.address.ui;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import seedu.address.logic.CommandAutocompleter;
import seedu.address.logic.CommandWords;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * The UI component that is responsible for receiving user command inputs.
 */
public class CommandBox extends UiPart<Region> {

    public static final String ERROR_STYLE_CLASS = "error";
    private static final String FXML = "CommandBox.fxml";

    private final CommandExecutor commandExecutor;
    private final CommandAutocompleter autocompleter;
    private boolean isUpdatingFromAutocomplete;

    @FXML
    private TextField commandTextField;

    /**
     * Creates a {@code CommandBox} with the given {@code CommandExecutor}
     * and the default command-word list.
     */
    public CommandBox(CommandExecutor commandExecutor) {
        this(commandExecutor, CommandWords.get());
    }

    /**
     * Creates a {@code CommandBox} with the given {@code CommandExecutor}
     * and command words used for Tab autocomplete.
     */
    public CommandBox(CommandExecutor commandExecutor, List<String> commandWords) {
        super(FXML);
        this.commandExecutor = commandExecutor;
        this.autocompleter = new CommandAutocompleter(commandWords);
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> {
            setStyleToDefault();
            if (!isUpdatingFromAutocomplete) {
                autocompleter.reset();
            }
        });
        commandTextField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleTabPressed);
    }

    /**
     * Completes the command word when Tab is pressed.
     */
    private void handleTabPressed(KeyEvent event) {
        if (event.getCode() != KeyCode.TAB) {
            return;
        }

        var completion = autocompleter.complete(commandTextField.getText(), commandTextField.getCaretPosition());
        if (completion.isEmpty()) {
            return;
        }

        event.consume();
        isUpdatingFromAutocomplete = true;
        try {
            commandTextField.setText(completion.get().newText());
            commandTextField.positionCaret(completion.get().newCaretPosition());
        } finally {
            isUpdatingFromAutocomplete = false;
        }
    }

    /**
     * Handles the Enter button pressed event.
     */
    @FXML
    private void handleCommandEntered() {
        String commandText = commandTextField.getText();
        if (commandText.equals("")) {
            return;
        }

        try {
            commandExecutor.execute(commandText);
            commandTextField.setText("");
        } catch (CommandException | ParseException e) {
            setStyleToIndicateCommandFailure();
        }
    }

    /**
     * Sets the command box style to use the default style.
     */
    private void setStyleToDefault() {
        commandTextField.getStyleClass().remove(ERROR_STYLE_CLASS);
    }

    /**
     * Sets the command box style to indicate a failed command.
     */
    private void setStyleToIndicateCommandFailure() {
        ObservableList<String> styleClass = commandTextField.getStyleClass();

        if (styleClass.contains(ERROR_STYLE_CLASS)) {
            return;
        }

        styleClass.add(ERROR_STYLE_CLASS);
    }

    /**
     * Represents a function that can execute commands.
     */
    @FunctionalInterface
    public interface CommandExecutor {
        /**
         * Executes the command and returns the result.
         *
         * @see seedu.address.logic.Logic#execute(String)
         */
        CommandResult execute(String commandText) throws CommandException, ParseException;
    }

}
