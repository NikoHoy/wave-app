package wave.app;

import javafx.scene.paint.Color;

public enum WallType {
    SOLID("drywall", 0.95, 0.3, Color.WHITE),
    CONCRETE("concrete", 0.97, 0.1, Color.DARKGRAY),
    BRICK("Brick", 0.97, 0.1, Color.RED),
    ABSORBER("Absorber", 0.1, 0.0, Color.DARKGREEN),
    WOOD("Wood", 0.95, 0.2, Color.BROWN),
    CUSTOM("Custom", 0.5, 0.5, Color.MAGENTA);
    
    private final String displayName;
    private final double defaultReflection;
    private final double defaultTransmission;
    private final Color color;
    
    WallType(String displayName, double reflection, double transmission, Color color) {
        this.displayName = displayName;
        this.defaultReflection = reflection;
        this.defaultTransmission = transmission;
        this.color = color;
    }
    
    public String getDisplayName() { return displayName; }
    public double getDefaultReflection() { return defaultReflection; }
    public double getDefaultTransmission() { return defaultTransmission; }
    public Color getColor() { return color; }
}