package inkball;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Main class for the Inkball game, handling game setup, level loading, rendering, and user interactions.
 */
public class App extends PApplet {

    public static final int TARGET_FRAME_RATE = 30;
    private static final float BONUS_TICK_SECONDS = 0.067f;
    private float levelSecondAccumulator;
    private float bonusAccumulator;
    private int previousFrameMillis;
    private int scoreAtLevelStart;


    /** Array of images representing balls in the game. */
    public PImage[] ballImages = new PImage[5];

    /** Array of images representing holes in the game. */
    public PImage[] holeImages = new PImage[5];

    /** Array of images representing walls in the game. */
    public PImage[] wallImages = new PImage[5];

    /** Entry point image. */
    public PImage entryPoint, tile;

    /** Level data loaded from file. */
    public String[] levelData;

    /** Size of each tile in pixels. */
    public int tileSize = 32;

    /** Height of the display bar at the top of the game window. */
    public int displayBarHeight = 64;

    /** Current level being played. */
    public int currentLevel = 0;

    /** Player's score. */
    public int score = 0;

    /** Remaining time in seconds for the current level. */
    public int timeLeft;

    /** Interval in seconds between ball spawns. */
    public float spawnInterval;

    /** Countdown timer for spawning the next ball. */
    public float countdownTimer;

    /** Array of colors for balls to be spawned in the level. */
    public String[] ballColors;

    /** Index of the next ball to be spawned. */
    public int ballsSpawnedIndex = 0;

    /** Array of positions for the balls in the queue. */
    public int[] ballPositions;

    /** Flag indicating if balls are moving in the queue. */
    public boolean ballsAreMoving = false;

    /** Speed of ball movement in pixels per frame. */
    public float ballSpeed = 1;

    /** Flag indicating if the level has been completed. */
    public boolean levelWon = false;

    /** Timer for moving yellow tiles during the win state. */
    public float yellowTileMoveTimer = 0;

    /** List of yellow tiles positions in the win state. */
    public ArrayList<PVector> yellowTiles = new ArrayList<>();

    /** Flag indicating if the game has ended. */
    public boolean gameEnded = false;

    /** List of active balls in the game. */
    public ArrayList<Ball> balls = new ArrayList<>();

    /** List of lines drawn by the player. */
    public ArrayList<Line> drawnLines = new ArrayList<>();

    /** Current line being drawn by the player. */
    public Line currentLine;

    /** List of spawner positions for balls. */
    public ArrayList<PVector> spawners = new ArrayList<>();

    /** List of hole positions in the level. */
    public ArrayList<Hole> holes = new ArrayList<>();

    /** List of bricks in the level. */
    public ArrayList<Brick> bricks = new ArrayList<>();

    /** Configuration data loaded from JSON file. */
    public JSONObject config;

    /** Flag indicating if the game is paused. */
    public boolean isPaused = false;

    /** Flag indicating if the level time has run out. */
    public boolean levelTimeUp = false;

    /**
     * Main entry point to launch the game.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        PApplet.main("inkball.App");
    }

    @Override
    public void settings() {
        size(576, 640);
    }

    @Override
    public void setup() {
        loadImages();
        blendMode(BLEND);
        frameRate(TARGET_FRAME_RATE);
        startLevel();
        previousFrameMillis = millis();
    }

    @Override
    public void keyPressed() {
        if (key == 'r' || key == 'R') {
            restartLevel();
        } else if (key == ' ') {
            if (!levelTimeUp && !gameEnded) {
                isPaused = !isPaused;
                currentLine = null;
            }
        }
    }

    /**
     * Restarts the current level, resetting balls, lines, bricks, and other level settings.
     */
    public void restartLevel() {
        if (gameEnded) {
            currentLevel = 0;
            score = 0;
        } else {
            score = scoreAtLevelStart;
        }
        startLevel();
        previousFrameMillis = millis();
    }

