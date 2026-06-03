package mygame;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.ui.Picture;

public class ParticleControl extends AbstractControl {

    private Vector3f velocity;
    private float lifespan;
    private long spawnTime;
    private ColorRGBA color;
    private int screenWidth, screenHeight;
    private Picture pic;

    public ParticleControl(Vector3f velocity, float lifespan, ColorRGBA color,
                           int screenWidth, int screenHeight) {
        this.velocity    = velocity;
        this.lifespan    = lifespan;
        this.color       = color.clone();
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
        spawnTime = System.currentTimeMillis();
    }

    @Override
    public void setSpatial(Spatial s) {
        super.setSpatial(s);
        if (s instanceof Node) {
            Node node = (Node) s;
            pic = (Picture) node.getChild(node.getName());
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        // Movimiento
        spatial.move(velocity.mult(tpf * 3f));
        velocity.multLocal(1 - 3f * tpf);
        if (Math.abs(velocity.x) + Math.abs(velocity.y) < 0.001f) {
            velocity.set(0, 0, 0);
        }

        // Rotación hacia la dirección de movimiento
        if (velocity.lengthSquared() > 0) {
            spatial.rotateUpTo(velocity.normalize());
            spatial.rotate(0, 0, FastMath.PI / 2f);
        }

        // Repeler partículas en los bordes de pantalla
        Vector3f loc = spatial.getLocalTranslation();
        if (loc.x < 0) {
            velocity.x = Math.abs(velocity.x);
        } else if (loc.x > screenWidth) {
            velocity.x = -Math.abs(velocity.x);
        }
        if (loc.y < 0) {
            velocity.y = Math.abs(velocity.y);
        } else if (loc.y > screenHeight) {
            velocity.y = -Math.abs(velocity.y);
        }

        // Escala y alpha según velocidad y tiempo de vida
        float speed = velocity.length();
        long difTime = System.currentTimeMillis() - spawnTime;
        float percentLife = 1 - difTime / lifespan;
        float alpha = lesserValue(1.5f, lesserValue(percentLife * 2, speed));
        alpha *= alpha;
        setAlpha(alpha);
        spatial.setLocalScale(0.3f + lesserValue(lesserValue(1.5f, 0.02f * speed + 0.1f), alpha));
        spatial.scale(0.65f);

        // Eliminar si expiró
        if (difTime > lifespan) {
            spatial.removeFromParent();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    private float lesserValue(float a, float b) {
        return a < b ? a : b;
    }

    private void setAlpha(float alpha) {
        color.set(color.r, color.g, color.b, alpha);
        if (pic != null) {
            pic.getMaterial().setColor("Color", color);
        }
    }

    public void applyGravity(Vector3f gravity, float distance) {
        Vector3f additionalVelocity = gravity.mult(1000f / (distance * distance + 10000f));
        velocity.addLocal(additionalVelocity);

        // Componente lateral para espiral hacia el agujero negro
        if (distance < 400) {
            additionalVelocity = new Vector3f(gravity.y, -gravity.x, 0)
                    .mult(3f / (distance + 100));
            velocity.addLocal(additionalVelocity);
        }
    }
}