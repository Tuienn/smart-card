module com.example.desktopapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.smartcardio;
    requires java.desktop;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.json;

    opens com.example.desktopapp to javafx.fxml;
    opens com.example.desktopapp.controller to javafx.fxml;
    
    exports com.example.desktopapp;
    exports com.example.desktopapp.controller;
    exports com.example.desktopapp.model;
    exports com.example.desktopapp.service;
    exports com.example.desktopapp.util;
}