    /** Initializes one level identically for launch, restart, and progression. */
    private void startLevel() {
        balls.clear();
        drawnLines.clear();
        currentLine = null;
        bricks.clear();
        yellowTiles.clear();
        ballsSpawnedIndex = 0;
        ballsAreMoving = false;
        levelTimeUp = false;
        levelWon = false;
        gameEnded = false;
        isPaused = false;
        levelSecondAccumulator = 0;
        bonusAccumulator = 0;
        yellowTileMoveTimer = 0;
        scoreAtLevelStart = score;
        loadConfig();
        spawnInitialBalls();
        spawnBall();
    }

    @Override
    public void draw() {
        int now = millis();
        float elapsedSeconds = Math.max(0, now - previousFrameMillis) / 1000f;
        previousFrameMillis = now;
        updateGame(elapsedSeconds);

        background(255);
        renderDisplayBar();
        renderGameArea(displayBarHeight);
        renderDrawnLines();
        renderBricks();
        renderBalls();
        if (levelWon) {
            renderYellowTiles();
        }
        if (isPaused) {
            textAlign(CENTER, CENTER);
            textSize(16);
            fill(0);
            text("PAUSED", 330, displayBarHeight / 2f);
        }
    }

    /** Advances gameplay without rendering; elapsed time excludes paused frames. */
    public void updateGame(float elapsedSeconds) {
        if (!Float.isFinite(elapsedSeconds) || elapsedSeconds < 0) {
            throw new IllegalArgumentException("Elapsed seconds must be finite and non-negative");
        }
        if (isPaused || gameEnded || levelTimeUp) {
            return;
        }
        if (levelWon) {
            updateWinState(elapsedSeconds);
            return;
        }
        if (checkLevelComplete()) {
            handleWin();
            return;
        }
        updateCountdownTimer(elapsedSeconds);
        if (!levelTimeUp) {
            updateBallQueueMovement();
            updateBalls();
            if (checkLevelComplete()) {
                handleWin();
            }
        }
    }

    /** Draws balls even when gameplay is paused, timed out, or complete. */
    public void renderBalls() {
        for (Ball ball : balls) {
            ball.render(this);
        }
    }

    /**
     * Loads images for balls, holes, walls, entry points, and tiles.
     */
    public void loadImages() {
        for (int i = 0; i < 5; i++) {
            ballImages[i] = loadImage("inkball/ball" + i + ".png");
            holeImages[i] = loadImage("inkball/hole" + i + ".png");
            wallImages[i] = loadImage("inkball/wall" + i + ".png");
        }
        entryPoint = loadImage("inkball/entrypoint.png");
        tile = loadImage("inkball/tile.png");
    }

    /**
     * Loads configuration from a JSON file and initializes level settings.
     */
    public void loadConfig() {
        config = GameResources.loadConfig();
        JSONArray levels = config.getJSONArray("levels");

        if (currentLevel < 0 || currentLevel >= levels.size()) {
            throw new IllegalArgumentException("Level index out of range: " + currentLevel);
        }
        JSONObject level = levels.getJSONObject(currentLevel);
        String layoutFile = level.getString("layout");
        loadLevel(layoutFile);

        timeLeft = level.getInt("time");
        spawnInterval = level.getFloat("spawn_interval");
        if (timeLeft <= 0 || !Float.isFinite(spawnInterval) || spawnInterval <= 0) {
            throw new IllegalArgumentException("Level time and spawn interval must be positive");
        }
        countdownTimer = spawnInterval;

        JSONArray ballsArray = level.getJSONArray("balls");
        ballColors = new String[ballsArray.size()];
        ballPositions = new int[ballsArray.size()];
        for (int i = 0; i < ballsArray.size(); i++) {
            ballColors[i] = ballsArray.getString(i);
            ballPositions[i] = 10 + i * (tileSize + 5);
        }
        if (ballColors.length > 0 && spawners.isEmpty()) {
            throw new IllegalArgumentException("A level with queued balls needs a spawner");
        }

        if (level.hasKey("bricks")) {
            JSONArray bricksArray = level.getJSONArray("bricks");
            for (int i = 0; i < bricksArray.size(); i++) {
                JSONObject brickData = bricksArray.getJSONObject(i);
                int x = brickData.getInt("x") * tileSize;
                int y = brickData.getInt("y") * tileSize + displayBarHeight;
                String color = brickData.getString("color");
                int colorIndex = getColorIndex(color);
                bricks.add(new Brick(x, y, tileSize, color, wallImages[colorIndex]));
            }
        }
    }

