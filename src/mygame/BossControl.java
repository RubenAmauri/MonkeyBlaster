package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.util.Random;

public class BossControl extends AbstractControl {

    private static final int   FRAME_COUNT   = 5;
    private static final long  FRAME_MS      = 150;
    private static final float SPEED         = 150f;
    private static final float WALL_HALF     = 300f;
    private static final float BOUNCE_MARGIN = 50f;

    private final Spatial      player;
    private final Node         bossLaserNode;
    private final AssetManager assetManager;
    private final int          screenWidth, screenHeight;
    private final float        cx, cy;

    // Animación
    private final Texture2D[] frames;
    private Picture bossPicture;
    private int     currentFrame  = 0;
    private long    lastFrameTime = 0;

    // Movimiento libre
    private final Vector3f velocity;
    private long           nextDirChange;

    // Disparo
    private int  shotsRemaining = 0;
    private long nextGroupTime;
    private long nextShotTime   = 0;
    private final Random rand   = new Random();

    // Salud
    private int hitpoints = 200;

    public BossControl(Spatial player, Node bossLaserNode,
                       AssetManager assetManager, int screenWidth, int screenHeight) {
        this.player        = player;
        this.bossLaserNode = bossLaserNode;
        this.assetManager  = assetManager;
        this.screenWidth   = screenWidth;
        this.screenHeight  = screenHeight;
        this.cx            = screenWidth  / 2f;
        this.cy            = screenHeight / 2f;

        long now           = System.currentTimeMillis();
        this.nextGroupTime = now + 1500;
        this.velocity      = randomVelocity();
        this.nextDirChange = now + 1000 + rand.nextInt(2001);

        frames = new Texture2D[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = (Texture2D) assetManager
                    .loadTexture("Textures/boss_" + (i + 1) + "_SinFondo.png");
        }
    }

    @Override
    public void setSpatial(Spatial s) {
        super.setSpatial(s);
        if (s instanceof Node) {
            bossPicture = (Picture) ((Node) s).getChild("bossSprite");

            // Posiciona el jefe fuera del cuadrado 600x600 de Wanderers
            int side = rand.nextInt(4);
            float startX = cx, startY = cy;
            switch (side) {
                case 0: startX = cx - 400f; break;
                case 1: startX = cx + 400f; break;
                case 2: startY = cy - 400f; break;
                default: startY = cy + 400f; break;
            }
            startX = Math.max(64f, Math.min(screenWidth  - 64f, startX));
            startY = Math.max(64f, Math.min(screenHeight - 64f, startY));
            s.setLocalTranslation(startX, startY, 0f);
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        long now = System.currentTimeMillis();

        // Animación de frames
        if (now - lastFrameTime >= FRAME_MS) {
            currentFrame  = (currentFrame + 1) % FRAME_COUNT;
            lastFrameTime = now;
            setFrame(currentFrame);
        }

        // Movimiento libre
        Vector3f pos = spatial.getLocalTranslation().clone();
        pos.x += velocity.x * tpf;
        pos.y += velocity.y * tpf;

        // Rebote en bordes de pantalla
        if (pos.x <= 0f)            { pos.x = 0f;           velocity.x =  Math.abs(velocity.x); }
        if (pos.x >= screenWidth)   { pos.x = screenWidth;  velocity.x = -Math.abs(velocity.x); }
        if (pos.y <= 0f)            { pos.y = 0f;           velocity.y =  Math.abs(velocity.y); }
        if (pos.y >= screenHeight)  { pos.y = screenHeight; velocity.y = -Math.abs(velocity.y); }

        // Rebote al acercarse al exterior del cuadrado de Wanderers
        float dx = Math.abs(pos.x - cx) - WALL_HALF;
        float dy = Math.abs(pos.y - cy) - WALL_HALF;
        if (dx >= 0f && dx < BOUNCE_MARGIN) {
            float sign = pos.x > cx ? 1f : -1f;
            velocity.x = Math.abs(velocity.x) * sign; // empuja hacia afuera
        }
        if (dy >= 0f && dy < BOUNCE_MARGIN) {
            float sign = pos.y > cy ? 1f : -1f;
            velocity.y = Math.abs(velocity.y) * sign;
        }

        spatial.setLocalTranslation(pos);

        // Cambio de dirección aleatorio cada 1-3 s
        if (now >= nextDirChange) {
            velocity.set(randomVelocity());
            nextDirChange = now + 1000 + rand.nextInt(2001);
        }

        // Patrón de disparo semialeatório
        if (shotsRemaining > 0) {
            if (now >= nextShotTime) {
                fireLaser();
                shotsRemaining--;
                nextShotTime = now + 200;
            }
        } else if (now >= nextGroupTime) {
            shotsRemaining = 1 + rand.nextInt(3);
            nextGroupTime  = now + 1000 + rand.nextInt(1001);
            nextShotTime   = now;
        }
    }

    private Vector3f randomVelocity() {
        float angle = rand.nextFloat() * 2f * (float) Math.PI;
        return new Vector3f((float) Math.cos(angle) * SPEED,
                            (float) Math.sin(angle) * SPEED, 0f);
    }

    private void fireLaser() {
        Vector3f bossPos = spatial.getLocalTranslation();
        Vector3f dir     = player.getLocalTranslation().subtract(bossPos).normalizeLocal();

        Node laser = createLaserSpatial();
        laser.setLocalTranslation(bossPos.clone());
        laser.addControl(new BossLaserControl(dir, screenWidth, screenHeight));
        bossLaserNode.attachChild(laser);
    }

    private Node createLaserSpatial() {
        Node node = new Node("BossLaser");

        Picture   pic = new Picture("BossLaser");
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/laser_boss_SinFondo.png");
        pic.setTexture(assetManager, tex, true);

        float w = tex.getImage().getWidth();
        float h = tex.getImage().getHeight();
        pic.setWidth(w);
        pic.setHeight(h);
        pic.move(-w / 2f, -h / 2f, 0);

        Material mat = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");
        mat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
        node.setMaterial(mat);
        node.setUserData("radius", w / 2f);
        node.attachChild(pic);
        return node;
    }

    private void setFrame(int frame) {
        if (bossPicture == null) return;
        bossPicture.getMaterial().setTexture("Texture", frames[frame]);
    }

    public void wasShot()              { hitpoints--; }
    public void takeDamage(int amount) { hitpoints -= amount; }
    public boolean isDead()            { return hitpoints <= 0; }
    public int getHp()                 { return hitpoints; }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}
