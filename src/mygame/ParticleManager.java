package mygame;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.Random;

public class ParticleManager {

    private Node guiNode;
    private Spatial standardParticle;
    private Spatial glowParticle;
    private Node particleNode;
    private Random rand;
    private int screenWidth, screenHeight;
    private long spawnTime;

    public ParticleManager(Node guiNode, Spatial standardParticle,
                           Spatial glowParticle, int screenWidth, int screenHeight) {
        this.guiNode          = guiNode;
        this.standardParticle = standardParticle;
        this.glowParticle     = glowParticle;
        this.screenWidth      = screenWidth;
        this.screenHeight     = screenHeight;
        particleNode = new Node("particles");
        guiNode.attachChild(particleNode);
        rand = new Random();
        spawnTime = System.currentTimeMillis();
    }

    // --- Explosión de enemigo: 120 partículas con colores aleatorios ---
    public void enemyExplosion(Vector3f position) {
        float hue1 = rand.nextFloat() * 6;
        float hue2 = (rand.nextFloat() * 2) % 6f;
        ColorRGBA color1 = hsvToColor(hue1, 0.5f, 1f);
        ColorRGBA color2 = hsvToColor(hue2, 0.5f, 1f);

        for (int i = 0; i < 120; i++) {
            Vector3f velocity = getRandomVelocity(250);
            Spatial particle  = standardParticle.clone();
            particle.setLocalTranslation(position);
            ColorRGBA color = new ColorRGBA();
            color.interpolateLocal(color1, color2, rand.nextFloat() * 0.5f);
            particle.addControl(new ParticleControl(velocity, 3100, color,
                    screenWidth, screenHeight));
            particle.setUserData("affectedByGravity", true);
            particleNode.attachChild(particle);
        }
    }

    // --- Explosión de bala: 30 partículas azules ---
    public void bulletExplosion(Vector3f position) {
        for (int i = 0; i < 30; i++) {
            Vector3f velocity = getRandomVelocity(175);
            Spatial particle  = standardParticle.clone();
            particle.setLocalTranslation(position);
            ColorRGBA color = new ColorRGBA(0.676f, 0.844f, 0.898f, 1);
            particle.addControl(new ParticleControl(velocity, 1000, color,
                    screenWidth, screenHeight));
            particle.setUserData("affectedByGravity", true);
            particleNode.attachChild(particle);
        }
    }

    // --- Explosión del jugador: 1200 partículas blanco/amarillo ---
    public void playerExplosion(Vector3f position) {
        ColorRGBA color1 = ColorRGBA.White;
        ColorRGBA color2 = ColorRGBA.Yellow;

        for (int i = 0; i < 1200; i++) {
            Vector3f velocity = getRandomVelocity(1000);
            Spatial particle  = standardParticle.clone();
            particle.setLocalTranslation(position);
            ColorRGBA color = new ColorRGBA();
            color.interpolateLocal(color1, color2, rand.nextFloat());
            particle.addControl(new ParticleControl(velocity, 2800, color,
                    screenWidth, screenHeight));
            particle.setUserData("affectedByGravity", true);
            particleNode.attachChild(particle);
        }
    }

    // --- Explosión de agujero negro al recibir impacto ---
    public void blackHoleExplosion(Vector3f position) {
        float hue = ((System.currentTimeMillis() - spawnTime) * 0.003f) % 6f;
        int numParticles = 150;
        ColorRGBA color = hsvToColor(hue, 0.25f, 1);
        float startOffset = rand.nextFloat() * FastMath.PI * 2 / numParticles;

        for (int i = 0; i < numParticles; i++) {
            float alpha = FastMath.PI * 2 * i / numParticles + startOffset;
            Vector3f velocity = MonkeyBlasterMain.getVectorFromAngle(alpha)
                    .multLocal(rand.nextFloat() * 200 + 300);
            Vector3f pos = position.add(velocity.mult(0.1f));
            Spatial particle = standardParticle.clone();
            particle.setLocalTranslation(pos);
            particle.addControl(new ParticleControl(velocity, 1000, color,
                    screenWidth, screenHeight));
            particle.setUserData("affectedByGravity", false);
            particleNode.attachChild(particle);
        }
    }

