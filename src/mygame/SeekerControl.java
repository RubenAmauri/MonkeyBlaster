package mygame;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

public class SeekerControl extends AbstractControl {

    private Spatial player;
    private float speed = 800f;
    private Vector3f velocity = new Vector3f();
    private long spawnTime;
    private final Quaternion rot = new Quaternion();

    public SeekerControl(Spatial player) {
        this.player = player;
        this.spawnTime = System.currentTimeMillis();
    }

    @Override
    protected void controlUpdate(float tpf) {
        boolean active = (Boolean) spatial.getUserData("active");
        long elapsed = System.currentTimeMillis() - spawnTime;

        if (!active) {
            // Fade in durante 1 segundo
            if (elapsed >= 250) {
                spatial.setUserData("active", true);
                spatial.setUserData("activeTime", System.currentTimeMillis());
            }
            return;
        }

        // Dirección hacia el jugador
        Vector3f playerPos = player.getLocalTranslation();
        Vector3f myPos     = spatial.getLocalTranslation();
        Vector3f direction = playerPos.subtract(myPos).normalizeLocal();

        // Acelerar hacia el jugador
        velocity.addLocal(direction.mult(speed * tpf));

        // Fricción — limita la velocidad máxima suavemente
        velocity.multLocal(0.98f);

        // Mover
        spatial.move(velocity.mult(tpf));

        // Rotar hacia la dirección de movimiento
        if (velocity.length() > 0.1f) {
            float angle = MonkeyBlasterMain.getAngleFromVector(velocity);
            spatial.setLocalRotation(rot.fromAngles(0, 0, angle));
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public void applyGravity(Vector3f gravity) {
        velocity.addLocal(gravity);
    }
}