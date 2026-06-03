package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.scene.Node;
import java.io.*;

public class Hud {

    private AssetManager assetManager;
    private Node guiNode;
    private int screenWidth, screenHeight;
    private final int fontSize = 30;
    private final int multiplierExpiryTime = 2000;
    private final int maxMultiplier = 25;
    public int lives;
    public int score;
    public int multiplier;
    private long multiplierActivationTime;
    private int scoreForExtraLife;
    private BitmapFont guiFont;
    private BitmapText livesText;
    private BitmapText scoreText;
    private BitmapText multiplierText;
    private Node gameOverNode;

    public Hud(AssetManager assetManager, Node guiNode,
               int screenWidth, int screenHeight) {
        this.assetManager = assetManager;
        this.guiNode      = guiNode;
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
        setupText();
    }

    private void setupText() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        livesText = new BitmapText(guiFont, false);
        livesText.setLocalTranslation(30, screenHeight - 30, 0);
        livesText.setSize(fontSize);
        livesText.setText("Lives: " + lives);
        guiNode.attachChild(livesText);

        scoreText = new BitmapText(guiFont, false);
        scoreText.setLocalTranslation(screenWidth - 200, screenHeight - 30, 0);
        scoreText.setSize(fontSize);
        scoreText.setText("Score: " + score);
        guiNode.attachChild(scoreText);

        multiplierText = new BitmapText(guiFont, false);
        multiplierText.setLocalTranslation(screenWidth - 200, screenHeight - 100, 0);
        multiplierText.setSize(fontSize);
        multiplierText.setText("Multiplier: " + multiplier);
        guiNode.attachChild(multiplierText);
    }

    public void reset() {
        score      = 0;
        multiplier = 1;
        lives      = 4;
        multiplierActivationTime = System.currentTimeMillis();
        scoreForExtraLife = 2000;
        updateHUD();
    }

    private void updateHUD() {
        livesText.setText("Lives: " + lives);
        scoreText.setText("Score: " + score);
        multiplierText.setText("Multiplier: " + multiplier);
    }

    public void addPoints(int basePoints) {
        score += basePoints * multiplier;
        if (score >= scoreForExtraLife) {
            scoreForExtraLife += 2000;
            lives++;
        }
        increaseMultiplier();
        updateHUD();
    }

    private void increaseMultiplier() {
        multiplierActivationTime = System.currentTimeMillis();
        if (multiplier < maxMultiplier)
            multiplier++;
    }

    public boolean removeLife() {
        if (lives == 0) return false;
        lives--;
        updateHUD();
        return true;
    }

    public void update() {
        if (multiplier > 1) {
            if (System.currentTimeMillis() - multiplierActivationTime
                    > multiplierExpiryTime) {
                multiplier = 1;
                multiplierActivationTime = System.currentTimeMillis();
                updateHUD();
            }
        }
    }

    public void endGame() {
        gameOverNode = new Node();
        gameOverNode.setLocalTranslation(
                screenWidth / 2 - 180, screenHeight / 2 + 100, 0);
        guiNode.attachChild(gameOverNode);

        int highscore = loadHighscore();
        if (score > highscore) saveHighscore();

        BitmapText gameOverText = new BitmapText(guiFont, false);
        gameOverText.setLocalTranslation(0, 0, 0);
        gameOverText.setSize(fontSize);
        gameOverText.setText("Game Over");
        gameOverNode.attachChild(gameOverText);

        BitmapText yourScoreText = new BitmapText(guiFont, false);
        yourScoreText.setLocalTranslation(0, -50, 0);
        yourScoreText.setSize(fontSize);
        yourScoreText.setText("Your Score: " + score);
        gameOverNode.attachChild(yourScoreText);

        BitmapText highscoreText = new BitmapText(guiFont, false);
        highscoreText.setLocalTranslation(0, -100, 0);
        highscoreText.setSize(fontSize);
        highscoreText.setText("Highscore: " + highscore);
        gameOverNode.attachChild(highscoreText);
    }

    public void forceUpdateDisplay() {
        updateHUD();
    }

    public void cleanup() {
        guiNode.detachChild(livesText);
        guiNode.detachChild(scoreText);
        guiNode.detachChild(multiplierText);
        if (gameOverNode != null) {
            guiNode.detachChild(gameOverNode);
            gameOverNode = null;
        }
    }

    private int loadHighscore() {
        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader(new File("highscore.txt")));
            String line = reader.readLine();
            reader.close();
            return Integer.valueOf(line);
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveHighscore() {
        try {
            FileWriter writer = new FileWriter(
                    new File("highscore.txt"), false);
            writer.write(score + System.getProperty("line.separator"));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}