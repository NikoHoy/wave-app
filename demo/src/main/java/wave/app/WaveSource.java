package wave.app;

public class WaveSource {
        double x, y;
        int emitRate = 10; // Emit wave every 5 frames
        int frameCounter = 0;
        
        WaveSource(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }