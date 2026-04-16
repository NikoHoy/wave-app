package wave.app;

import java.io.File;
import java.net.URL;

import javafx.scene.media.*;

public class MusicPlayer {
    private static MediaPlayer mediaPlayer;

    public static void playSong() {
        try{
            URL resource = MusicPlayer.class.getResource("/audio/vlog-hip-hop.mp3");
            //System.out.println(resource);

            if (resource != null) {
        //Media song = new Media(new File("demo/src/main/resources/audio/vlog-hip-hop.mp3").toURI().toString());
        Media song = new Media(resource.toString());
        mediaPlayer = new MediaPlayer(song);
        mediaPlayer.play();
        } else {
            System.err.println("Audio file not found: /audio/vlog-hip-hop.mp3");
        }
        } catch (Exception e){
            System.err.println("Error playing song: vlog-hip-hop.mp3 " + e.getMessage());
        }
    }

    public static void setVolume(double volume) {
        mediaPlayer.setVolume(volume);
    }

    public static void stopSong() {
        mediaPlayer.stop();
    }

}
