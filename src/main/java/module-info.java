module com.hugsforbugs.cs471pc {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.hugsforbugs.cs471pc to javafx.fxml;
    exports com.hugsforbugs.cs471pc;
}