    /**
     * Retrieves the index of a color by name.
     *
     * @param color Color name as a string.
     * @return Integer index of the color.
     */
    public int getColorIndex(String color) {
        switch (color.toLowerCase()) {
            case "grey":
                return 0;
            case "orange":
                return 1;
            case "blue":
                return 2;
            case "green":
                return 3;
            case "yellow":
                return 4;
            default:
                return 0;
        }
    }
    /**
     * Loads level layout from a file.
     *
     * @param filename Name of the file containing the level layout.
     */
    public void loadLevel(String filename) {
        levelData = GameResources.loadLevel(filename);
        spawners.clear();
        holes.clear();
        bricks.clear();

        for (int row = 0; row < levelData.length; row++) {
            for (int col = 0; col < levelData[row].length(); col++) {
                char tileChar = levelData[row].charAt(col);
                if (tileChar == 'H' || tileChar == 'B' || tileChar == 'b') {
                    if (col + 1 >= levelData[row].length()
                            || levelData[row].charAt(col + 1) < '0'
                            || levelData[row].charAt(col + 1) > '4') {
                        throw new IllegalArgumentException("Invalid color token in " + filename
                                + " at row " + (row + 1) + ", column " + (col + 1));
                    }
                }
                if (tileChar == 'S') {
                    spawners.add(new PVector(col * tileSize, row * tileSize + displayBarHeight));
                } else if (tileChar == 'H') {
                    int holeColorIndex = Character.getNumericValue(levelData[row].charAt(col + 1));
                    int x = col * tileSize;
                    int y = row * tileSize + displayBarHeight;
                    holes.add(new Hole(x, y, holeColorIndex, holeImages[holeColorIndex]));
                    col++; // Skip color index character
                } else if (tileChar == 'b') {
                    int brickColorIndex = Character.getNumericValue(levelData[row].charAt(col + 1));
                    String brickColor = getColorFromBallNumber(brickColorIndex);
                    bricks.add(new Brick(col * tileSize, row * tileSize + displayBarHeight, tileSize, brickColor, wallImages[brickColorIndex]));
                    col++; // Skip color index character
                }
            }
        }
    }

    /**
     * Renders the display bar at the top of the screen, showing score, timer, and status.
     */
    public void renderDisplayBar() {
        noStroke();
        fill(200);
        rect(0, 0, width, displayBarHeight);

        if (gameEnded) {
            textAlign(CENTER, CENTER);
            textSize(24);
            fill(0);
            text("=== ENDED ===", width / 2f, displayBarHeight / 2f);
        } else if (levelTimeUp) {
            textAlign(CENTER, CENTER);
            textSize(24);
            fill(0);
            text("=== TIME'S UP ===", width / 2f, displayBarHeight / 2f);
        } else {
            renderBallQueue();
            renderCountdownTimer();
            renderScoreAndTimer();
        }
    }

    /**
     * Renders the queue of balls waiting to spawn.
     */
    public void renderBallQueue() {
        fill(0);
        int ballCount = Math.min(5, ballColors.length - ballsSpawnedIndex);
        int blackRegionWidth = (tileSize + 5) * 5;
        int blackRegionHeight = tileSize + 10;

        rect(5, 5, blackRegionWidth, blackRegionHeight);
        // Incoming queue sprites must not cover the adjacent countdown while sliding.
        clip(5, 5, blackRegionWidth, blackRegionHeight);
        for (int i = 0; i < ballCount; i++) {
            String ballColor = ballColors[ballsSpawnedIndex + i];
            PImage ballImage = getBallImage(ballColor);
            if (ballImage != null) {
                image(ballImage, ballPositions[ballsSpawnedIndex + i], 10, tileSize, tileSize);
            }
        }
        noClip();
    }

    /**
     * Retrieves the image for a specific ball color.
     *
     * @param color Color of the ball as a string.
     * @return The PImage representing the ball.
     */
    public PImage getBallImage(String color) {
        switch (color.toLowerCase()) {
            case "grey":
                return ballImages[0];
            case "orange":
                return ballImages[1];
            case "blue":
                return ballImages[2];
            case "green":
                return ballImages[3];
            case "yellow":
                return ballImages[4];
            default:
                return null;
        }
    }

