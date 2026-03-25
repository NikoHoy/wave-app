module wave.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.media;

    opens wave.app to javafx.fxml;
    exports wave.app;
}
