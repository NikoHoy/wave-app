package wave.app;

import java.io.File;

import javafx.scene.media.*;

public class MusicPlayer {
    private static MediaPlayer mediaPlayer;

    public static void playSong(){
    Media song = new Media(new File("demo/src/main/java/wave/app/vlog-hip-hop.mp3").toURI().toString());
    mediaPlayer = new MediaPlayer(song);
    mediaPlayer.play();
    }

    public static void setVolume(double volume){
        mediaPlayer.setVolume(volume);
    }

    public static void stopSong(){
        mediaPlayer.stop();
    }

    public void addUserPicture(){
        
    }
    
}
