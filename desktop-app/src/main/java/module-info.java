module com.example.desktopapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.desktopapp to javafx.fxml;
    exports com.example.desktopapp;
}