    // --- Partículas del escape del jugador ---
    public void makeExhaustFire(Vector3f position, float rotation) {
        ColorRGBA midColor  = new ColorRGBA(1f, 0.73f, 0.12f, 0.7f);
        ColorRGBA sideColor = new ColorRGBA(0.78f, 0.15f, 0.04f, 0.7f);
        Vector3f direction  = MonkeyBlasterMain.getVectorFromAngle(rotation);
        float t = (System.currentTimeMillis() - spawnTime) / 1000f;
        Vector3f baseVel = direction.mult(-45f);
        Vector3f perpVel = new Vector3f(baseVel.y, -baseVel.x, 0)
                .multLocal(2f * FastMath.sin(t * 10f));
        Vector3f pos = position.add(
                MonkeyBlasterMain.getVectorFromAngle(rotation).multLocal(-25f));

        // Stream central
        Vector3f velMid = baseVel.add(
                MonkeyBlasterMain.getVectorFromAngle(rand.nextFloat() * FastMath.PI * 2)
                .mult(7.5f));
        spawnExhaustParticle(standardParticle, pos, velMid, midColor);
        spawnExhaustParticle(glowParticle,     pos, velMid, midColor);

        // Streams laterales
        Vector3f velSide1 = baseVel.add(
                MonkeyBlasterMain.getVectorFromAngle(rand.nextFloat() * FastMath.PI * 2)
                .mult(2.4f)).addLocal(perpVel);
        Vector3f velSide2 = baseVel.add(
                MonkeyBlasterMain.getVectorFromAngle(rand.nextFloat() * FastMath.PI * 2)
                .mult(2.4f)).subtractLocal(perpVel);
        spawnExhaustParticle(standardParticle, pos, velSide1, sideColor);
        spawnExhaustParticle(standardParticle, pos, velSide2, sideColor);
        spawnExhaustParticle(glowParticle,     pos, velSide1, sideColor);
        spawnExhaustParticle(glowParticle,     pos, velSide2, sideColor);
    }

    private void spawnExhaustParticle(Spatial template, Vector3f pos,
                                       Vector3f vel, ColorRGBA color) {
        Spatial p = template.clone();
        p.setLocalTranslation(pos);
        p.addControl(new ParticleControl(vel, 800, color, screenWidth, screenHeight));
        p.setUserData("affectedByGravity", true);
        particleNode.attachChild(p);
    }

    // --- Partículas del agujero negro (spray continuo) ---
    public void sprayParticle(Vector3f position, Vector3f sprayVel) {
        Spatial particle = standardParticle.clone();
        particle.setLocalTranslation(position);
        ColorRGBA color = new ColorRGBA(0.8f, 0.4f, 0.8f, 1f);
        particle.addControl(new ParticleControl(sprayVel, 3500, color,
                screenWidth, screenHeight));
        particle.setUserData("affectedByGravity", true);
        particleNode.attachChild(particle);
    }

    // --- Helpers ---
    private Vector3f getRandomVelocity(float max) {
        Vector3f velocity = new Vector3f(
                rand.nextFloat() - 0.5f,
                rand.nextFloat() - 0.5f,
                0).normalizeLocal();
        float random = rand.nextFloat() * 5 + 1;
        float particleSpeed = max * (1f - 0.6f / random);
        velocity.multLocal(particleSpeed);
        return velocity;
    }

    public ColorRGBA hsvToColor(float h, float s, float v) {
        if (h == 0 && s == 0) return new ColorRGBA(v, v, v, 1);
        float c = s * v;
        float x = c * (1 - Math.abs(h % 2 - 1));
        float m = v - c;
        if      (h < 1) return new ColorRGBA(c+m, x+m, m,   1);
        else if (h < 2) return new ColorRGBA(x+m, c+m, m,   1);
        else if (h < 3) return new ColorRGBA(m,   c+m, x+m, 1);
        else if (h < 4) return new ColorRGBA(m,   x+m, c+m, 1);
        else if (h < 5) return new ColorRGBA(x+m, m,   c+m, 1);
        else            return new ColorRGBA(c+m, m,   x+m, 1);
    }

    public Node getParticleNode() { return particleNode; }
}