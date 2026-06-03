package mygame;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class BulletControl extends AbstractControl {

    private int screenWidth, screenHeight;
    private float speed = 1100f;
    public Vector3f direction;
    private float rotation;
    private ParticleManager particleManager;
    private Grid grid;

    public BulletControl(Vector3f direction, int screenWidth, int screenHeight,
            ParticleManager particleManager, Grid grid) {
        this.direction = direction;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.particleManager = particleManager;
        this.grid = grid;
    }

    @Override
    protected void controlUpdate(float tpf) {
        // Movimiento
        spatial.move(direction.mult(speed * tpf));
        grid.applyExplosiveForce(
        direction.length() * 18f,
        spatial.getLocalTranslation(), 80);

        // Rotación
        float actualRotation = MonkeyBlasterMain.getAngleFromVector(direction);
        if (actualRotation != rotation) {
            spatial.rotate(0, 0, actualRotation - rotation);
            rotation = actualRotation;
        }
       
        // Eliminar y explotar si sale de pantalla
        Vector3f loc = spatial.getLocalTranslation();
        if (loc.x > screenWidth || loc.y > screenHeight
                || loc.x < 0 || loc.y < 0) {
            particleManager.bulletExplosion(loc);
            spatial.removeFromParent();
        }
    }

    public void applyGravity(Vector3f gravity) {
        direction.addLocal(gravity);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
