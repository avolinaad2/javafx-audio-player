module com.example.pudgeplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires org.apache.commons.io;
    requires mp3agic;

    opens com.example.audioplayer to javafx.fxml;
    exports com.example.audioplayer;
}