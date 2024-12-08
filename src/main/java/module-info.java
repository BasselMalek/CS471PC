module com.hugsforbugs.cs471pc {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.logging;

    opens com.hugsforbugs.cs471pc to javafx.fxml;
    exports com.hugsforbugs.cs471pc;
}