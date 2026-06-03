package mygame;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class BossLaserControl extends AbstractControl {

    private static final float SPEED = 300f;
    private final int screenWidth, screenHeight;
    public Vector3f direction;
    private float rotation;

    public BossLaserControl(Vector3f direction, int screenWidth, int screenHeight) {
        this.direction   = direction.normalizeLocal();
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
    }

    @Override
    protected void controlUpdate(float tpf) {
        spatial.move(direction.mult(SPEED * tpf));

        float angle = MonkeyBlasterMain.getAngleFromVector(direction);
        if (angle != rotation) {
            spatial.rotate(0, 0, angle - rotation);
            rotation = angle;
        }

        Vector3f loc = spatial.getLocalTranslation();
        if (loc.x < 0 || loc.x > screenWidth || loc.y < 0 || loc.y > screenHeight) {
            spatial.removeFromParent();
        }
    }

    public void applyGravity(Vector3f gravity) {
        direction.addLocal(gravity);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}
