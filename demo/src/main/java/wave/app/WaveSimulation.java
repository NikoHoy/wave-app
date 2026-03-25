package wave.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.animation.AnimationTimer;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WaveSimulation extends Application {

    private Pane mapPane;
    private Canvas gridCanvas;
    private List<WaveSource> sources = new ArrayList<>();
    private ConcurrentLinkedQueue<WaveFront> waveFronts = new ConcurrentLinkedQueue<>();
    private List<WaveFront> wavesToRemove = new ArrayList<>();
    private List<Wall> walls = new ArrayList<>();
    private Stack<Object> addedElementsStack = new Stack<>();
    private int gridSize = 30;
    // Wave parameters
    private double waveSpeed = 2.0;
    // private double reflectionCoeff = 0.7; // 70% reflects
    // private double transmissionCoeff = 0.3; // 30% passes through

    // Wall drawing mode
    private boolean wallDrawingMode = false;
    private boolean canYouAddDots = false;
    private boolean isMusicPlayerOn = false;

    // wall type stuff, can be moved to a better place i think but im checking if it
    // works first lmao
    private WallType currentWallType = WallType.DRYWALL;
    private ComboBox<WallType> wallTypeCombo;
    private Slider customReflectionSlider;
    private Slider customTransmissionSlider;
    private Label customValuesLabel;
    private int currentEmitAngle;
    private double chosenAmplitude=1;

    private ImageView iv = new ImageView();

    @Override
    public void start(Stage primaryStage) {
        // Main map pane
        mapPane = new Pane();
        mapPane.setStyle("-fx-background-color: #1a1a1a;");
        mapPane.setPrefSize(800, 600);

        drawGrid();

        // Create some sample walls
        createSampleWalls();

        // Create control panel
        VBox rightPanel = new VBox();

        VBox controls = createControls();
        VBox soundControls = createWaveControls();

        VBox wallControlPanel = createWallPanel();

        Tab wallControls = new Tab("Wall options", wallControlPanel);
        wallControls.setClosable(false);

        TabPane tabs = new TabPane();
        tabs.getTabs().add(wallControls);

        Tab waveSourceTab = new Tab("Wave source options", soundControls);
        waveSourceTab.setClosable(false);
        tabs.getTabs().add(waveSourceTab);

        // Loading and playing audio
        // MusicPlayer.playSong();
        // MusicPlayer.setVolume(0.4);

        // Mouse click to add new wave source (when not in wall mode)
        mapPane.setOnMouseClicked(this::handleMapClick);

        // Layout
        BorderPane root = new BorderPane();
        root.setCenter(mapPane);

        rightPanel.getChildren().addAll(tabs, controls);
        root.setRight(rightPanel);
        // Animation loop
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateWaves();
                renderWaves();
            }
        };
        timer.start();

        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setTitle("Wave Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createWaveControls() {
        VBox waveControls = new VBox(10);
        waveControls.setStyle("-fx-padding: 20; -fx-background-color: #333;");
        waveControls.setPrefWidth(280);

        Label title = new Label("Speaker direction");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        // Image arrowImage = new
        // Image("demo/src/main/java/wave/app/red-sticker-arrow-4.png");
        Image arrowImage = new Image(getClass().getResourceAsStream("red-sticker-arrow-4.png"), 100, 100, true, true);
        ImageView imageView = new ImageView(arrowImage);

        Slider rotationSlider = new Slider(0, 72, 0);
        rotationSlider.setShowTickLabels(true);
        rotationSlider.setShowTickMarks(true);
        imageView.rotateProperty().bind(rotationSlider.valueProperty().multiply(5));
        rotationSlider.valueProperty().addListener((obs, old, val) -> {
            currentEmitAngle = val.intValue();
        });

        Label speakerLabel = new Label("Speaker amplitude");
        speakerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        VBox speakertypes= new VBox(10);
        Button btn1=new Button("speaker 0.3");
        Button btn2=new Button("speaker 1");
        Button btn3=new Button("speaker 2.0");
        btn1.setOnAction(e -> {
            chosenAmplitude=0.3;
        });
        btn2.setOnAction(e -> {
            chosenAmplitude=1;
        });
        btn3.setOnAction(e -> {
            chosenAmplitude=2;
        });


        speakertypes.getChildren().addAll(btn1,btn2,btn3);

        waveControls.getChildren().addAll(title, rotationSlider, imageView,speakerLabel, speakertypes);




        return waveControls;
    }

    private VBox createWallPanel() {

        VBox controls = new VBox(10);

        controls.setStyle("-fx-padding: 20; -fx-background-color: #333;");
        controls.setPrefWidth(280); // Slightly wider for new controls

        // Title
        Label title = new Label("Wall options");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        Label wallTypeLabel = new Label("Wall Type:");
        wallTypeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        wallTypeCombo = new ComboBox<>();
        wallTypeCombo.getItems().addAll(WallType.values());
        wallTypeCombo.setValue(WallType.DRYWALL);
        wallTypeCombo.setMaxWidth(Double.MAX_VALUE);
        wallTypeCombo.setStyle("-fx-background-color: #444; -fx-text-fill: white;");

        // Custom wall properties (initially hidden)
        customValuesLabel = new Label("Custom Properties:");
        customValuesLabel.setStyle("-fx-text-fill: white;");
        customValuesLabel.setVisible(false);

        customReflectionSlider = new Slider(0, 1.0, 0.5);
        customReflectionSlider.setShowTickLabels(true);
        customReflectionSlider.setShowTickMarks(true);
        customReflectionSlider.setVisible(false);

        Label reflectValueLabel = new Label("Reflection: 0.5");
        reflectValueLabel.setStyle("-fx-text-fill: white;");
        reflectValueLabel.setVisible(false);
        customReflectionSlider.valueProperty().addListener((obs, old, val) -> {
            reflectValueLabel.setText(String.format("Reflection: %.2f", val.doubleValue()));
        });

        customTransmissionSlider = new Slider(0, 1.0, 0.5);
        customTransmissionSlider.setShowTickLabels(true);
        customTransmissionSlider.setShowTickMarks(true);
        customTransmissionSlider.setVisible(false);

        Label transmitValueLabel = new Label("Transmission: 0.5");
        transmitValueLabel.setStyle("-fx-text-fill: white;");
        transmitValueLabel.setVisible(false);
        customTransmissionSlider.valueProperty().addListener((obs, old, val) -> {
            transmitValueLabel.setText(String.format("Transmission: %.2f", val.doubleValue()));
        });

        // Show/hide custom controls based on selection
        wallTypeCombo.setOnAction(e -> {
            currentWallType = wallTypeCombo.getValue();
            boolean isCustom = currentWallType == WallType.CUSTOM;
            customValuesLabel.setVisible(isCustom);
            customReflectionSlider.setVisible(isCustom);
            customTransmissionSlider.setVisible(isCustom);
            reflectValueLabel.setVisible(isCustom);
            transmitValueLabel.setVisible(isCustom);
        });

        controls.getChildren().addAll(
                title,
                new Label(" "),
                new Label(" "),
                wallTypeLabel, wallTypeCombo,
                customValuesLabel,
                reflectValueLabel, customReflectionSlider,
                transmitValueLabel, customTransmissionSlider,
                new Label(" "));

        return controls;
    }

    private VBox createControls() {
        VBox controls = new VBox(10);
        controls.setStyle("-fx-padding: 20;   -fx-background-color: #333;");
        controls.setPrefWidth(280); // Slightly wider for new controls

        // Wave speed slider (existing code)
        Label speedLabel = new Label("Wave Speed: 2.0");
        speedLabel.setStyle("-fx-text-fill: white;");
        Slider speedSlider = new Slider(0.5, 5.0, 2.0);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.valueProperty().addListener((obs, old, val) -> {
            waveSpeed = val.doubleValue();
            speedLabel.setText(String.format("Wave Speed: %.1f", waveSpeed));
        });

        // initialize buttons wallmode and dot mode
        Button wallModeBtn = new Button("Add Wall mode");
        Button addDotsButton = new Button("Add wave source");

        // Wall mode toggle button functionality (updated)
        wallModeBtn.setMaxWidth(Double.MAX_VALUE);
        wallModeBtn.setOnAction(e -> {
            wallDrawingMode = !wallDrawingMode;
            if (wallDrawingMode) {
                if (canYouAddDots) {
                    addDotsButton.fire();
                }
                wallModeBtn.setText("Exit Wall Mode");
                mapPane.setStyle("-fx-background-color: #1a1a1a; -fx-cursor: CROSSHAIR;");
                enableWallDrawing();
            } else {
                wallModeBtn.setText("Enter Wall Mode");
                mapPane.setStyle("-fx-background-color: #1a1a1a; -fx-cursor: DEFAULT;");
                mapPane.setOnMousePressed(null);
                mapPane.setOnMouseDragged(null);
                mapPane.setOnMouseReleased(null);
                mapPane.setOnMouseClicked(this::handleMapClick);
            }
        });

        // add dots button functionality
        addDotsButton.setMaxWidth(Double.MAX_VALUE);
        addDotsButton.setOnAction(e -> {
            canYouAddDots = !canYouAddDots;
            if (canYouAddDots) {
                if (wallDrawingMode) {
                    wallModeBtn.fire();
                }
                addDotsButton.setText("Exit wave source mode");
                mapPane.setOnMouseClicked(this::handleMapClick);
            } else {
                addDotsButton.setText("Enter wave source mode");
            }
        });

        // Clear all button
        Button clearBtn = new Button("Clear All Waves");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> {
            waveFronts.clear();
            Platform.runLater(() -> {
                mapPane.getChildren()
                        .removeIf(node -> node instanceof Circle && ((Circle) node).getFill() == Color.TRANSPARENT);
            });
        });

        // nyt tuo poistaa aina äänilähteet ekana ja sitte vasta seinät
        Button undoBtn = new Button("undo last element");
        undoBtn.setMaxWidth(Double.MAX_VALUE);
        undoBtn.setOnAction(e -> {
            System.out.println(sources);
            System.out.println(walls);

            if (sources.size() > 0) {
                sources.remove(sources.size() - 1);
                waveFronts.clear();
                mapPane.getChildren().removeLast();
                redrawAll();
                return;
            }
            if (walls.size() > 0) {
                walls.remove(walls.size() - 1);
                mapPane.getChildren().removeLast();
                mapPane.getChildren().removeLast();
                redrawAll();
                return;
            }

            mapPane.getChildren().clear();
            drawGrid();

        });

        // Reset button
        Button resetBtn = new Button("Reset Simulation");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> {
            waveFronts.clear();
            sources.clear();
            walls.clear();
            Platform.runLater(() -> {
                mapPane.getChildren().clear();
                drawGrid();
                createSampleWalls();
            });
        });

        // Full reset button
        Button fullReset = new Button("Remove all elements");
        fullReset.setMaxWidth(Double.MAX_VALUE);
        fullReset.setOnAction(e -> {
            waveFronts.clear();
            sources.clear();
            walls.clear();
            Platform.runLater(() -> {
                mapPane.getChildren().clear();
                drawGrid();
            });
        });

        Button checkMusicLoudness = new Button("Check volume level");
        checkMusicLoudness.setMaxWidth(Double.MAX_VALUE);
        checkMusicLoudness.setOnAction(e -> {
            isMusicPlayerOn = !isMusicPlayerOn;
            if (isMusicPlayerOn) {
                MusicPlayer.playSong();
                MusicPlayer.setVolume(0.4);
            }
            if (!isMusicPlayerOn) {
                MusicPlayer.stopSong();
            }
            mapPane.setOnMouseClicked(event -> {
                double clickX = event.getX();
                double clickY = event.getY();
                double volume = volumeClick(clickX, clickY);
                // System.out.println(volume);
                Image userImage = new Image(getClass().getResourceAsStream("user.jpg"), 20, 20, true, true);
                iv.setImage(userImage);

                iv.setLayoutX(clickX);
                iv.setLayoutY(clickY);
                if(!mapPane.getChildren().contains(iv)){
                    mapPane.getChildren().add(iv);
                }

                MusicPlayer.setVolume(volume);
                //System.out.println(volume);
            }

            );
        });

        controls.getChildren().addAll(
                new Label(" "),
                speedLabel, speedSlider,
                new Label(" "),
                // wallTypeLabel, wallTypeCombo,
                // customValuesLabel,
                // reflectValueLabel, customReflectionSlider,
                // transmitValueLabel, customTransmissionSlider,
                new Label(" "),
                wallModeBtn,
                addDotsButton,
                checkMusicLoudness,
                clearBtn,
                resetBtn,
                fullReset,
                undoBtn);

        return controls;
    }

    private double volumeClick(double clickX, double clickY) {
        WaveFront closestWave = getClosestWave(clickX, clickY);
        double amplitude = 0;

        if (closestWave != null) {
            amplitude = closestWave.amplitude;
        }
        return amplitude;
    }

    private WaveFront getClosestWave(double x, double y) {
        WaveFront closest = null;
        double minDistance = 300;
        double radius = 20;

        for (WaveFront wave : waveFronts) {
            double dx = wave.x - x;
            double dy = wave.y - y;
            double distanceSq = dx * dx + dy * dy;

            if(distanceSq <= radius * radius){
                if(closest == null || wave.amplitude > closest.amplitude){
                    closest = wave;
                    minDistance = distanceSq;
                }
            }


            // if (distanceSq < minDistance) {
            //     minDistance = distanceSq;
            //     closest = wave;
            // }
        }
        return closest;
    }

    private void redrawAll() {
        redrawGrid();
        for (Object element : addedElementsStack) {
            if (!addedElementsStack.isEmpty()) {
                if (element.getClass().toString().equals("WaveSource")) {
                    WaveSource addSource = (WaveSource) element;
                    addWaveSource(addSource.x, addSource.y, Color.GREEN);
                } else if (element.getClass().toString().equals("Wall")) {
                    Wall addWall = (Wall) element;
                    addWall(addWall.x1, addWall.y1, addWall.x2, addWall.y2, addWall.type);
                }

                Platform.runLater(() -> {

                });
            }
        }

    }

    private void drawGrid() {
        // Create canvas once during setup
        gridCanvas = new Canvas();
        mapPane.getChildren().add(gridCanvas);

        // Bind size
        gridCanvas.widthProperty().bind(mapPane.widthProperty());
        gridCanvas.heightProperty().bind(mapPane.heightProperty());

        // Add resize listeners
        gridCanvas.widthProperty().addListener((obs, oldVal, newVal) -> redrawGrid());
        gridCanvas.heightProperty().addListener((obs, oldVal, newVal) -> redrawGrid());

        // Initial draw
        redrawGrid();
    }

    private void redrawGrid() {
        if (gridCanvas == null)
            return;

        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.1);

        double width = gridCanvas.getWidth();
        double height = gridCanvas.getHeight();

        for (int x = 0; x <= width; x += gridSize) {
            gc.strokeLine(x, 0, x, height);
        }
        for (int y = 0; y <= height; y += gridSize) {
            gc.strokeLine(0, y, width, y);
        }
    }

    private void enableWallDrawing() {
        final double[] startPoint = new double[2];

        mapPane.setOnMousePressed(e -> {
            startPoint[0] = Math.round((float) e.getX() / gridSize) * gridSize;
            startPoint[1] = Math.round((float) e.getY() / gridSize) * gridSize;
        });

        mapPane.setOnMouseReleased(e -> {
            WallType selectedType = wallTypeCombo.getValue();

            if (selectedType == WallType.CUSTOM) {
                // Use custom slider values
                addWall(startPoint[0], startPoint[1], Math.round((float) e.getX() / gridSize) * gridSize,
                        Math.round((float) e.getY() / gridSize) * gridSize,
                        selectedType,
                        customReflectionSlider.getValue(),
                        customTransmissionSlider.getValue());
            } else {
                // Use default values for the selected type
                addWall(startPoint[0], startPoint[1], Math.round((float) e.getX() / gridSize) * gridSize,
                        Math.round((float) e.getY() / gridSize) * gridSize, selectedType);
            }
        });
    }

    // Overloaded methods for adding walls
    private void addWall(double x1, double y1, double x2, double y2, WallType type) {
        Wall wall = new Wall(x1, y1, x2, y2, type);
        walls.add(wall);
        // addedElementsStack.add(wall);

        // Visual representation with type-specific color
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(type.getColor());
        line.setStrokeWidth(3);

        // Add a label to show wall type (optional)
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;
        Label typeLabel = new Label(type.getDisplayName().substring(0, 1)); // First letter
        typeLabel.setTextFill(Color.WHITE);
        typeLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold;");
        typeLabel.setLayoutX(midX - 5);
        typeLabel.setLayoutY(midY - 10);

        Platform.runLater(() -> {
            mapPane.getChildren().addAll(line, typeLabel);
        });
    }

    private void addWall(double x1, double y1, double x2, double y2,
            WallType type, double reflection, double transmission) {
        Wall wall = new Wall(x1, y1, x2, y2, type, reflection, transmission);
        walls.add(wall);
        // addedElementsStack.add(wall);

        // Visual representation
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(type.getColor());
        line.setStrokeWidth(3);

        // Show custom values on wall
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;
        Label valueLabel = new Label(String.format("R:%.1f T:%.1f", reflection, transmission));
        valueLabel.setTextFill(Color.WHITE);
        valueLabel.setStyle("-fx-font-size: 8;");
        valueLabel.setLayoutX(midX - 15);
        valueLabel.setLayoutY(midY - 10);

        Platform.runLater(() -> {
            mapPane.getChildren().addAll(line, valueLabel);
        });
    }

    private void createSampleWalls() {
        // Create different types of walls
        addWall(200, 100, 200, 500, WallType.DRYWALL); // Solid wall (white)
        addWall(600, 100, 600, 500, WallType.CONCRETE); // Glass (light blue)
        addWall(100, 300, 700, 300, WallType.BRICK); // Water (cyan)
        addWall(400, 200, 400, 400, WallType.WOOD); // Mirror (yellow)
        addWall(100, 500, 300, 300, WallType.ABSORBER); // Absorber (dark gray)

        // Add a custom wall with specific values
        addWall(500, 500, 700, 500, WallType.CUSTOM, 0.4, 0.6);

        // Add some wave sources
        addWaveSource(300, 250, Color.RED);
        addWaveSource(500, 350, Color.BLUE);
    }

    private void addWaveSource(double x, double y, Color color) {
        WaveSource source = new WaveSource(x, y, currentEmitAngle, chosenAmplitude);
        sources.add(source);
        // addedElementsStack.add(source);

        // Visual dot
        Circle dot = new Circle(x, y, 8);
        dot.setFill(color);
        dot.setStroke(Color.WHITE);
        dot.setStrokeWidth(2);
        dot.setViewOrder(-1);

        // Make draggable
        makeDraggable(dot, source);

        Platform.runLater(() -> mapPane.getChildren().add(dot));
    }

    private void makeDraggable(Circle dot, WaveSource source) {
        final double[] dragDelta = new double[2];

        dot.setOnMousePressed(e -> {
            dragDelta[0] = dot.getCenterX() - e.getX();
            dragDelta[1] = dot.getCenterY() - e.getY();
        });

        dot.setOnMouseDragged(e -> {
            dot.setCenterX(e.getX() + dragDelta[0]);
            dot.setCenterY(e.getY() + dragDelta[1]);
            source.x = dot.getCenterX();
            source.y = dot.getCenterY();
        });
    }

    private void handleMapClick(javafx.scene.input.MouseEvent e) {
        if (canYouAddDots && !wallDrawingMode) {
            addWaveSource(e.getX(), e.getY(), Color.GREEN);
        }
    }

    private void updateWaves() {
        // Create new wave fronts from sources

        for (WaveSource source : sources) {
            source.frameCounter++;
            if (source.frameCounter >= source.emitRate) {
                source.frameCounter = 0;

                // Emit waves in multiple directions for more realistic effect
                // was this: for (int i = 0; i < 36; i++) {
                for (int i = source.emitAngle - 12; i < source.emitAngle + 12; i++) {
                    double angle = (i * 5) * Math.PI / 180; // was: double angle = (i * 10) * Math.PI / 180;
                    // Directly add to concurrent queue - safe!
                    waveFronts.add(new WaveFront(
                            source.x, source.y, angle, source.amplitude, 0));
                }
            }
        }

        // Clear removal list
        wavesToRemove.clear();

        // Update existing wave fronts
        for (WaveFront wave : waveFronts) {
            // Store previous position for collision detection
            double prevX = wave.x;
            double prevY = wave.y;

            // Move wave
            double dx = Math.cos(wave.angle) * waveSpeed;
            double dy = Math.sin(wave.angle) * waveSpeed;
            wave.x += dx;
            wave.y += dy;
            wave.age++;
            wave.amplitude *= 0.99; // Natural decay

            // Check wall collisions
            boolean collided = false;
            for (Wall wall : walls) {
                if (checkCollision(prevX, prevY, wave.x, wave.y, wall)) {
                    // Handle collision - this will ADD new waves to the queue
                    handleCollision(wave, wall);
                    collided = true;
                    break;
                }
            }

            // Mark for removal if collided or too old
            if (collided || wave.age > 200 || wave.amplitude < 0.05) {
                wavesToRemove.add(wave);
            }
        }

        // Remove all marked waves
        waveFronts.removeAll(wavesToRemove);
    }

    private boolean checkCollision(double x1, double y1, double x2, double y2, Wall wall) {
        return lineIntersection(x1, y1, x2, y2,
                wall.x1, wall.y1, wall.x2, wall.y2) != null;
    }

    private double[] lineIntersection(double x1, double y1, double x2, double y2,
            double x3, double y3, double x4, double y4) {
        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (denom == 0)
            return null;

        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;

        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            return new double[] { x1 + t * (x2 - x1), y1 + t * (y2 - y1) };
        }
        return null;
    }

    private void handleCollision(WaveFront wave, Wall wall) {
        // Calculate direction vector
        double dx = Math.cos(wave.angle);
        double dy = Math.sin(wave.angle);

        // Calculate reflection angle
        double nx = wall.normalX;
        double ny = wall.normalY;

        // Dot product
        double dot = dx * nx + dy * ny;

        // Reflection vector: R = V - 2*(V·N)*N
        double reflectX = dx - 2 * dot * nx;
        double reflectY = dy - 2 * dot * ny;
        double reflectAngle = Math.atan2(reflectY, reflectX);

        // Use wall-specific coefficients!
        double wallReflection = wall.getReflectionCoeff();
        double wallTransmission = wall.getTransmissionCoeff();

        double offset = 3.01;

        // Create reflected wave
        if (wallReflection > 0 && wave.generation < 5) {
            waveFronts.add(new WaveFront(
                    wave.x + reflectX * offset,
                    wave.y + reflectY * offset,
                    reflectAngle,
                    wave.amplitude * wallReflection,
                    wave.generation + 1));
        }

        // Create transmitted wave
        if (wallTransmission > 0) {
            waveFronts.add(new WaveFront(
                    wave.x + dx * offset,
                    wave.y + dy * offset,
                    wave.angle,
                    wave.amplitude * wallTransmission,
                    wave.generation + 1));
        }
    }

    private void renderWaves() {
        // This runs on the animation thread, so we need Platform.runLater for UI
        // updates
        Platform.runLater(() -> {
            // Remove old wave circles
            mapPane.getChildren()
                    .removeIf(node -> node instanceof Circle && ((Circle) node).getFill() == Color.TRANSPARENT);

            // Draw current wave fronts
            for (WaveFront wave : waveFronts) {
                Circle circle = new Circle(wave.x, wave.y, 3);
                circle.setFill(Color.TRANSPARENT);

                // Color based on amplitude and generation
                Color color = Color.CYAN.deriveColor(
                        0, 1, 1,
                        Math.min(1, wave.amplitude));

                if (wave.generation == 1)
                    color = Color.YELLOW.deriveColor(0, 1, 1, wave.amplitude);
                if (wave.generation == 2)
                    color = Color.ORANGE.deriveColor(0, 1, 1, wave.amplitude);
                if (wave.generation >= 3)
                    color = Color.RED.deriveColor(0, 1, 1, wave.amplitude);

                circle.setStroke(color);
                circle.setStrokeWidth(1.5);

                mapPane.getChildren().add(circle);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}