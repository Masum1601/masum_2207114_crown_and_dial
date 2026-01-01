module com.example.final_project_114 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires transitive javafx.graphics;
    requires transitive java.sql;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires org.slf4j;
    requires jbcrypt;

    opens com.example.final_project_114 to javafx.fxml;
    opens com.example.final_project_114.model to javafx.base;
    exports com.example.final_project_114;
    exports com.example.final_project_114.model;
}