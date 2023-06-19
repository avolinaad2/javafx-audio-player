package com.example.audioplayer;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.apache.commons.io.FilenameUtils;

import static java.lang.Math.round;

public class Controller implements Initializable {
    @FXML
    private Pane pane;
    @FXML
    private Label artistLabel, titleLabel, albumLabel;
    @FXML
    private Button playButton, previousButton, resetButton, nextButton, openButton, forwardButton;
    @FXML
    private Text volumeText;
    @FXML
    private ProgressBar songProgressBar;
    @FXML
    private Slider volumeSlider;
    @FXML
    private ImageView coverImageView;
    @FXML
    private ListView libraryList;

    private Media media;
    private MediaPlayer mediaPlayer;
    private ArrayList<File> songs;

    private int songNumber;

    private Timer timer;
    private boolean timerRunning;
    private boolean running;

    private String path;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        getPath();
        updateDirectory();

        try {
            updateTitle();
        } catch (InvalidDataException | UnsupportedTagException | IOException e) {
            throw new RuntimeException(e);
        }

        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);
                volumeText.setText("Volume: " + round(volumeSlider.getValue() * 2) + "%");
            }
        });
    }

    public void getPath() {
        try(BufferedReader br = new BufferedReader(new FileReader("savedDir.txt"))) {
            path = br.readLine();
        } catch (IOException e) {
            path = "music";
//            throw new RuntimeException(e);
        }
    }

    public void updateDirectory() {
        songs = new ArrayList<File>();

        File directory = new File(path);

        File[] files = directory.listFiles();

        for (File file : files) {
            if (FilenameUtils.getExtension(file.getName()).equals("mp3")) {
                songs.add(file);
            }
        }

        libraryList.getItems().clear();
        for (File song : songs) {
            libraryList.getItems().add(song.getName());
//            System.out.println(song.getName());
        }
//        libraryList.getItems().addAll(songs);

        libraryList.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent click) {

                if (click.getClickCount() == 2) {
                    songNumber = libraryList.getSelectionModel().getSelectedIndex();
                    try {
                        changeSong();
                    } catch (InvalidDataException | UnsupportedTagException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public void updateTitle() throws InvalidDataException, UnsupportedTagException, IOException {
        media = new Media(songs.get(songNumber).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        Mp3File mp3file = new Mp3File(songs.get(songNumber));

        if (mp3file.hasId3v2Tag()) {
            ID3v2 id3v2Tag = mp3file.getId3v2Tag();
            artistLabel.setText(id3v2Tag.getArtist());
            albumLabel.setText(id3v2Tag.getAlbum());
            titleLabel.setText(id3v2Tag.getTitle());
        } else {
            artistLabel.setText("unknown");
            albumLabel.setText("unknown");
            titleLabel.setText("unknown");
        }

    }

    public void changeSong() throws InvalidDataException, UnsupportedTagException, IOException {
        mediaPlayer.stop();
        updateTitle();
        if (timerRunning) {
            cancelTimer();
        }
        songProgressBar.setProgress(0);
        mediaPlayer.stop();

        if (running) {
            mediaPlayer.play();
        }

        if (!libraryList.getItems().isEmpty()) {
            libraryList.getSelectionModel().select(songNumber);
        }
    }

    public void toggleMedia() {
        if (running) {
            cancelTimer();
            mediaPlayer.pause();
            playButton.setText("Play");
        } else {
            beginTimer();
            mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);
            mediaPlayer.play();
            playButton.setText("Pause");
        }
        running = !running;
    }

    public void resetMedia() {
        songProgressBar.setProgress(0);
        mediaPlayer.seek(Duration.seconds(0));
    }

    public void nextMedia() throws InvalidDataException, UnsupportedTagException, IOException {
        if (songNumber < songs.size() - 1) {
            songNumber++;
        } else {
            songNumber = 0;
        }
        changeSong();
    }
    public void previousMedia() throws InvalidDataException, UnsupportedTagException, IOException {
        if (songNumber > 0) {
            songNumber--;
        } else {
            songNumber = songs.size() - 1;
        }
        changeSong();
    }

    public void beginTimer() {
        timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                timerRunning = true;
                double current = mediaPlayer.getCurrentTime().toSeconds();
                double end = media.getDuration().toSeconds();

                songProgressBar.setProgress(current / end);

                if (current / end == 1) {
                    cancelTimer();
                }
            }
        };

        timer.scheduleAtFixedRate(task, 1000, 1000);
    }

    public void cancelTimer() {
        timerRunning = false;
        timer.cancel();
    }

    public void openDirectoryChooser() {
        final DirectoryChooser dirChooser = new DirectoryChooser();

        Stage stage = (Stage) pane.getScene().getWindow();

        File file = dirChooser.showDialog(stage);

        path = file.getAbsolutePath();

        updateDirectory();
    }

    public void skipForward() {
        Duration current = mediaPlayer.getCurrentTime();
        Duration end = media.getDuration();
        if (current.toSeconds() < end.toSeconds() - 5) {
            mediaPlayer.seek(current.add(Duration.seconds(5)));
            songProgressBar.setProgress(current.toSeconds() / end.toSeconds());
        }
    }

    public void skipBackward() {
        Duration current = mediaPlayer.getCurrentTime();
        Duration end = media.getDuration();
        if (current.toSeconds() > 5) {
            mediaPlayer.seek(current.subtract(Duration.seconds(5)));
            songProgressBar.setProgress(current.toSeconds() / end.toSeconds());
        }
    }

    public void shuffleMedia() throws InvalidDataException, UnsupportedTagException, IOException {
        Collections.shuffle(songs);
        beginTimer();
        libraryList.getItems().clear();
        libraryList.getItems().addAll(songs);
        changeSong();
    }

    public void savePlaylist() {
        try {
            FileWriter myWriter = new FileWriter("savedDir.txt");
            myWriter.write(path);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}