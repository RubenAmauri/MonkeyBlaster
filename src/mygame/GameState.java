package mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.util.Random;

public class GameState extends AbstractAppState
        implements ActionListener, AnalogListener {

    private SimpleApplication app;
    private Node guiNode;
    private int screenWidth, screenHeight;

    private Spatial player;
    private Node bulletNode;
    private Node enemyNode;
    private Node blackHoleNode;
    private Node bossNode;
    private Node bossLaserNode;

    private ParticleManager particleManager;
    private Hud hud;
    private Grid grid;
    private Sound sound;

    private boolean gameOver = false;
    private boolean paused = false;
    private long bulletCooldown = 0;
    private long spawnCooldown = 0;
    private long spawnCooldownBlackHole = 0;
    private float inverseSpawnChance = 30f;
    private static final Random rand = new Random();

    // Jefe
    private int lastBossScore = 0;

    // Modo debug (empieza con 49 puntos para testear el jefe)
    private final boolean debugMode;

    public GameState() {
        this.debugMode = false;
    }

    public GameState(boolean debugMode) {
        this.debugMode = debugMode;
    }

    // -------------------------------------------------------
    // Ciclo de vida AppState
    // -------------------------------------------------------
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.guiNode = this.app.getGuiNode();
        screenWidth = this.app.getContext().getSettings().getWidth();
        screenHeight = this.app.getContext().getSettings().getHeight();

        setupScene();
        setupInput();
    }

    private void setupScene() {
        grid = new Grid(screenWidth, screenHeight,
                new Vector2f(25, 25), guiNode, app.getAssetManager());

        bulletNode = new Node("bullets");
        enemyNode = new Node("enemies");
        blackHoleNode = new Node("blackholes");
        bossNode = new Node("boss");
        bossLaserNode = new Node("bossLasers");
        guiNode.attachChild(bulletNode);
        guiNode.attachChild(enemyNode);
        guiNode.attachChild(blackHoleNode);
        guiNode.attachChild(bossNode);
        guiNode.attachChild(bossLaserNode);

        particleManager = new ParticleManager(guiNode,
                getSpatial("laser_particle"), getSpatial("laser_particle"),
                screenWidth, screenHeight);

        player = getSpatial("Player");
        player.setUserData("alive", true);
        player.move(screenWidth / 2f, screenHeight / 2f, 0);
        guiNode.attachChild(player);
        player.addControl(new PlayerControl(screenWidth, screenHeight, particleManager));

        hud = new Hud(app.getAssetManager(), guiNode, screenWidth, screenHeight);
        hud.reset();

        if (debugMode) {
            hud.score = 49;
            hud.forceUpdateDisplay();
        }

        sound = new Sound(app.getAssetManager());
        sound.playMusic();

        app.getInputManager().setMouseCursor(
                (JmeCursor) app.getAssetManager().loadAsset("Textures/Cursor.ico"));
    }

    private void setupInput() {
        app.getInputManager().addMapping("moveLeft",
                new KeyTrigger(KeyInput.KEY_LEFT), new KeyTrigger(KeyInput.KEY_A));
        app.getInputManager().addMapping("moveRight",
                new KeyTrigger(KeyInput.KEY_RIGHT), new KeyTrigger(KeyInput.KEY_D));
        app.getInputManager().addMapping("moveUp",
                new KeyTrigger(KeyInput.KEY_UP), new KeyTrigger(KeyInput.KEY_W));
        app.getInputManager().addMapping("moveDown",
                new KeyTrigger(KeyInput.KEY_DOWN), new KeyTrigger(KeyInput.KEY_S));
        app.getInputManager().addMapping("shoot",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addMapping("pause",
                new KeyTrigger(KeyInput.KEY_P));
        app.getInputManager().addListener(this,
                "moveLeft", "moveRight", "moveUp", "moveDown", "shoot", "pause");
    }

    @Override
    public void update(float tpf) {
        grid.update(tpf);

        if (paused) {
            return;
        }

        hud.update();
        spawnEnemies();
        spawnBlackHoles();
        activateWallWanderers();

        // Spawn del jefe cada 50 puntos si no hay uno activo
        if (!gameOver && bossNode.getQuantity() == 0
                && hud.score >= lastBossScore + 50) {
            spawnBoss();
        }

        handleCollisions();
        handleGravity(tpf);

        // Fases del jefe según HP
        if (bossNode.getQuantity() > 0) {
            BossControl bc = bossNode.getChild(0).getControl(BossControl.class);
            if (bc != null) {
                int hp = bc.getHp();
                if (hp <= 160 && blackHoleNode.getQuantity() < 2) spawnBossBlackHole();
                if (hp <= 40  && blackHoleNode.getQuantity() < 3) spawnBossBlackHole();
            }
        }

        if (!(Boolean) player.getUserData("alive")) {
            long dieTime = (Long) player.getUserData("dieTime");
            if (System.currentTimeMillis() - dieTime > 3000f && !gameOver) {
                respawnPlayer();
            }
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();

        app.getInputManager().removeListener(this);
        app.getInputManager().deleteMapping("moveLeft");
        app.getInputManager().deleteMapping("moveRight");
        app.getInputManager().deleteMapping("moveUp");
        app.getInputManager().deleteMapping("moveDown");
        app.getInputManager().deleteMapping("shoot");
        app.getInputManager().deleteMapping("pause");

        app.getInputManager().setMouseCursor(null);

        sound.stopMusic();
        sound.stopBossMusic();

        hud.cleanup();
        guiNode.detachChild(grid.getNode());
        guiNode.detachChild(player);
        guiNode.detachChild(bulletNode);
        guiNode.detachChild(enemyNode);
        guiNode.detachChild(blackHoleNode);
        guiNode.detachChild(bossNode);
        guiNode.detachChild(bossLaserNode);
        guiNode.detachChild(particleManager.getParticleNode());
    }

    // -------------------------------------------------------
    // Input
    // -------------------------------------------------------
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("pause") && isPressed && !paused) {
            setPaused(true);
            app.getStateManager().attach(new PauseState(this));
            return;
        }

        if (paused || !(Boolean) player.getUserData("alive")) {
            return;
        }

        PlayerControl pc = player.getControl(PlayerControl.class);
        switch (name) {
            case "moveUp":
                pc.up = isPressed;
                break;
            case "moveDown":
                pc.down = isPressed;
                break;
            case "moveLeft":
                pc.left = isPressed;
                break;
            case "moveRight":
                pc.right = isPressed;
                break;
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (paused || !(Boolean) player.getUserData("alive")) {
            return;
        }

        if (name.equals("shoot")
                && System.currentTimeMillis() - bulletCooldown > 83f) {
            bulletCooldown = System.currentTimeMillis();

            Vector3f aim = getAimDirection();
            Vector3f offset = new Vector3f(aim.y / 3, -aim.x / 3, 0);

            Spatial b1 = getSpatial("Bullet");
            b1.setLocalTranslation(player.getLocalTranslation().add(aim.add(offset).mult(30)));
            b1.addControl(new BulletControl(aim, screenWidth, screenHeight, particleManager, grid));
            bulletNode.attachChild(b1);

            Spatial b2 = getSpatial("Bullet");
            b2.setLocalTranslation(player.getLocalTranslation().add(aim.add(offset.negate()).mult(30)));
            b2.addControl(new BulletControl(aim, screenWidth, screenHeight, particleManager, grid));
            bulletNode.attachChild(b2);

            sound.shot();
        }
    }

    // -------------------------------------------------------
    // Pausa — congela/descongela todos los controles activos
    // -------------------------------------------------------
    public void setPaused(boolean paused) {
        this.paused = paused;

        PlayerControl pc = player.getControl(PlayerControl.class);
        pc.setEnabled(!paused);
        if (paused) {
            pc.reset();
        }

        for (int i = 0; i < enemyNode.getQuantity(); i++) {
            setEnemyEnabled(enemyNode.getChild(i), !paused);
        }

        for (int i = 0; i < bulletNode.getQuantity(); i++) {
            BulletControl bc = bulletNode.getChild(i).getControl(BulletControl.class);
            if (bc != null) {
                bc.setEnabled(!paused);
            }
        }
        for (int i = 0; i < blackHoleNode.getQuantity(); i++) {
            BlackHoleControl bhc = blackHoleNode.getChild(i).getControl(BlackHoleControl.class);
            if (bhc != null) {
                bhc.setEnabled(!paused);
            }
        }
        for (int i = 0; i < bossNode.getQuantity(); i++) {
            BossControl bosc = bossNode.getChild(i).getControl(BossControl.class);
            if (bosc != null) {
                bosc.setEnabled(!paused);
            }
        }
        for (int i = 0; i < bossLaserNode.getQuantity(); i++) {
            BossLaserControl blc = bossLaserNode.getChild(i).getControl(BossLaserControl.class);
            if (blc != null) {
                blc.setEnabled(!paused);
            }
        }
        Node particleNode = particleManager.getParticleNode();
        for (int i = 0; i < particleNode.getQuantity(); i++) {
            ParticleControl ptc = particleNode.getChild(i).getControl(ParticleControl.class);
            if (ptc != null) {
                ptc.setEnabled(!paused);
            }
        }
    }

    private void setEnemyEnabled(Spatial enemy, boolean enabled) {
        SeekerControl sc = enemy.getControl(SeekerControl.class);
        if (sc != null) {
            sc.setEnabled(enabled);
            return;
        }
        WandererControl wc = enemy.getControl(WandererControl.class);
        if (wc != null) {
            wc.setEnabled(enabled);
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private Vector3f getAimDirection() {
        Vector2f mouse = app.getInputManager().getCursorPosition();
        Vector3f playerPos = player.getLocalTranslation();
        return new Vector3f(mouse.x - playerPos.x,
                mouse.y - playerPos.y, 0).normalizeLocal();
    }

    // Sprite estándar con textura _SinFondo.png
    private Spatial getSpatial(String name) {
        Node node = new Node(name);

        Picture pic = new Picture(name);
        Texture2D tex = (Texture2D) app.getAssetManager()
                .loadTexture("Textures/" + name + "_SinFondo.png");
        pic.setTexture(app.getAssetManager(), tex, true);

        float w = tex.getImage().getWidth();
        float h = tex.getImage().getHeight();
        pic.setWidth(w);
        pic.setHeight(h);
        pic.move(-w / 2f, -h / 2f, 0);

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Gui/Gui.j3md");
        mat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
        node.setMaterial(mat);
        node.setUserData("radius", w / 2f);
        node.attachChild(pic);
        return node;
    }

    private Spatial getBossSpatial() {
        Texture2D tex = (Texture2D) app.getAssetManager()
                .loadTexture("Textures/boss_1_SinFondo.png");

        Picture pic = new Picture("bossSprite");
        pic.setTexture(app.getAssetManager(), tex, true);
        pic.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);

        float w = tex.getImage().getWidth();
        float h = tex.getImage().getHeight();
        pic.setWidth(w);
        pic.setHeight(h);
        pic.move(-w / 2f, -h / 2f, 0);

        Node node = new Node("boss");
        node.attachChild(pic);
        node.setUserData("radius", w / 2f);
        return node;
    }

    // -------------------------------------------------------
    // Spawn
    // -------------------------------------------------------
    private void spawnEnemies() {
        if (!(Boolean) player.getUserData("alive")) {
            return;
        }

        if (System.currentTimeMillis() - spawnCooldown > 83f) {
            spawnCooldown = System.currentTimeMillis();
            if (rand.nextInt((int) inverseSpawnChance) == 0) {
                createEnemy("Seeker");
            }
            if (rand.nextInt((int) inverseSpawnChance) == 0) {
                createEnemy("Wanderer");
            }
        }
        if (inverseSpawnChance > 5f) {
            inverseSpawnChance -= 0.005f;
        }
    }

    private void createEnemy(String type) {
        Spatial enemy = getSpatial(type);
        enemy.setLocalTranslation(getSpawnPosition());
        enemy.setUserData("active", false);
        enemy.addControl(type.equals("Seeker")
                ? new SeekerControl(player)
                : new WandererControl(screenWidth, screenHeight));
        enemyNode.attachChild(enemy);
        sound.spawn();
    }

    private void spawnBlackHoles() {
        if (blackHoleNode.getQuantity() >= 2) {
            return;
        }
        if (System.currentTimeMillis() - spawnCooldownBlackHole > 10f) {
            spawnCooldownBlackHole = System.currentTimeMillis();
            if (rand.nextInt(1000) == 0) {
                createBlackHole(getSpawnPosition());
            }
        }
    }

    private void createBlackHole(Vector3f pos) {
        Spatial bh = getSpatial("BlackHole");
        bh.setLocalTranslation(pos);
        bh.addControl(new BlackHoleControl(particleManager, grid));
        bh.setUserData("active", false);
        blackHoleNode.attachChild(bh);
        sound.spawn();
    }

    private void spawnBoss() {
        lastBossScore = hud.score;

        Spatial boss = getBossSpatial();
        boss.setLocalTranslation(screenWidth / 2f, screenHeight / 2f, 0);
        boss.addControl(new BossControl(player, bossLaserNode,
                app.getAssetManager(), screenWidth, screenHeight));
        bossNode.attachChild(boss);

        // Pared de 32 Wanderers estáticos en cuadrado 600x600 centrado en pantalla
        float cx = screenWidth / 2f;
        float cy = screenHeight / 2f;
        float half = 300f;
        float step = 600f / 9f; // 8 puntos sin esquinas por lado
        for (int k = 1; k <= 8; k++) {
            float off = -half + step * k;
            spawnWallWanderer(cx + off, cy - half); // lado inferior
            spawnWallWanderer(cx + half, cy + off);  // lado derecho
            spawnWallWanderer(cx + off, cy + half); // lado superior
            spawnWallWanderer(cx - half, cy + off);  // lado izquierdo
        }

        spawnBossBlackHole();
        sound.playBossMusic();
    }

    private void spawnBossBlackHole() {
        float cx = screenWidth  / 2f;
        float cy = screenHeight / 2f;

        // Cuenta los Wanderers de la pared disponibles
        int wallCount = 0;
        for (int i = 0; i < enemyNode.getQuantity(); i++) {
            if (Boolean.TRUE.equals(enemyNode.getChild(i).getUserData("bossWall"))) wallCount++;
        }
        if (wallCount == 0) return;

        for (int attempt = 0; attempt < 10; attempt++) {
            // Elige un Wanderer de la pared al azar
            int pick = rand.nextInt(wallCount);
            int count = 0;
            Vector3f wp = null;
            for (int i = 0; i < enemyNode.getQuantity(); i++) {
                Spatial e = enemyNode.getChild(i);
                if (Boolean.TRUE.equals(e.getUserData("bossWall"))) {
                    if (count == pick) { wp = e.getLocalTranslation().clone(); break; }
                    count++;
                }
            }
            if (wp == null) continue;

            // 40px hacia adentro del cuadrado desde el Wanderer
            Vector3f dir   = new Vector3f(cx - wp.x, cy - wp.y, 0f).normalizeLocal();
            Vector3f bhPos = wp.add(dir.mult(40f));

            // Verifica que no haya otro black hole a menos de 100px
            boolean tooClose = false;
            for (int i = 0; i < blackHoleNode.getQuantity(); i++) {
                if (blackHoleNode.getChild(i).getLocalTranslation()
                        .distanceSquared(bhPos) < 100f * 100f) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                createBlackHole(bhPos);
                return;
            }
        }
    }

    private void spawnWallWanderer(float x, float y) {
        Spatial w = getSpatial("Wanderer");
        w.setLocalTranslation(x, y, 0);
        w.setUserData("active", false);
        w.setUserData("radius", 20f);
        w.setUserData("bossWall", true);
        w.setUserData("spawnTime", System.currentTimeMillis());
        enemyNode.attachChild(w);
    }

    private void activateWallWanderers() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < enemyNode.getQuantity(); i++) {
            Spatial e = enemyNode.getChild(i);
            if (!Boolean.TRUE.equals(e.getUserData("bossWall"))) continue;
            if ((Boolean) e.getUserData("active")) continue;
            Long spawnTime = e.getUserData("spawnTime");
            if (spawnTime != null && now - spawnTime >= 1000) {
                e.setUserData("active", true);
            }
        }
    }

    private Vector3f getSpawnPosition() {
        Vector3f pos;
        do {
            pos = new Vector3f(rand.nextInt(screenWidth), rand.nextInt(screenHeight), 0);
        } while (pos.distanceSquared(player.getLocalTranslation()) < 250 * 250);
        return pos;
    }

    // -------------------------------------------------------
    // Muerte del jefe — llamado desde handleCollisions()
    // -------------------------------------------------------
    public void onBossDeath(Vector3f position) {
        Vector3f p = position;
        particleManager.playerExplosion(p);
        particleManager.playerExplosion(p.add(30, 20, 0));
        particleManager.playerExplosion(p.add(-25, -15, 0));

        bossNode.detachAllChildren();
        for (int i = enemyNode.getQuantity() - 1; i >= 0; i--) {
            Spatial wall = enemyNode.getChild(i);
            if (Boolean.TRUE.equals(wall.getUserData("bossWall"))) {
                particleManager.enemyExplosion(wall.getLocalTranslation());
                enemyNode.detachChildAt(i);
            }
        }
        blackHoleNode.detachAllChildren();
        lastBossScore = hud.score;

        sound.explosion();
        sound.explosion();
        sound.stopBossMusic();
        sound.playMusic();
    }

    // -------------------------------------------------------
    // Colisiones
    // -------------------------------------------------------
    private void handleCollisions() {
        float playerRadius = (Float) player.getUserData("radius");

        // Bala vs Enemigo
        for (int i = enemyNode.getQuantity() - 1; i >= 0; i--) {
            Spatial enemy = enemyNode.getChild(i);
            if (!(Boolean) enemy.getUserData("active")) continue;
            if (Boolean.TRUE.equals(enemy.getUserData("bossWall"))) continue;
            float er = (Float) enemy.getUserData("radius");

            for (int j = bulletNode.getQuantity() - 1; j >= 0; j--) {
                Spatial bullet = bulletNode.getChild(j);
                if (checkCollision(enemy, er, bullet, (Float) bullet.getUserData("radius"))) {
                    hud.addPoints(enemy.getName().equals("Seeker") ? 2 : 1);
                    particleManager.enemyExplosion(enemy.getLocalTranslation());
                    bulletNode.detachChildAt(j);
                    enemyNode.detachChildAt(i);
                    sound.explosion();
                    break;
                }
            }
        }

        // Bala vs Jefe
        for (int i = bossNode.getQuantity() - 1; i >= 0; i--) {
            Spatial boss = bossNode.getChild(i);
            float bossR = (Float) boss.getUserData("radius");

            for (int j = bulletNode.getQuantity() - 1; j >= 0; j--) {
                Spatial bullet = bulletNode.getChild(j);
                if (checkCollision(boss, bossR, bullet, (Float) bullet.getUserData("radius"))) {
                    bulletNode.detachChildAt(j);
                    BossControl bc = boss.getControl(BossControl.class);
                    bc.wasShot();
                    if (bc.isDead()) {
                        hud.addPoints(50);
                        onBossDeath(boss.getLocalTranslation().clone());
                    }
                    break;
                }
            }
        }

        // Láser del jefe vs Jugador
        if ((Boolean) player.getUserData("alive")) {
            for (int i = bossLaserNode.getQuantity() - 1; i >= 0; i--) {
                Spatial laser = bossLaserNode.getChild(i);
                if (checkCollision(player, playerRadius, laser,
                        (Float) laser.getUserData("radius"))) {
                    bossLaserNode.detachChildAt(i);
                    killPlayer();
                    break;
                }
            }
        }

        // Jugador vs Enemigo
        if ((Boolean) player.getUserData("alive")) {
            for (int i = 0; i < enemyNode.getQuantity(); i++) {
                Spatial enemy = enemyNode.getChild(i);
                if (!(Boolean) enemy.getUserData("active")) {
                    continue;
                }
                if (checkCollision(player, playerRadius, enemy,
                        (Float) enemy.getUserData("radius"))) {
                    killPlayer();
                    break;
                }
            }
        }

        // Enemigo vs Enemigo
        for (int i = 0; i < enemyNode.getQuantity(); i++) {
            for (int j = i + 1; j < enemyNode.getQuantity(); j++) {
                Spatial a = enemyNode.getChild(i);
                Spatial b = enemyNode.getChild(j);
                if (checkCollision(a, (Float) a.getUserData("radius"),
                        b, (Float) b.getUserData("radius"))) {
                    pushApart(a, b);
                }
            }
        }

        // Black Hole vs todo
        boolean playerKilled = false;
        for (int i = blackHoleNode.getQuantity() - 1; i >= 0; i--) {
            Spatial bh = blackHoleNode.getChild(i);
            if (!(Boolean) bh.getUserData("active")) {
                continue;
            }
            float bhr = (Float) bh.getUserData("radius");

            if (checkCollision(player, playerRadius, bh, bhr)) {
                playerKilled = true;
                break;
            }

            for (int j = enemyNode.getQuantity() - 1; j >= 0; j--) {
                Spatial enemy = enemyNode.getChild(j);
                if (!(Boolean) enemy.getUserData("active")) {
                    continue;
                }
                Long activeTime = enemy.getUserData("activeTime");
                if (activeTime == null || System.currentTimeMillis() - activeTime < 500) {
                    continue;
                }
                if (checkCollision(enemy, (Float) enemy.getUserData("radius"), bh, bhr)) {
                    particleManager.enemyExplosion(enemy.getLocalTranslation());
                    enemyNode.detachChildAt(j);
                }
            }

            for (int j = bulletNode.getQuantity() - 1; j >= 0; j--) {
                Spatial bullet = bulletNode.getChild(j);
                if (checkCollision(bullet, (Float) bullet.getUserData("radius"), bh, bhr)) {
                    bulletNode.detachChildAt(j);
                    BlackHoleControl bhc = bh.getControl(BlackHoleControl.class);
                    bhc.wasShot();
                    if (bhc.isDead()) {
                        particleManager.blackHoleExplosion(bh.getLocalTranslation());
                        blackHoleNode.detachChildAt(i);
                        sound.explosion();
                        break;
                    }
                }
            }
        }

        if (playerKilled) {
            killPlayer();
        }
    }

    private boolean checkCollision(Spatial a, float ra, Spatial b, float rb) {
        float sum = ra + rb;
        return a.getLocalTranslation().distanceSquared(
                b.getLocalTranslation()) < sum * sum;
    }

    private void pushApart(Spatial a, Spatial b) {
        Vector3f d = a.getLocalTranslation().subtract(b.getLocalTranslation());
        Vector3f push = d.normalize().mult(10f / (d.lengthSquared() + 1));

        SeekerControl sa = a.getControl(SeekerControl.class);
        WandererControl wa = a.getControl(WandererControl.class);
        if (sa != null) {
            sa.applyGravity(push);
        } else if (wa != null) {
            wa.applyGravity(push);
        }

        SeekerControl sb = b.getControl(SeekerControl.class);
        WandererControl wb = b.getControl(WandererControl.class);
        if (sb != null) {
            sb.applyGravity(push.negate());
        } else if (wb != null) {
            wb.applyGravity(push.negate());
        }
    }

    // -------------------------------------------------------
    // Gravedad
    // -------------------------------------------------------
    private void handleGravity(float tpf) {
        for (int i = 0; i < blackHoleNode.getQuantity(); i++) {
            Spatial bh = blackHoleNode.getChild(i);
            if (!(Boolean) bh.getUserData("active")) {
                continue;
            }

            if (isNearby(player, bh, 250)) {
                applyGravity(bh, player, tpf);
            }

            for (int j = 0; j < bulletNode.getQuantity(); j++) {
                Spatial bullet = bulletNode.getChild(j);
                if (isNearby(bullet, bh, 250)) {
                    applyGravity(bh, bullet, tpf);
                }
            }
            for (int j = 0; j < bossLaserNode.getQuantity(); j++) {
                Spatial laser = bossLaserNode.getChild(j);
                if (isNearby(laser, bh, 250)) {
                    applyGravityToLaser(bh, laser, tpf);
                }
            }
            for (int j = 0; j < enemyNode.getQuantity(); j++) {
                Spatial enemy = enemyNode.getChild(j);
                if (!(Boolean) enemy.getUserData("active")) {
                    continue;
                }
                if (isNearby(enemy, bh, 250)) {
                    applyGravity(bh, enemy, tpf);
                }
            }

            Node particleNode = particleManager.getParticleNode();
            for (int j = 0; j < particleNode.getQuantity(); j++) {
                Spatial p = particleNode.getChild(j);
                Boolean affected = p.getUserData("affectedByGravity");
                if (affected != null && affected) {
                    applyGravityToParticle(bh, p, tpf);
                }
            }
        }
    }

    private boolean isNearby(Spatial a, Spatial b, float dist) {
        return a.getLocalTranslation().distanceSquared(
                b.getLocalTranslation()) <= dist * dist;
    }

    private void applyGravity(Spatial bh, Spatial target, float tpf) {
        Vector3f diff = bh.getLocalTranslation().subtract(target.getLocalTranslation());
        float distance = diff.length();
        Vector3f gravity = diff.normalize().multLocal(tpf);

        switch (target.getName()) {
            case "Player":
                gravity.multLocal(250f / distance);
                target.getControl(PlayerControl.class).applyGravity(gravity.mult(80f));
                break;
            case "Bullet":
                gravity.multLocal(250f / distance);
                target.getControl(BulletControl.class).applyGravity(gravity.mult(-0.8f));
                break;
            case "Seeker":
                SeekerControl sc = target.getControl(SeekerControl.class);
                if (sc != null) {
                    sc.applyGravity(gravity.mult(50000));
                }
                break;
            case "Wanderer":
                WandererControl wc = target.getControl(WandererControl.class);
                if (wc != null) {
                    wc.applyGravity(gravity.mult(50000));
                }
                break;
        }
    }

    private void applyGravityToLaser(Spatial bh, Spatial laser, float tpf) {
        Vector3f diff = bh.getLocalTranslation().subtract(laser.getLocalTranslation());
        Vector3f gravity = diff.normalize().multLocal(tpf);
        float dist = diff.length();
        gravity.multLocal(250f / dist);
        BossLaserControl blc = laser.getControl(BossLaserControl.class);
        if (blc != null) {
            blc.applyGravity(gravity.mult(-0.8f));
        }
    }

    private void applyGravityToParticle(Spatial bh, Spatial particle, float tpf) {
        Vector3f diff = bh.getLocalTranslation().subtract(particle.getLocalTranslation());
        float distance = diff.length();
        ParticleControl pc = particle.getControl(ParticleControl.class);
        if (pc != null) {
            pc.applyGravity(diff.normalize().multLocal(tpf).mult(15000), distance);
        }
    }

    // -------------------------------------------------------
    // Muerte y respawn
    // -------------------------------------------------------
    private void killPlayer() {
        player.setUserData("alive", false);
        player.setUserData("dieTime", System.currentTimeMillis());
        player.getControl(PlayerControl.class).reset();
        player.setCullHint(Spatial.CullHint.Always);
        enemyNode.detachAllChildren();
        blackHoleNode.detachAllChildren();
        bossNode.detachAllChildren();
        bossLaserNode.detachAllChildren();
        particleManager.playerExplosion(player.getLocalTranslation());
        sound.stopBossMusic();
        sound.explosion();

        if (!hud.removeLife()) {
            hud.endGame();
            gameOver = true;
        }
    }

    private void respawnPlayer() {
        player.setLocalTranslation(screenWidth / 2f, screenHeight / 2f, 0);
        player.setUserData("alive", true);
        player.setCullHint(Spatial.CullHint.Inherit);
        inverseSpawnChance = 30f;
        sound.playMusic();
        grid.applyDirectedForce(new Vector3f(0, 0, 5000),
                player.getLocalTranslation(), 100);
    }
}
