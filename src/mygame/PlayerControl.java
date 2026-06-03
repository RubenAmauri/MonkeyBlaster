package mygame;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class PlayerControl extends AbstractControl {

    private int screenWidth, screenHeight;
    public boolean up, down, left, right;
    private float speed = 800f;
    private float lastRotation;
    private ParticleManager particleManager;

    public PlayerControl(int width, int height, ParticleManager particleManager) {
        this.screenWidth      = width;
        this.screenHeight     = height;
        this.particleManager  = particleManager;
    }

    @Override
    protected void controlUpdate(float tpf) {
        float radius = (Float) spatial.getUserData("radius");
        Vector3f pos = spatial.getLocalTranslation();

        float dx = (right ? 1f : 0f) - (left ? 1f : 0f);
        float dy = (up    ? 1f : 0f) - (down ? 1f : 0f);

        // Clamp contra los bordes antes de normalizar
        if (dx > 0 && pos.x >= screenWidth  - radius) dx = 0;
        if (dx < 0 && pos.x <= radius)                dx = 0;
        if (dy > 0 && pos.y >= screenHeight - radius) dy = 0;
        if (dy < 0 && pos.y <= radius)                dy = 0;

        if (dx != 0 || dy != 0) {
            Vector3f moveDir = new Vector3f(dx, dy, 0).normalizeLocal();
            spatial.move(moveDir.mult(speed * tpf));

            float angle = MonkeyBlasterMain.getAngleFromVector(moveDir);
            spatial.rotate(0, 0, -lastRotation + angle);
            lastRotation = angle;

            particleManager.makeExhaustFire(spatial.getLocalTranslation(), lastRotation);
        }
    }

    public void applyGravity(Vector3f gravity) {
        spatial.move(gravity);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public void reset() {
        up = down = left = right = false;
    }
}