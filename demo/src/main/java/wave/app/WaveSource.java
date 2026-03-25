package wave.app;

public class WaveSource {
        double x, y;
        int emitRate = 10; // Emit wave every 5 frames
        int frameCounter = 0;
        int emitAngle=0;
        double amplitude=1;
        
        WaveSource(double x, double y, int emitAngle, double amplitude) {
            this.x = x;
            this.y = y;
            this.emitAngle=emitAngle;
            this.amplitude=amplitude;

        }
    }