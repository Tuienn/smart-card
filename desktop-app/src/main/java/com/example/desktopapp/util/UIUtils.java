package com.example.desktopapp.util;

import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * Utility class providing common UI helper methods for controllers.
 */
public final class UIUtils {

    private UIUtils() {
        // Prevent instantiation
    }

    /**
     * Shows a warning alert dialog.
     * 
     * @param title   The title of the alert
     * @param message The message content
     */
    public static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an alert dialog with specified type.
     * 
     * @param type    The alert type
     * @param title   The title of the alert
     * @param message The message content
     */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error alert dialog.
     * 
     * @param title   The title of the alert
     * @param header  The header text
     * @param message The message content
     */
    public static void showError(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Creates a FontIcon with specified icon, color and size.
     * 
     * @param iconCode The FontAwesome icon code
     * @param color    The color as hex string (e.g., "#22c55e" or "white")
     * @param size     The icon size in pixels
     * @return Configured FontIcon instance
     */
    public static FontIcon createIcon(FontAwesomeSolid iconCode, String color, int size) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(size);
        icon.setIconColor(Color.web(color));
        return icon;
    }

    /**
     * Creates a FontIcon with specified icon and size (default white color).
     * 
     * @param iconCode The FontAwesome icon code
     * @param size     The icon size in pixels
     * @return Configured FontIcon instance with white color
     */
    public static FontIcon createIcon(FontAwesomeSolid iconCode, int size) {
        return createIcon(iconCode, "white", size);
    }
}
