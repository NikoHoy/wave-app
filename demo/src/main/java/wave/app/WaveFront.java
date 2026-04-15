package wave.app;

public class WaveFront {
        double x, y;
        double angle;
        int age = 0;
        double amplitude;
        int generation; // Track number of reflections
        double bassAmp;
        
        WaveFront(double x, double y, double angle, double amplitude, int generation, double bassAmp) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.amplitude = amplitude;
            this.generation = generation;
            this.bassAmp=bassAmp;
        }
    }
