package wave.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.animation.AnimationTimer;
import javafx.stage.Stage;

import java.io.InputStream;
import java.text.DecimalFormat;
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


    // Wall drawing mode
    private boolean wallDrawingMode = false;
    private boolean canYouAddDots = false;
    private boolean isMusicPlayerOn = false;

    // wall type stuff, can be moved to a better place i think but im checking if it
    // works first
    private WallType currentWallType = WallType.DRYWALL;
    private ComboBox<WallType> wallTypeCombo;
    private Slider customReflectionSlider;
    private Slider customTransmissionSlider;
    private Label customValuesLabel;
    private int currentEmitAngle;
    private double chosenAmplitude = 0.5;
    private double chosenBass = 0.7;
    private boolean bassToggled = false;

    private ImageView iv = new ImageView();
    private Double[] speakerValues = new Double[3];
    private Label ampLabel = new Label();
    private Label bassAmpLabel = new Label();
    private Label emitAngleLabel = new Label();
    VBox speakerInfo = new VBox(10);
    private int sourceCount=0;
    private Label countLabel=new Label();
    private Slider rightAngleSlider= new Slider(0,72,0);
    private Slider ampSlider = new Slider(0, 1, 0);
    private Slider bassSlider = new Slider(0, 1, 0);

    private DecimalFormat rightLabelFormat= new DecimalFormat("#.##");
    private String buttonStyle =
            "-fx-background-color: white;" +
            "-fx-text-fill: black;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 6 12;" +
            "-fx-font-size: 14px;";


    private WaveSource selectedSource;

    TitledPane speakerOptions = new TitledPane();

    @Override
    public void start(Stage primaryStage) {
        StackPane startScreen = new StackPane();

        VBox main = new VBox(20);
        main.setAlignment(Pos.CENTER);

        Label welcometxt = new Label("Welcome!");

        welcometxt.setFont(Font.font("Segoe Script", 70));
        welcometxt.setStyle("-fx-font-weight: bold;");

        Button openSimulationBtn = new Button("Create New Space");
        openSimulationBtn.setOnAction(e -> {
            createSimulation(primaryStage);
        });
        openSimulationBtn.setPrefHeight(50);
        openSimulationBtn.setMaxWidth(250);

        Button exampleBtn = new Button("Old Space");
        exampleBtn.setOnAction(e -> {
            createSimulation(primaryStage);
            createSampleWalls();
        });
        exampleBtn.setPrefHeight(50);
        exampleBtn.setMaxWidth(250);

        main.getChildren().addAll(welcometxt, openSimulationBtn, exampleBtn);

        startScreen.getChildren().addAll(main);

        Scene startScene = new Scene(startScreen, 1000, 800);

        primaryStage.setTitle("Virtual Showroom");
        primaryStage.setScene(startScene);
        primaryStage.show();
    }

    private void createSimulation(Stage primaryStage) {
        // Main map pane
        mapPane = new Pane();
        mapPane.setStyle("-fx-background-color: #1a1a1a;");
        mapPane.setPrefSize(800, 600);

        drawGrid();

        // Create control panel
        VBox leftPanel = new VBox();
        leftPanel.setStyle("-fx-spacing: 40;-fx-padding: 10;");

        HBox controls = createControls();
        VBox soundControls = createWaveControls();

        Button addDotsButton = new Button("Add wave source");
        Button wallModeBtn = new Button("Add Wall");

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
        VBox wallControlPanel = createWallPanel();

        wallModeBtn.setStyle(buttonStyle);
        wallControlPanel.getChildren().add(wallModeBtn);

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

        addDotsButton.setStyle(buttonStyle);
        soundControls.getChildren().add(addDotsButton);

        Tab wallControls = new Tab("Wall options", wallControlPanel);
        wallControls.setClosable(false);

        wallControls.setOnSelectionChanged(e -> {
            wallModeBtn.fire();
        });
        wallControlPanel.setOnMouseClicked(e -> {
            wallModeBtn.fire();
        });
        TitledPane sourceTittlePane = new TitledPane();
        TitledPane wallTitledPanel = new TitledPane();
        wallTitledPanel.setExpanded(false);
        wallTitledPanel.setText("+Wall");
        wallTitledPanel.setStyle("-fx-pref-tile-height: 100");

        wallTitledPanel.setOnMouseClicked(e -> {
            wallModeBtn.fire();
            if (sourceTittlePane.isExpanded()) {
                // addDotsButton.fire();
                sourceTittlePane.setExpanded(false);
            }

        });
        wallTitledPanel.setContent(wallControlPanel);

        Tab waveSourceTab = new Tab("Wave source options", soundControls);
        waveSourceTab.setClosable(false);
        waveSourceTab.setOnSelectionChanged(e -> {
            addDotsButton.fire();
        });

        sourceTittlePane.setExpanded(false);
        sourceTittlePane.setText("+Wave source");
        sourceTittlePane.setOnMouseClicked(e -> {
            addDotsButton.fire();
            if (wallTitledPanel.isExpanded()) {
                // wallModeBtn.fire();
                wallTitledPanel.setExpanded(false);
            }

        });
        sourceTittlePane.setContent(soundControls);

        leftPanel.getChildren().addAll(wallTitledPanel, sourceTittlePane);


        // rightPanel
        VBox rightPanel = new VBox();
        rightPanel.setStyle("-fx-spacing: 40;-fx-padding: 10");

        speakerOptions.setText("selected speaker");
        speakerOptions.setExpanded(false);
        VBox si = createSpeakerOptions();
        speakerOptions.setContent(si);

        ampSlider.setShowTickMarks(true);
        ampSlider.setShowTickLabels(true);
        ampSlider.valueProperty().addListener((obs, old, val) -> {
            if (selectedSource != null) {
                selectedSource.amplitude = val.doubleValue();
                ampLabel.setText("Speaker amplitude: " + val.intValue());
            }
        });


        bassSlider.setShowTickMarks(true);
        bassSlider.setShowTickLabels(true);
        bassSlider.valueProperty().addListener((obs, old, val) -> {
            if (selectedSource != null) {
                selectedSource.bassAmp = val.doubleValue();
                bassAmpLabel.setText("Speaker bass amplitude: " + val.intValue());
            }
        });


        // Layout
        BorderPane root = new BorderPane();
        root.setCenter(mapPane);
        root.setStyle("-fx-background-color: #333");

        rightPanel.getChildren().add(speakerOptions);

        root.setTop(controls);
        root.setLeft(leftPanel);
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

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Virtual Showroom");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSpeakerOptions() {
        speakerInfo.setStyle("-fx-padding: 20; -fx-background-color: #333;");
        speakerInfo.setPrefWidth(280);

        Label title = new Label("Speaker information: ");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        speakerInfo.getChildren().add(title);

        return speakerInfo;
    }

    //opens the right pane and shows speaker info
    //is used in the makeDraggable() method on line 928
    // I added String name to WaveSource so its possible to have Genelec G3 etc.
    private void onClick(Circle dot, WaveSource source) {
        speakerValues[0] = source.amplitude;
        speakerValues[1] = source.bassAmp;
        speakerValues[2] = (double) source.emitAngle;
        speakerOptions.setExpanded(true);

        selectedSource=source;

        ampSlider.setValue(source.amplitude);
        bassSlider.setValue(source.bassAmp);
        rightAngleSlider.setValue(source.emitAngle);

        ampLabel.setText("Speaker amplitude: " + rightLabelFormat.format(source.amplitude));
        bassAmpLabel.setText("Speaker bass amplitude: " + rightLabelFormat.format(source.bassAmp));
        emitAngleLabel.setText("Speaker emit angle: " + rightLabelFormat.format(source.emitAngle));
        ImageView imageView = null;
        countLabel.setText("count: "+ sourceCount);





        InputStream inputStream = getClass().getResourceAsStream("/images/red-sticker-arrow-4.png");
        if (inputStream != null) {
            Image arrowImage = new Image(inputStream, 100, 100, true, true);
            imageView = new ImageView(arrowImage);
        } else {
            System.err.println("image not found: images/red-sticker-arrow-4.png");
        }

        rightAngleSlider.setShowTickLabels(true);
        rightAngleSlider.setShowTickMarks(true);
        imageView.rotateProperty().bind(rightAngleSlider.valueProperty().multiply(5));
        rightAngleSlider.valueProperty().addListener((obs, old, val) -> {
            if(selectedSource!=null){
                selectedSource.emitAngle = val.intValue();
            }

        });



        if (!speakerInfo.getChildren().contains(ampLabel)) {
            ampLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
            bassAmpLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
            emitAngleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
            countLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
            sourceCount++;
        }
        if (!speakerInfo.getChildren().contains(rightAngleSlider)) {
            speakerInfo.getChildren().addAll(ampLabel, ampSlider, bassAmpLabel, bassSlider, emitAngleLabel, rightAngleSlider);
        }
    }

    private VBox createWaveControls() {
        VBox waveControls = new VBox(10);
        waveControls.setStyle("-fx-padding: 20; -fx-background-color: #333;");
        waveControls.setPrefWidth(280);

        Label title = new Label("Speaker direction");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");


        ImageView imageView = null;
        InputStream inputStream = getClass().getResourceAsStream("/images/red-sticker-arrow-4.png");
        if (inputStream != null) {
            Image arrowImage = new Image(inputStream, 100, 100, true, true);
            imageView = new ImageView(arrowImage);
        } else {
            System.err.println("image not found: images/red-sticker-arrow-4.png");
        }

        Slider rotationSlider = new Slider(0, 72, 0);
        rotationSlider.setShowTickLabels(true);
        rotationSlider.setShowTickMarks(true);
        imageView.rotateProperty().bind(rotationSlider.valueProperty().multiply(5));
        rotationSlider.valueProperty().addListener((obs, old, val) -> {
            currentEmitAngle = val.intValue();
        });

        Label speakerLabel = new Label("Speaker amplitude");
        speakerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        VBox speakertypes = new VBox(10);
        Button btn1 = new Button("speaker 0.3");
        Button btn2 = new Button("speaker 0.5");
        Button btn3 = new Button("speaker 0.8");
        btn1.setOnAction(e -> {
            chosenAmplitude = 0.3;
            chosenBass = 0.5;
        });
        btn2.setOnAction(e -> {
            chosenAmplitude = 0.5;
            chosenBass = 0.7;
        });
        btn3.setOnAction(e -> {
            chosenAmplitude = 0.8;
            chosenBass = 1.0;
        });
        btn2.fire();

        btn1.setStyle(buttonStyle);
        btn2.setStyle(buttonStyle);
        btn3.setStyle(buttonStyle);


        speakertypes.getChildren().addAll(btn1, btn2, btn3);

        waveControls.getChildren().addAll(title, rotationSlider, imageView, speakerLabel, speakertypes);

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
        wallTypeCombo.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-font-weight: bold");

        // Custom wall properties
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

        Button wallModeBtn = new Button("Add Wall");
        wallModeBtn.setMaxWidth(Double.MAX_VALUE);
        wallModeBtn.setOnAction(e -> {
            wallDrawingMode = !wallDrawingMode;
            if (wallDrawingMode) {

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

        Button undoButton = new Button("Remove last element");
        undoButton.setMaxWidth(Double.MAX_VALUE);
        undoButton.setOnAction(e -> {
            // System.out.println(sources);
            // System.out.println(walls);

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


        undoButton.setStyle(buttonStyle);

        controls.getChildren().addAll(
                title,
                new Label(" "),
                new Label(" "),
                wallTypeLabel, wallTypeCombo,
                customValuesLabel,
                reflectValueLabel, customReflectionSlider,
                transmitValueLabel, customTransmissionSlider,
                undoButton,
                new Label(" "));

        return controls;
    }

    private HBox createControls() {


        HBox controls = new HBox(10);
        controls.setStyle("-fx-padding: 20;   -fx-background-color: #333");
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

        Button addDotsButton = new Button("Add wave source");


        // add dots button functionality
        addDotsButton.setMaxWidth(Double.MAX_VALUE);
        addDotsButton.setOnAction(e -> {
            canYouAddDots = !canYouAddDots;
            if (canYouAddDots) {
                if (wallDrawingMode) {
                    // wallModeBtn.fire();
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

        Button undoBtn = new Button("undo last element");
        undoBtn.setMaxWidth(Double.MAX_VALUE);
        undoBtn.setOnAction(e -> {
            // System.out.println(sources);
            // System.out.println(walls);

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
        Button fullReset = new Button("Remove all");
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

        Button checkMusicLoudness = new Button("Check volume");
        checkMusicLoudness.setMaxWidth(Double.MAX_VALUE);
        checkMusicLoudness.setOnAction(e -> {
            isMusicPlayerOn = !isMusicPlayerOn;
            if (isMusicPlayerOn) {
                MusicPlayer.playSong();
                MusicPlayer.setVolume(0.4);
                checkMusicLoudness.setText("Stop music");
            }
            if (!isMusicPlayerOn) {
                MusicPlayer.stopSong();
                if (mapPane.getChildren().contains(iv)) {
                    mapPane.getChildren().remove(iv);
                }
                checkMusicLoudness.setText("Check volume");
            }
            mapPane.setOnMouseClicked(event -> {
                if (isMusicPlayerOn) {
                    double clickX = event.getX();
                    double clickY = event.getY();
                    double volume = volumeClick(clickX, clickY);

                    InputStream is = getClass().getResourceAsStream("/images/user.jpg");

                    if (is != null) {
                        Image userImage = new Image(is, 20, 20, true, true);
                        iv.setImage(userImage);
                    } else {
                        System.err.println("image not found: images/user.jpg");
                    }

                    iv.setLayoutX(clickX - 10);
                    iv.setLayoutY(clickY - 10);
                    if (!mapPane.getChildren().contains(iv)) {
                        mapPane.getChildren().add(iv);
                    }

                    MusicPlayer.setVolume(volume);
                    // System.out.println(volume);
                }
            }

            );
        });
        // bass toggle, eri tajuksien katsomiseen. kokeilun vuoksi pelkkä basso.

        Button bassToggle = new Button("toggle bass");
        bassToggle.setMaxWidth(Double.MAX_VALUE);
        bassToggle.setOnAction(e -> {
            bassToggled = !bassToggled;

        });
        // nonfunctional placeholder save/load/exit
        Button save = new Button("Save");
        Button load = new Button("Load");

        load.setOnAction(e -> {
            waveFronts.clear();
            sources.clear();
            walls.clear();
            Platform.runLater(() -> {
                mapPane.getChildren().clear();
                drawGrid();
            });
            createSampleWalls();
        });

        Label nameLabel = new Label("Virtual showroom");

        nameLabel.setVisible(true);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold");

        addDotsButton.setStyle(buttonStyle);
        clearBtn.setStyle(buttonStyle);
        undoBtn.setStyle(buttonStyle);
        resetBtn.setStyle(buttonStyle);
        fullReset.setStyle(buttonStyle);
        checkMusicLoudness.setStyle(buttonStyle);
        bassToggle.setStyle(buttonStyle);
        save.setStyle(buttonStyle);
        load.setStyle(buttonStyle);


        Region spacer1 = new Region();
        spacer1.setPrefWidth(130);

        Region spacer2 = new Region();
        spacer2.setPrefWidth(40);


        controls.getChildren().addAll(
                nameLabel,
                spacer1,
                checkMusicLoudness,
                bassToggle,

                clearBtn,
                fullReset,
                undoBtn,
                spacer2,
                save, load);

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

            if (distanceSq <= radius * radius) {
                if (closest == null || wave.amplitude > closest.amplitude) {
                    closest = wave;
                    minDistance = distanceSq;
                }
            }

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
        addWall(120, 90, 120, 600, WallType.CONCRETE);
        addWall(120, 600, 450, 600, WallType.CONCRETE);
        addWall(450, 600, 450, 210, WallType.CONCRETE);
        addWall(450, 210, 450, 210, WallType.CONCRETE);
        addWall(450, 90, 450, 210, WallType.CONCRETE);
        addWall(450, 90, 120, 90, WallType.BRICK);

        addWall(240, 350, 450, 350, WallType.AUDIO_PANEL); // Absorber


        // Add some wave sources
        addWaveSourceWithAngle(130, 100, 12);
        addWaveSourceWithAngle(430, 500, 30);
    }

    private void addWaveSource(double x, double y, Color color) {
        WaveSource source = new WaveSource(x, y, currentEmitAngle, chosenAmplitude, chosenBass);
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

    private void addWaveSourceWithAngle(double x, double y, int angle) {
        WaveSource source = new WaveSource(x, y, angle, chosenAmplitude, chosenBass);
        sources.add(source);

        // Visual dot
        Circle dot = new Circle(x, y, 8);
        dot.setFill(Color.RED);
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
            onClick(dot, source);
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
            double clickX = e.getX();
            double clickY = e.getY();
            double minDistance = 10;
            boolean tooClose = false;

            for (WaveSource s : sources) {
                double dx = s.x - clickX;
                double dy = s.y - clickY;
                if (Math.hypot(dx, dy) < minDistance) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                addWaveSource(e.getX(), e.getY(), Color.GREEN);
            }
        }
    }

    private void updateWaves() {
        // Create new wave fronts from sources

        for (WaveSource source : sources) {
            source.frameCounter++;
            if (source.frameCounter >= source.emitRate) {
                source.frameCounter = 0;
                for (int i = source.emitAngle - 12; i < source.emitAngle + 12; i++) {
                    double angle = (i * 5) * Math.PI / 180;

                    waveFronts.add(new WaveFront(
                            source.x, source.y, angle, source.amplitude, 0, source.bassAmp));
                }
            }
        }
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
            wave.bassAmp *= 0.99;

            // Check wall collisions
            boolean collided = false;
            for (Wall wall : walls) {
                if (checkCollision(prevX, prevY, wave.x, wave.y, wall)) {
                    handleCollision(wave, wall);
                    collided = true;
                    break;
                }
            }

            // Mark for removal if collided or too old
            if (collided || wave.age > 500 || (wave.amplitude < 0.05 && wave.bassAmp < 0.05)) {
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

        double dot = dx * nx + dy * ny;

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
                    wave.generation + 1, wave.bassAmp * wallReflection));
        }

        // Create transmitted wave
        if (wallTransmission > 0) {
            waveFronts.add(new WaveFront(
                    wave.x + dx * offset,
                    wave.y + dy * offset,
                    wave.angle,
                    wave.amplitude * wallTransmission,
                    wave.generation + 1, wave.bassAmp * wallTransmission));
        }
    }

    private void renderWaves() {
        Platform.runLater(() -> {
            mapPane.getChildren()
                    .removeIf(node -> node instanceof Circle && ((Circle) node).getFill() == Color.TRANSPARENT);

            double amp = 0;
            for (WaveFront wave : waveFronts) {
                Circle circle = new Circle(wave.x, wave.y, 3);
                circle.setFill(Color.TRANSPARENT);
                if (!bassToggled) {
                    amp = wave.amplitude;

                } else {
                    amp = wave.bassAmp;
                }

                // Color based on amplitude and generation

                Color color = Color.CYAN.deriveColor(
                        0, 1, 1, Math.min(1, amp));

                if (wave.generation == 1)
                    color = Color.YELLOW.deriveColor(0, 1, 1, amp);
                if (wave.generation == 2)
                    color = Color.ORANGE.deriveColor(0, 1, 1, amp);
                if (wave.generation >= 3)
                    color = Color.RED.deriveColor(0, 1, 1, amp);

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