package mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class MenuState extends AbstractAppState implements ActionListener {

    private SimpleApplication app;
    private Node guiNode;
    private Grid grid;

    private BitmapText titleText;
    private BitmapText[] menuTexts;
    private int selectedIndex = 0;

    private static final String[]  LABELS  = {"Jugar", "2 Jugadores", "Selección de nave", "Salir", "Jefe instantaneo"};
    private static final boolean[] ENABLED = {true,   false,          false,               true,    true};

    private static final ColorRGBA COL_SELECTED = ColorRGBA.White;
    private static final ColorRGBA COL_NORMAL = new ColorRGBA(0.55f, 0.55f, 0.55f, 1f);
    private static final ColorRGBA COL_DISABLED = new ColorRGBA(0.28f, 0.28f, 0.28f, 1f);

    private Sound sound;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.guiNode = this.app.getGuiNode();

        int w = this.app.getContext().getSettings().getWidth();
        int h = this.app.getContext().getSettings().getHeight();

        grid = new Grid(w, h, new Vector2f(25, 25), guiNode, this.app.getAssetManager());

        setupText(w, h);
        setupInput();
        sound = new Sound(this.app.getAssetManager());
        sound.playMenuMusic();
        updateColors();
    }

    private void setupText(int w, int h) {
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        titleText = new BitmapText(font, false);
        titleText.setSize(60);
        titleText.setText("Monkey Blaster");
        titleText.setColor(new ColorRGBA(0.4f, 0.8f, 1f, 1f));
        titleText.setLocalTranslation(
                (w - titleText.getLineWidth()) / 2f, h * 0.82f, 1f);
        guiNode.attachChild(titleText);

        menuTexts = new BitmapText[LABELS.length];
        float startY = h * 0.55f;
        float stepY = 65f;

        for (int i = 0; i < LABELS.length; i++) {
            menuTexts[i] = new BitmapText(font, false);
            menuTexts[i].setSize(36);
            menuTexts[i].setText(LABELS[i]);
            menuTexts[i].setLocalTranslation(
                    (w - menuTexts[i].getLineWidth()) / 2f,
                    startY - i * stepY, 1f);
            guiNode.attachChild(menuTexts[i]);
        }
    }

    private void setupInput() {
        app.getInputManager().addMapping("menuUp",
                new KeyTrigger(KeyInput.KEY_UP));
        app.getInputManager().addMapping("menuDown",
                new KeyTrigger(KeyInput.KEY_DOWN));
        app.getInputManager().addMapping("menuSelect",
                new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addListener(this, "menuUp", "menuDown", "menuSelect");
    }

    // -------------------------------------------------------
    // Update
    // -------------------------------------------------------
    @Override
    public void update(float tpf) {
        grid.update(tpf);

        // El grid se dobla hacia el botón seleccionado
        Vector3f pos = menuTexts[selectedIndex].getWorldTranslation();
        grid.applyImplosiveForce(30f, pos, 150f);
    }

    // -------------------------------------------------------
    // Input
    // -------------------------------------------------------
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) {
            return;
        }

        switch (name) {
            case "menuUp":
                do {
                    selectedIndex = (selectedIndex - 1 + LABELS.length) % LABELS.length;
                } while (!ENABLED[selectedIndex]);
                updateColors();
                break;

            case "menuDown":
                do {
                    selectedIndex = (selectedIndex + 1) % LABELS.length;
                } while (!ENABLED[selectedIndex]);
                updateColors();
                break;

            case "menuSelect":
                handleSelection();
                break;
        }
    }

    private void handleSelection() {
        switch (selectedIndex) {
            case 0: // Jugar
                sound.shot();
                sound.stopMenuMusic();
                app.getStateManager().detach(this);
                app.getStateManager().attach(new GameState(false));
                break;
            case 3: // Salir
                app.stop();
                break;
            case 4: // Modo Debug — empieza con 49 puntos para ver al jefe inmediatamente
                sound.shot();
                sound.stopMenuMusic();
                app.getStateManager().detach(this);
                app.getStateManager().attach(new GameState(true));
                break;
        }
    }

    private void updateColors() {
        for (int i = 0; i < menuTexts.length; i++) {
            if (i == selectedIndex) {
                menuTexts[i].setColor(COL_SELECTED);
            } else if (ENABLED[i]) {
                menuTexts[i].setColor(COL_NORMAL);
            } else {
                menuTexts[i].setColor(COL_DISABLED);
            }
        }
    }

    // -------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------
    @Override
    public void cleanup() {
        super.cleanup();

        app.getInputManager().removeListener(this);
        app.getInputManager().deleteMapping("menuUp");
        app.getInputManager().deleteMapping("menuDown");
        app.getInputManager().deleteMapping("menuSelect");
        sound.stopMenuMusic();

        guiNode.detachChild(titleText);
        for (BitmapText t : menuTexts) {
            guiNode.detachChild(t);
        }
        guiNode.detachChild(grid.getNode());
    }
}
