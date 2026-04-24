package wave.app;

public class WaveSource {
        double x, y;
        int emitRate = 10; // Emit wave every 5 frames
        int frameCounter = 0;
        int emitAngle=0;
        double amplitude=1;
        double bassAmp=1;
        String name;
        
        WaveSource(double x, double y, int emitAngle, double amplitude, double bassAmp) {
            this.x = x;
            this.y = y;
            this.emitAngle=emitAngle;
            this.amplitude=amplitude;

        }

        WaveSource(double x, double y, int emitAngle, double amplitude, double bassAmp, String name) {
            this.x = x;
            this.y = y;
            this.emitAngle=emitAngle;
            this.amplitude=amplitude;
            this.name = name;
        }

        public void setAmplitude(double amplitude) {
            this.amplitude = amplitude;
        }
    }