    /**
     * Renders the score and remaining time in the display bar.
     */
    public void renderScoreAndTimer() {
        fill(0);
        textSize(16);
        text("Score: " + score, width - 120, 20);
        text("Time: " + timeLeft, width - 120, 40);
    }

    /**
     * Renders the countdown timer for the next ball spawn.
     */
    public void renderCountdownTimer() {
        fill(0);
        textSize(20);
        textAlign(LEFT, CENTER);
        text(nf(countdownTimer, 0, 1), 10 + (tileSize + 5) * 5 + 10, displayBarHeight / 2f);
    }

    /**
     * Updates the countdown timer and spawns new balls as needed.
     */
    public void updateCountdownTimer() {
        updateCountdownTimer(1f / TARGET_FRAME_RATE);
    }

    public void updateCountdownTimer(float elapsedSeconds) {
        if (timeLeft <= 0 || isPaused || levelTimeUp || levelWon || gameEnded) {
            return;
        }
        levelSecondAccumulator += elapsedSeconds;
        int wholeSeconds = (int) (levelSecondAccumulator + 0.000001f);
        if (wholeSeconds > 0) {
            timeLeft = Math.max(0, timeLeft - wholeSeconds);
            levelSecondAccumulator -= wholeSeconds;
        }
        if (timeLeft == 0) {
            levelTimeUp = true;
            currentLine = null;
            return;
        }
        if (ballsSpawnedIndex < ballColors.length) {
            countdownTimer -= elapsedSeconds;
            while (countdownTimer <= 0 && ballsSpawnedIndex < ballColors.length) {
                int previousIndex = ballsSpawnedIndex;
                spawnBall();
                countdownTimer += spawnInterval;
                if (ballsSpawnedIndex == previousIndex) {
                    break;
                }
            }
        }
        if (ballsSpawnedIndex >= ballColors.length) {
            countdownTimer = 0;
        }
    }

    /**
     * Moves balls in the queue to their target positions.
     */
    public void updateBallQueueMovement() {
        if (ballsAreMoving) {
            boolean allBallsReached = true;

            for (int i = ballsSpawnedIndex; i < ballColors.length && i < ballsSpawnedIndex + 5; i++) {
                int targetX = 10 + (i - ballsSpawnedIndex) * (tileSize + 5);
                if (ballPositions[i] > targetX) {
                    ballPositions[i] = Math.max(targetX, ballPositions[i] - Math.max(1, (int) ballSpeed));
                    allBallsReached = false;
                }
            }

            if (allBallsReached) {
                ballsAreMoving = false;
            }
        }
    }

    /**
     * Renders the game area with tiles, walls, spawners, and holes.
     *
     * @param yOffset Vertical offset for rendering the game area.
     */
    public void renderGameArea(int yOffset) {
        for (int row = 0; row < levelData.length; row++) {
            for (int col = 0; col < levelData[row].length(); col++) {
                int x = col * tileSize;
                int y = row * tileSize + yOffset;
                image(tile, x, y, tileSize, tileSize);
            }
        }

        for (int row = 0; row < levelData.length; row++) {
            for (int col = 0; col < levelData[row].length(); col++) {
                int x = col * tileSize;
                int y = row * tileSize + yOffset;
                char tileChar = levelData[row].charAt(col);

                switch (tileChar) {
                    case 'X':
                        image(wallImages[0], x, y, tileSize, tileSize);
                        break;
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                        int wallColor = Character.getNumericValue(tileChar);
                        image(wallImages[wallColor], x, y, tileSize, tileSize);
                        break;
                    case 'S':
                        image(entryPoint, x, y, tileSize, tileSize);
                        break;
                    case 'B':
                    case 'H':
                    case 'b':
                        col++;
                        break;
                }
            }
        }

        for (Hole hole : holes) {
            hole.render(this);
        }
    }

    /**
     * Renders bricks in the game.
     */
    public void renderBricks() {
        for (Brick brick : bricks) {
            brick.render(this);
        }
    }

