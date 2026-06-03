package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;

public class BombControl extends AbstractControl {

    private static final float SPEED           = 200f;
    private static final long  EXPLODE_MS      = 1500L;
    private static final long  EXP_FRAME_MS    = 33L;   // ~30 fps
    private static final int   EXP_FRAME_COUNT = 90;
    private static final float INNER_RADIUS    = 100f;
    private static final float OUTER_RADIUS    = 200f;
    private static final long  DEBUG_CIRCLE_MS = 2000L;

    private final AssetManager    assetManager;
    private final Node             guiNode;
    private final ParticleManager  particleManager;
    private final Grid             grid;
    private final Node             blackHoleNode;
    private final Node             enemyNode;
    private final Node             bossNode;
    private final Sound            sound;

    // Dirección de movimiento (igual que BulletControl: vector unitario, gravedad la acumula)
    public Vector3f direction;
    private final long spawnTime;

    // Animación de explosión
    private final Texture2D[] explosionFrames;
    private final AudioNode   explosionSound;
    private Picture           explosionPic     = null;
    private int               expFrame         = 0;
    private long              lastExpFrameTime = 0;

    // Estado
    private boolean  exploded      = false;
    private long     explosionTime = 0;
    private Geometry debugCircle   = null;

    public BombControl(Vector3f direction, AssetManager assetManager,
                       Node guiNode, ParticleManager particleManager,
                       Grid grid, Node blackHoleNode, Node enemyNode,
                       Node bossNode, Sound sound, Texture2D[] explosionFrames) {
        this.direction       = direction.clone();
        this.assetManager    = assetManager;
        this.guiNode         = guiNode;
        this.particleManager = particleManager;
        this.grid            = grid;
        this.blackHoleNode   = blackHoleNode;
        this.enemyNode       = enemyNode;
        this.bossNode        = bossNode;
        this.sound           = sound;
        this.spawnTime       = System.currentTimeMillis();
        this.explosionFrames = explosionFrames; // frames precargados por GameState

        // AudioNode directo para explosion.ogg
        explosionSound = new AudioNode(assetManager,
                "Sounds/Explosions/explosion.ogg", DataType.Buffer);
        explosionSound.setPositional(false);
        explosionSound.setLooping(false);
        explosionSound.setVolume(0.9f);
    }

    @Override
    protected void controlUpdate(float tpf) {
        long now = System.currentTimeMillis();

        if (!exploded) {
            spatial.move(direction.x * SPEED * tpf, direction.y * SPEED * tpf, 0f);

            if (now - spawnTime >= EXPLODE_MS) {
                explode(now);
            }
        } else {
            updateExplosion(now);
        }
    }

    private void explode(long now) {
        exploded      = true;
        explosionTime = now;

        Vector3f pos = spatial.getLocalTranslation().clone();

        // Ocultar sprite de la bomba
        ((Node) spatial).getChild(0).setCullHint(Spatial.CullHint.Always);

        // Sonido de explosión directamente con AudioNode
        explosionSound.playInstance();

        // Daño en área
        applyBlastDamage(pos);

        // Deformar el grid
        grid.applyExplosiveForce(500f, pos, OUTER_RADIUS);

        // Picture de la animación de explosión (160×120, centrada en la posición)
        explosionPic = new Picture("BombExplosion");
        explosionPic.setTexture(assetManager, explosionFrames[0], true);
        explosionPic.setWidth(160f);
        explosionPic.setHeight(120f);
        explosionPic.setLocalTranslation(pos.x - 80f, pos.y - 60f, 0.1f);
        explosionPic.getMaterial().getAdditionalRenderState()
                .setBlendMode(BlendMode.AlphaAdditive);
        guiNode.attachChild(explosionPic);
        lastExpFrameTime = now;

        // Círculo de debug: radio 200px, rojo semitransparente
        debugCircle = createDebugCircle(OUTER_RADIUS);
        debugCircle.setLocalTranslation(pos.x, pos.y, 0.05f);
        guiNode.attachChild(debugCircle);
    }

    private void updateExplosion(long now) {
        // Avanzar frames de animación a ~30 fps
        if (explosionPic != null && now - lastExpFrameTime >= EXP_FRAME_MS) {
            expFrame++;
            lastExpFrameTime = now;
            if (expFrame >= EXP_FRAME_COUNT) {
                guiNode.detachChild(explosionPic);
                explosionPic = null;
            } else {
                explosionPic.getMaterial().setTexture("Texture", explosionFrames[expFrame]);
            }
        }

        // Eliminar círculo de debug tras 2 segundos
        if (debugCircle != null && now - explosionTime >= DEBUG_CIRCLE_MS) {
            guiNode.detachChild(debugCircle);
            debugCircle = null;
        }

        // Retirar la bomba del grafo de escena cuando todos los efectos han terminado
        if (explosionPic == null && debugCircle == null) {
            spatial.removeFromParent();
        }
    }

    private void applyBlastDamage(Vector3f pos) {
        // Black holes: destruir todos en cualquier radio
        for (int i = blackHoleNode.getQuantity() - 1; i >= 0; i--) {
            Spatial bh = blackHoleNode.getChild(i);
            if (bh.getLocalTranslation().distance(pos) <= OUTER_RADIUS) {
                particleManager.blackHoleExplosion(bh.getLocalTranslation());
                blackHoleNode.detachChildAt(i);
            }
        }

        // Jefe: daño según distancia (30 en radio interior, 15 en radio exterior)
        if (bossNode.getQuantity() > 0) {
            Spatial boss = bossNode.getChild(0);
            float d = boss.getLocalTranslation().distance(pos);
            if (d <= OUTER_RADIUS) {
                BossControl bc = boss.getControl(BossControl.class);
                if (bc != null) {
                    bc.takeDamage(d <= INNER_RADIUS ? 30 : 15);
                }
            }
        }

        // Enemigos normales: destruir todos en cualquier radio
        for (int i = enemyNode.getQuantity() - 1; i >= 0; i--) {
            Spatial enemy = enemyNode.getChild(i);
            if (enemy.getLocalTranslation().distance(pos) <= OUTER_RADIUS) {
                particleManager.enemyExplosion(enemy.getLocalTranslation());
                enemyNode.detachChildAt(i);
            }
        }
    }

    private Geometry createDebugCircle(float radius) {
        int      segments = 48;
        Vector3f[] verts  = new Vector3f[segments];
        int[]    indices  = new int[segments * 2];

        for (int i = 0; i < segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            verts[i] = new Vector3f(
                    (float) Math.cos(angle) * radius,
                    (float) Math.sin(angle) * radius,
                    0f);
            indices[i * 2]     = i;
            indices[i * 2 + 1] = (i + 1) % segments;
        }

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Lines);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(verts));
        mesh.setBuffer(VertexBuffer.Type.Index,    1, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();

        Geometry geom = new Geometry("bombRadius", mesh);
        Material mat  = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1f, 0f, 0f, 0.5f));
        mat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
        geom.setMaterial(mat);
        return geom;
    }

    // Igual que BulletControl: la gravedad de un black hole acumula en la dirección
    public void applyGravity(Vector3f gravity) {
        direction.addLocal(gravity);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}
