module com.hugsforbugs.cs471pc {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires org.apache.commons.net;
    requires mysql.connector.j;
    requires java.sql;
    requires commons.io;
    requires java.net.http;

    opens com.hugsforbugs.cs471pc to javafx.fxml;
    exports com.hugsforbugs.cs471pc;
}