    /**
     * Updates and moves all active balls in the game, checking for collisions and scoring.
     */
    public void updateBalls() {
        for (int i = balls.size() - 1; i >= 0; i--) {
            Ball ball = balls.get(i);
            ball.applyHoleAttraction(holes, config, currentLevel);
            ball.update();
            if (!ball.isCaptured) {
                checkWallCollision(ball);
                checkLineCollision(ball);
                checkBrickCollision(ball);
            }

            if (ball.isCaptured) {
                score = Math.max(0, score + ball.scoreChange);
                if (ball.needsRespawn) {
                    enqueueRetry(ball.color);
                }

                balls.remove(i);
            }
        }
    }

    /** Appends retries without overwriting unspawned colors or losing layout balls. */
    private void enqueueRetry(String color) {
        boolean queueWasEmpty = ballsSpawnedIndex >= ballColors.length;
        int index = ballColors.length;
        ballColors = Arrays.copyOf(ballColors, index + 1);
        ballPositions = Arrays.copyOf(ballPositions, index + 1);
        ballColors[index] = color;
        ballPositions[index] = 10 + (index - ballsSpawnedIndex) * (tileSize + 5);
        if (queueWasEmpty) {
            countdownTimer = spawnInterval;
        }
    }

    /**
     * Retrieves the color of the hole at a given position.
     *
     * @param x X-coordinate.
     * @param y Y-coordinate.
     * @return Color of the hole at the specified position.
     */
    public String getHoleColorAt(float x, float y) {
        for (Hole hole : holes) {
            if (PVector.dist(new PVector(x, y), hole.getCenter()) < hole.size / 2f) {
                return hole.getHoleColor();
            }
        }
        return "";
    }

    /**
     * Checks if a ball collides with bricks and handles collision response.
     *
     * @param ball Ball to check for brick collisions.
     */
    public void checkBrickCollision(Ball ball) {
        for (Brick brick : bricks) {
            if (brick.isDestroyed()) {
                continue;
            }

            if (ball.x + ball.size > brick.x && ball.x < brick.x + tileSize &&
                    ball.y + ball.size > brick.y && ball.y < brick.y + tileSize) {

                float ballCenterX = ball.x + ball.size / 2f;
                float ballCenterY = ball.y + ball.size / 2f;
                float brickCenterX = brick.x + tileSize / 2f;
                float brickCenterY = brick.y + tileSize / 2f;

                float dx = ballCenterX - brickCenterX;
                float dy = ballCenterY - brickCenterY;

                float absDX = Math.abs(dx);
                float absDY = Math.abs(dy);

                float overlapX = (ball.size / 2f + tileSize / 2f) - absDX;
                float overlapY = (ball.size / 2f + tileSize / 2f) - absDY;

                if (overlapX < overlapY) {
                    if (dx > 0) {
                        ball.x += overlapX;
                    } else {
                        ball.x -= overlapX;
                    }
                    ball.vx = -ball.vx;
                } else {
                    if (dy > 0) {
                        ball.y += overlapY;
                    } else {
                        ball.y -= overlapY;
                    }
                    ball.vy = -ball.vy;
                }

                if (ball.color.equals(brick.color) || brick.color.equals("grey")) {
                    brick.hit(ball.color);
                }
            }
        }
    }

    /**
     * Checks if the level is complete by verifying all balls have been spawned and captured.
     *
     * @return True if the level is complete, false otherwise.
     */
    public boolean checkLevelComplete() {
        return ballsSpawnedIndex >= ballColors.length && balls.isEmpty();
    }

    /**
     * Handles actions when the player completes the level.
     */
    public void handleWin() {
        levelWon = true;
        currentLine = null;
        bonusAccumulator = 0;
        yellowTileMoveTimer = 0;
        yellowTiles.clear();
        yellowTiles.add(new PVector(0, displayBarHeight));
        yellowTiles.add(new PVector(width - tileSize, height - tileSize));
    }

    /**
     * Updates the state of the game in the win condition.
     */
    public void updateWinState() {
        updateWinState(1f / TARGET_FRAME_RATE);
    }

