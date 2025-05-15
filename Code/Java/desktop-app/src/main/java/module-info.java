module projectDay.desktopApp {
    requires javafx.controls;
    requires javafx.web;
    requires javafx.swing;
    requires org.slf4j;

    opens com.beMore to javafx.graphics;
}