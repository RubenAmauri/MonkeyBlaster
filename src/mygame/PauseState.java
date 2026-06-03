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
import com.jme3.scene.Node;

public class PauseState extends AbstractAppState implements ActionListener {

    private SimpleApplication app;
    private Node guiNode;
    private final GameState gameState;

    private BitmapText titleText;
    private BitmapText[] optionTexts;
    private int selectedIndex = 0;

    private static final String[] OPTIONS = {"Continuar", "Reiniciar", "Menú Principal"};

    private static final ColorRGBA COL_SELECTED = ColorRGBA.White;
    private static final ColorRGBA COL_NORMAL   = new ColorRGBA(0.55f, 0.55f, 0.55f, 1f);

    public PauseState(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app     = (SimpleApplication) app;
        this.guiNode = this.app.getGuiNode();

        int w = this.app.getContext().getSettings().getWidth();
        int h = this.app.getContext().getSettings().getHeight();

        setupUI(w, h);
        setupInput();
        updateColors();
    }

    private void setupUI(int w, int h) {
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        titleText = new BitmapText(font, false);
        titleText.setSize(70);
        titleText.setText("PAUSA");
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(
                (w - titleText.getLineWidth()) / 2f, h * 0.65f, 2f);
        guiNode.attachChild(titleText);

        optionTexts = new BitmapText[OPTIONS.length];
        float startY = h * 0.48f;
        float stepY  = 60f;

        for (int i = 0; i < OPTIONS.length; i++) {
            optionTexts[i] = new BitmapText(font, false);
            optionTexts[i].setSize(36);
            optionTexts[i].setText(OPTIONS[i]);
            optionTexts[i].setLocalTranslation(
                    (w - optionTexts[i].getLineWidth()) / 2f,
                    startY - i * stepY, 2f);
            guiNode.attachChild(optionTexts[i]);
        }
    }

    private void setupInput() {
        app.getInputManager().addMapping("pauseUp",
                new KeyTrigger(KeyInput.KEY_UP));
        app.getInputManager().addMapping("pauseDown",
                new KeyTrigger(KeyInput.KEY_DOWN));
        app.getInputManager().addMapping("pauseSelect",
                new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addListener(this, "pauseUp", "pauseDown", "pauseSelect");
    }

    // -------------------------------------------------------
    // Update — el grid sigue animado porque GameState.update()
    // sigue ejecutándose (paused=true salta la lógica, no el grid)
    // -------------------------------------------------------
    @Override
    public void update(float tpf) {}

    // -------------------------------------------------------
    // Input
    // -------------------------------------------------------
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) return;

        switch (name) {
            case "pauseUp":
                selectedIndex = (selectedIndex - 1 + OPTIONS.length) % OPTIONS.length;
                updateColors();
                break;
            case "pauseDown":
                selectedIndex = (selectedIndex + 1) % OPTIONS.length;
                updateColors();
                break;
            case "pauseSelect":
                handleSelection();
                break;
        }
    }

    private void handleSelection() {
        AppStateManager sm = app.getStateManager();
        switch (selectedIndex) {
            case 0: // Continuar
                gameState.setPaused(false);
                sm.detach(this);
                break;
            case 1: // Reiniciar
                sm.detach(this);
                sm.detach(gameState);
                sm.attach(new GameState());
                break;
            case 2: // Menú Principal
                sm.detach(this);
                sm.detach(gameState);
                sm.attach(new MenuState());
                break;
        }
    }

    private void updateColors() {
        for (int i = 0; i < optionTexts.length; i++) {
            optionTexts[i].setColor(i == selectedIndex ? COL_SELECTED : COL_NORMAL);
        }
    }

    // -------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------
    @Override
    public void cleanup() {
        super.cleanup();

        app.getInputManager().removeListener(this);
        app.getInputManager().deleteMapping("pauseUp");
        app.getInputManager().deleteMapping("pauseDown");
        app.getInputManager().deleteMapping("pauseSelect");

        guiNode.detachChild(titleText);
        for (BitmapText t : optionTexts) guiNode.detachChild(t);
    }
}