    public void updateWinState(float elapsedSeconds) {
        bonusAccumulator += elapsedSeconds;
        int ticks = (int) ((bonusAccumulator + 0.000001f) / BONUS_TICK_SECONDS);
        if (ticks > 0) {
            int converted = Math.min(ticks, timeLeft);
            score += converted;
            timeLeft -= converted;
            bonusAccumulator -= ticks * BONUS_TICK_SECONDS;
        }
        yellowTileMoveTimer += elapsedSeconds;
        while (yellowTileMoveTimer + 0.000001f >= BONUS_TICK_SECONDS) {
            yellowTileMoveTimer -= BONUS_TICK_SECONDS;
            moveYellowTilesClockwise();
        }
        if (timeLeft <= 0) {
            advanceToNextLevel();
        }
    }

    /**
     * Moves yellow tiles in a clockwise pattern.
     */
    public void moveYellowTilesClockwise() {
        for (PVector tile : yellowTiles) {
            if (tile.x == 0 && tile.y < height - tileSize) {
                tile.y += tileSize;
            } else if (tile.y == height - tileSize && tile.x < width - tileSize) {
                tile.x += tileSize;
            } else if (tile.x == width - tileSize && tile.y > displayBarHeight) {
                tile.y -= tileSize;
            } else if (tile.y == displayBarHeight && tile.x > 0) {
                tile.x -= tileSize;
            }
        }
    }

    /**
     * Renders yellow tiles in the win condition.
     */
    public void renderYellowTiles() {
        for (PVector tile : yellowTiles) {
            image(wallImages[4], tile.x, tile.y, tileSize, tileSize);
        }
    }

    /**
     * Advances to the next level, resetting relevant state.
     */
    public void advanceToNextLevel() {
        JSONArray levels = config.getJSONArray("levels");
        if (currentLevel + 1 < levels.size()) {
            currentLevel++;
            startLevel();
        } else {
            levelWon = false;
            yellowTiles.clear();
            gameEnded = true;
        }
    }

    /**
     * Spawns a new ball at a random spawner location.
     */
    public void spawnBall() {
        if (spawners.size() > 0 && ballsSpawnedIndex < ballColors.length) {
            int randomIndex = (int) random(0, spawners.size());
            PVector spawnPoint = spawners.get(randomIndex);

            float vx = random(-1, 1) > 0 ? 2 : -2;
            float vy = random(-1, 1) > 0 ? 2 : -2;

            String color = ballColors[ballsSpawnedIndex];
            Ball newBall = new Ball(spawnPoint.x, spawnPoint.y, vx, vy, tileSize, color, getBallImage(color));
            balls.add(newBall);
            ballsSpawnedIndex++;
            ballsAreMoving = true;
        }
    }

    /**
     * Spawns balls as defined in the level layout.
     */
    public void spawnInitialBalls() {
        for (int row = 0; row < levelData.length; row++) {
            for (int col = 0; col < levelData[row].length(); col++) {
                if (levelData[row].charAt(col) == 'B') {
                    int ballColorIndex = Character.getNumericValue(levelData[row].charAt(col + 1));
                    String color = getColorFromBallNumber(ballColorIndex);

                    float vx = random(-1, 1) > 0 ? 2 : -2;
                    float vy = random(-1, 1) > 0 ? 2 : -2;

                    float x = col * tileSize;
                    float y = row * tileSize + displayBarHeight;
                    Ball newBall = new Ball(x, y, vx, vy, tileSize, color, getBallImage(color));
                    balls.add(newBall);
                    col++; // Skip color index character
                }
            }
        }
    }

    /**
     * Retrieves the color name for a ball based on its index.
     *
     * @param ballColorIndex Index of the ball color.
     * @return String name of the color.
     */
    public String getColorFromBallNumber(int ballColorIndex) {
        switch (ballColorIndex) {
            case 0:
                return "grey";
            case 1:
                return "orange";
            case 2:
                return "blue";
            case 3:
                return "green";
            case 4:
                return "yellow";
            default:
                return "grey";
        }
    }

