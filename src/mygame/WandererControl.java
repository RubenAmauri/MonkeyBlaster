package mygame;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.util.Random;

public class WandererControl extends AbstractControl {

    private Vector3f velocity    = new Vector3f();
    private float direction;
    private int frameCount       = 0;
    private long spawnTime;
    private int screenWidth, screenHeight;
    private static final Random rand = new Random();

    public WandererControl(int screenWidth, int screenHeight) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
        this.spawnTime    = System.currentTimeMillis();
        // Dirección inicial aleatoria en radianes
        this.direction    = rand.nextFloat() * FastMath.TWO_PI;
    }

    @Override
    protected void controlUpdate(float tpf) {
        boolean active = (Boolean) spatial.getUserData("active");
        long elapsed   = System.currentTimeMillis() - spawnTime;

        if (!active) {
            if (elapsed >= 250) {
                spatial.setUserData("active", true);
                spatial.setUserData("activeTime", System.currentTimeMillis());
            }
            return;
        }

        frameCount++;

        // Cada 6 frames ajusta ligeramente la dirección
        if (frameCount % 6 == 0) {
            direction += (rand.nextFloat() - 0.5f) * 0.2f;
            direction  = (direction + FastMath.TWO_PI) % FastMath.TWO_PI;

            // Si está fuera de los límites, redirigir hacia el centro
            Vector3f pos = spatial.getLocalTranslation();
            float margin = 50f;
            if (pos.x < margin || pos.x > screenWidth - margin ||
                pos.y < margin || pos.y > screenHeight - margin) {
                Vector3f center = new Vector3f(screenWidth / 2f, screenHeight / 2f, 0);
                Vector3f toCenter = center.subtract(pos).normalizeLocal();
                direction = MonkeyBlasterMain.getAngleFromVector(toCenter)
                          + (rand.nextFloat() - 0.5f) * FastMath.HALF_PI;
            }
        }

        // Acumular velocidad en la dirección actual
        Vector3f force = MonkeyBlasterMain.getVectorFromAngle(direction).mult(0.4f);
        velocity.addLocal(force);

        // Fricción
        velocity.multLocal(0.98f);

        // Mover
        spatial.move(velocity.mult(tpf * 60f));

        // Rotar levemente en sentido antihorario (efecto visual)
        spatial.rotate(0, 0, -0.05f * tpf * 60f);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public void applyGravity(Vector3f gravity) {
        velocity.addLocal(gravity);
    }
}