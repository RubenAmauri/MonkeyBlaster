package mygame;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import com.jme3.scene.control.AbstractControl;
import java.util.Random;

public class BlackHoleControl extends AbstractControl {

    private long spawnTime;
    private int hitpoints;
    private ParticleManager particleManager;
    private long lastSprayTime;
    private float sprayAngle;
    private Random rand = new Random();
    private Grid grid;

    public BlackHoleControl(ParticleManager particleManager, Grid grid) {
        this.particleManager = particleManager;
        this.grid = grid;
        spawnTime = System.currentTimeMillis();
        hitpoints = 10;
        sprayAngle = 0;
    }

    @Override
    protected void controlUpdate(float tpf) {
        boolean active = (Boolean) spatial.getUserData("active");

        if (active) {
            // Spray de partículas púrpuras en intervalos
            long sprayDif = System.currentTimeMillis() - lastSprayTime;
            if ((System.currentTimeMillis() / 250) % 2 == 0 && sprayDif > 20) {
                lastSprayTime = System.currentTimeMillis();
                Vector3f sprayVel = MonkeyBlasterMain.getVectorFromAngle(sprayAngle)
                        .mult(rand.nextFloat() * 3 + 6);
                Vector3f randVec = MonkeyBlasterMain.getVectorFromAngle(
                        rand.nextFloat() * FastMath.PI * 2);
                randVec.multLocal(4 + rand.nextFloat() * 4);
                Vector3f position = spatial.getLocalTranslation()
                        .add(sprayVel.mult(2f)).addLocal(randVec);
                particleManager.sprayParticle(position, sprayVel.mult(30f));
            }
            sprayAngle -= FastMath.PI * tpf / 10f;
            grid.applyImplosiveForce(
                    FastMath.sin(sprayAngle / 2) * 10 + 20,
                    spatial.getLocalTranslation(), 250);

        } else {
            // Fade in durante 1 segundo
            long dif = System.currentTimeMillis() - spawnTime;
            if (dif >= 1000) {
                spatial.setUserData("active", true);
            }

            // Transparencia progresiva
            float alpha = dif / 1000f;
            ColorRGBA color = new ColorRGBA(1, 1, 1, alpha);
            Node spatialNode = (Node) spatial;
            Picture pic = (Picture) spatialNode.getChild("BlackHole");
            if (pic != null) {
                pic.getMaterial().setColor("Color", color);
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public void wasShot() {
        hitpoints--;
    }

    public boolean isDead() {
        return hitpoints <= 0;
    }
}