    /**
     * Checks if a ball collides with walls and adjusts its trajectory.
     *
     * @param ball Ball to check for collisions.
     */
    public void checkWallCollision(Ball ball) {
        checkWindowBoundaryCollision(ball);

        for (int row = 0; row < levelData.length; row++) {
            for (int col = 0; col < levelData[row].length(); col++) {
                char tileChar = levelData[row].charAt(col);

                if (tileChar == 'X' || Character.isDigit(tileChar)) {
                    int x = col * tileSize;
                    int y = row * tileSize + displayBarHeight;

                    if (ball.x + ball.size > x && ball.x < x + tileSize &&
                            ball.y + ball.size > y && ball.y < y + tileSize) {

                        float ballCenterX = ball.x + ball.size / 2f;
                        float ballCenterY = ball.y + ball.size / 2f;
                        float wallCenterX = x + tileSize / 2f;
                        float wallCenterY = y + tileSize / 2f;

                        float dx = ballCenterX - wallCenterX;
                        float dy = ballCenterY - wallCenterY;

                        float absDX = Math.abs(dx);
                        float absDY = Math.abs(dy);

                        float overlapX = (ball.size / 2f + tileSize / 2f) - absDX;
                        float overlapY = (ball.size / 2f + tileSize / 2f) - absDY;

                        PVector collisionNormal;

                        if (overlapX < overlapY) {
                            if (dx > 0) {
                                collisionNormal = new PVector(1, 0);
                                ball.x += overlapX;
                            } else {
                                collisionNormal = new PVector(-1, 0);
                                ball.x -= overlapX;
                            }
                        } else {
                            if (dy > 0) {
                                collisionNormal = new PVector(0, 1);
                                ball.y += overlapY;
                            } else {
                                collisionNormal = new PVector(0, -1);
                                ball.y -= overlapY;
                            }
                        }

                        reflectIfApproaching(ball, collisionNormal);

                        if (Character.isDigit(tileChar)) {
                            int wallColorIndex = Character.getNumericValue(tileChar);
                            String newColor = getColorFromBallNumber(wallColorIndex);
                            ball.changeColor(newColor, getBallImage(newColor));
                        }
                    }
                } else if (tileChar == 'B' || tileChar == 'H' || tileChar == 'b') {
                    col++;
                }
            }
        }
    }

    /**
     * Checks if a ball collides with the window boundaries.
     *
     * @param ball Ball to check for boundary collisions.
     */
    public void checkWindowBoundaryCollision(Ball ball) {
        if (ball.x <= 0) {
            reflectIfApproaching(ball, new PVector(1, 0));
            ball.x = 0;
        } else if (ball.x + ball.size >= width) {
            reflectIfApproaching(ball, new PVector(-1, 0));
            ball.x = width - ball.size;
        }
        if (ball.y <= displayBarHeight) {
            reflectIfApproaching(ball, new PVector(0, 1));
            ball.y = displayBarHeight;
        } else if (ball.y + ball.size >= height) {
            reflectIfApproaching(ball, new PVector(0, -1));
            ball.y = height - ball.size;
        }
    }

    private void reflectIfApproaching(Ball ball, PVector normal) {
        if (ball.vx * normal.x + ball.vy * normal.y < 0) {
            ball.reflect(normal);
        }
    }

    /**
     * Calculates the normal vector of a line segment.
     *
     * @param p1 First endpoint of the line.
     * @param p2 Second endpoint of the line.
     * @return The normal vector.
     */
    public PVector calculateNormalVector(PVector p1, PVector p2) {
        PVector lineVec = PVector.sub(p2, p1);
        PVector normal = new PVector(-lineVec.y, lineVec.x);
        normal.normalize();
        return normal;
    }

    /**
     * Checks if a ball collides with lines drawn by the player.
     *
     * @param ball Ball to check for line collisions.
     */
    public void checkLineCollision(Ball ball) {
        for (int i = drawnLines.size() - 1; i >= 0; i--) {
            Line line = drawnLines.get(i);
            boolean collisionDetected = false;
            for (int j = 1; j < line.points.size(); j++) {
                PVector p1 = line.points.get(j - 1);
                PVector p2 = line.points.get(j);

                if (willCollide(ball, p1, p2)) {
                    PVector normal = calculateNormalVector(p1, p2);
                    ball.reflect(normal);
                    collisionDetected = true;
                    break;
                }
            }
            if (collisionDetected) {
                drawnLines.remove(i);
                break;
            }
        }
    }

