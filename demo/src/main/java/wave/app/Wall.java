package wave.app;

//import wave.app.WallType;
import javafx.scene.paint.Color;

public class Wall {
    double x1, y1, x2, y2;
    double normalX, normalY;
    double length;
    WallType type;
    double customReflection;  // For custom adjustments
    double customTransmission; // For custom adjustments
    
    Wall(double x1, double y1, double x2, double y2, WallType type) {
        this(x1, y1, x2, y2, type, type.getDefaultReflection(), type.getDefaultTransmission());
    }
    
    Wall(double x1, double y1, double x2, double y2, WallType type, 
         double customReflection, double customTransmission) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.type = type;
        this.customReflection = customReflection;
        this.customTransmission = customTransmission;
        
        // Calculate wall vector and normal
        double dx = x2 - x1;
        double dy = y2 - y1;
        this.length = Math.sqrt(dx * dx + dy * dy);
        
        // Normal vector (perpendicular to wall)
        this.normalX = -dy / length;
        this.normalY = dx / length;
    }
    
    public double getReflectionCoeff() {
        return customReflection;
    }
    
    public double getTransmissionCoeff() {
        return customTransmission;
    }
    
    public Color getColor() {
        return type.getColor();
    }
}
