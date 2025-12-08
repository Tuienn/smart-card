package com.example.desktopapp.controller;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the reusable PIN Keypad component.
 * Allows input of a 6-digit PIN with visual feedback.
 */
public class PinKeypadController implements Initializable {

    private static final int MAX_PIN_LENGTH = 6;

    @FXML private Label pinDot1, pinDot2, pinDot3, pinDot4, pinDot5, pinDot6;
    @FXML private Label instructionLabel;
    @FXML private Button clearBtn, backspaceBtn;

    private final StringProperty pin = new SimpleStringProperty("");
    private Label[] pinDots;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pinDots = new Label[]{pinDot1, pinDot2, pinDot3, pinDot4, pinDot5, pinDot6};
        updatePinDisplay();
    }

    /**
     * Handles numeric key press
     */
    @FXML
    private void onKeyPress(ActionEvent event) {
        if (pin.get().length() >= MAX_PIN_LENGTH) {
            return;
        }

        Button btn = (Button) event.getSource();
        String digit = (String) btn.getUserData();
        pin.set(pin.get() + digit);
        updatePinDisplay();
    }

    /**
     * Clears all entered digits
     */
    @FXML
    private void onClearAll() {
        pin.set("");
        updatePinDisplay();
    }

    /**
     * Removes the last entered digit
     */
    @FXML
    private void onBackspace() {
        String current = pin.get();
        if (!current.isEmpty()) {
            pin.set(current.substring(0, current.length() - 1));
            updatePinDisplay();
        }
    }

    /**
     * Updates the visual PIN dot display
     */
    private void updatePinDisplay() {
        int length = pin.get().length();
        for (int i = 0; i < pinDots.length; i++) {
            pinDots[i].getStyleClass().removeAll("pin-dot-filled", "pin-dot-empty");
            if (i < length) {
                pinDots[i].getStyleClass().add("pin-dot-filled");
            } else {
                pinDots[i].getStyleClass().add("pin-dot-empty");
            }
        }

        // Update instruction based on state
        if (length == 0) {
            instructionLabel.setText("Nhập mã PIN 6 số");
            instructionLabel.getStyleClass().remove("pin-instruction-complete");
        } else if (length < MAX_PIN_LENGTH) {
            instructionLabel.setText("Còn " + (MAX_PIN_LENGTH - length) + " số nữa");
            instructionLabel.getStyleClass().remove("pin-instruction-complete");
        } else {
            instructionLabel.setText("✓ Hoàn tất");
            if (!instructionLabel.getStyleClass().contains("pin-instruction-complete")) {
                instructionLabel.getStyleClass().add("pin-instruction-complete");
            }
        }
    }

    /**
     * Gets the current PIN value
     */
    public String getPin() {
        return pin.get();
    }

    /**
     * Sets the PIN value programmatically
     */
    public void setPin(String value) {
        if (value != null && value.length() <= MAX_PIN_LENGTH && value.matches("\\d*")) {
            pin.set(value);
            updatePinDisplay();
        }
    }

    /**
     * Gets the PIN property for binding
     */
    public ReadOnlyStringProperty pinProperty() {
        return pin;
    }

    /**
     * Checks if PIN is complete (6 digits)
     */
    public boolean isPinComplete() {
        return pin.get().length() == MAX_PIN_LENGTH;
    }

    /**
     * Resets the keypad
     */
    public void reset() {
        pin.set("");
        updatePinDisplay();
    }

    /**
     * Sets the instruction label text
     */
    public void setInstructionText(String text) {
        instructionLabel.setText(text);
    }
}