    /**
     * Removes a line at the specified coordinates if it exists.
     *
     * @param x X-coordinate to check.
     * @param y Y-coordinate to check.
     */
    public void removeLineAt(int x, int y) {
        for (int i = drawnLines.size() - 1; i >= 0; i--) {
            Line line = drawnLines.get(i);
            if (isPointNearLine(x, y, line)) {
                drawnLines.remove(i);
                break;
            }
        }
    }

    /**
     * Checks if a point is near a given line.
     *
     * @param x    X-coordinate of the point.
     * @param y    Y-coordinate of the point.
     * @param line Line to check against.
     * @return True if the point is near the line, false otherwise.
     */
    public boolean isPointNearLine(int x, int y, Line line) {
        for (int i = 1; i < line.points.size(); i++) {
            PVector p1 = line.points.get(i - 1);
            PVector p2 = line.points.get(i);
            float distance = pointToSegmentDistance(new PVector(x, y), p1, p2);
            if (distance < 10) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void mousePressed() {
        if (levelTimeUp || gameEnded || isPaused || levelWon) {
            currentLine = null;
            return;
        }
        if (mouseY < displayBarHeight || mouseY >= height || mouseX < 0 || mouseX >= width) {
            return;
        }
        if (mouseButton == RIGHT || (mouseButton == LEFT && keyPressed && key == CODED && keyCode == CONTROL)) {
            removeLineAt(mouseX, mouseY);
        } else if (mouseButton == LEFT) {
            currentLine = new Line();
            currentLine.addPoint(mouseX, mouseY);
        }
    }

    @Override
    public void mouseDragged() {
        if (levelTimeUp || gameEnded || isPaused || levelWon) {
            currentLine = null;
            return;
        }
        if (mouseButton == LEFT && currentLine != null && mouseY >= displayBarHeight
                && mouseY < height && mouseX >= 0 && mouseX < width) {
            currentLine.addPoint(mouseX, mouseY);
        }
    }

    @Override
    public void mouseReleased() {
        if (levelTimeUp || gameEnded || isPaused || levelWon) {
            currentLine = null;
            return;
        }
        if (mouseButton == LEFT && currentLine != null) {
            if (currentLine.points.size() >= 2) {
                drawnLines.add(currentLine);
            }
            currentLine = null;
        }
    }

    /**
     * Renders all drawn lines on the screen.
     */
    public void renderDrawnLines() {
        for (Line line : drawnLines) {
            line.render(this);
        }
        if (currentLine != null) {
            currentLine.render(this);
        }
    }

    /**
     * Checks if a ball will collide with a line segment.
     *
     * @param ball Ball to check for collision.
     * @param p1   First endpoint of the line segment.
     * @param p2   Second endpoint of the line segment.
     * @return True if a collision is detected, false otherwise.
     */
    public boolean willCollide(Ball ball, PVector p1, PVector p2) {
        float nextX = ball.x + ball.vx;
        float nextY = ball.y + ball.vy;
        PVector ballNextPos = new PVector(nextX + ball.size / 2f, nextY + ball.size / 2f);

        float distance = pointToSegmentDistance(ballNextPos, p1, p2);

        return distance <= ball.size / 2f;
    }

    /**
     * Calculates the distance from a point to a line segment.
     *
     * @param p Point to check.
     * @param v First endpoint of the line segment.
     * @param w Second endpoint of the line segment.
     * @return Distance from the point to the line segment.
     */
    public float pointToSegmentDistance(PVector p, PVector v, PVector w) {
        float l2 = PVector.sub(w, v).magSq();
        if (l2 == 0.0) return PVector.dist(p, v);
        float t = PVector.sub(p, v).dot(PVector.sub(w, v)) / l2;
        t = constrain(t, 0, 1);
        PVector projection = PVector.add(v, PVector.mult(PVector.sub(w, v), t));
        return PVector.dist(p, projection);
    }
}
