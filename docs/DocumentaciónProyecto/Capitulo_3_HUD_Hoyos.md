# Capítulo 3 — GUI y Agujeros Negros

> Continuación de [[Capitulo_2_Enemigos]]  
> Basado en: [Make a Neon Vector Shooter in jMonkeyEngine — GUI & Black Holes](https://code.tutsplus.com/make-a-neon-vector-shooter-in-jmonkeyengine-the-basics--gamedev-11616t)  
> Motor: jMonkeyEngine 3.8.0 | Lenguaje: Java

---

## Índice

1. [BlackHoleControl — Comportamiento del agujero negro](https://claude.ai/chat/d9502d6e-33eb-4438-be7a-5f618def2d7d#1-blackholecontrol)
2. [Spawn de agujeros negros](https://claude.ai/chat/d9502d6e-33eb-4438-be7a-5f618def2d7d#2-spawn-de-agujeros-negros)
3. [Sistema de gravedad](https://claude.ai/chat/d9502d6e-33eb-4438-be7a-5f618def2d7d#3-sistema-de-gravedad)
4. [Colisiones con agujeros negros](https://claude.ai/chat/d9502d6e-33eb-4438-be7a-5f618def2d7d#4-colisiones-con-agujeros-negros)
5. [HUD — Clase Hud](https://claude.ai/chat/d9502d6e-33eb-4438-be7a-5f618def2d7d#5-hud)
6. [Cursor personalizado](https://claude.ai/chat/d9502d6e-33eb-4438-be7a-5f618def2d7d#6-cursor-personalizado)
7. [Clases modificadas](https://claude.ai/chat/d9502d6e-33eb-4438-be7a-5f618def2d7d#7-clases-modificadas)
8. [Estado al finalizar Capítulo 3](https://claude.ai/chat/d9502d6e-33eb-4438-be7a-5f618def2d7d#8-estado-al-finalizar-cap%C3%ADtulo-3)

---

## 1. BlackHoleControl

Archivo: `BlackHoleControl.java`

El agujero negro es uno de los enemigos más interesantes del juego. A diferencia de los enemigos normales, no se mueve — ejerce una fuerza gravitacional sobre todo lo que lo rodea y requiere 10 impactos de bala para ser destruido.

**Responsabilidades:**

- Fade in visual durante 1 segundo tras el spawn.
- Mantener un contador de hitpoints (10 por defecto).
- Exponer `wasShot()` e `isDead()` para el sistema de colisiones.
- Reservar el bloque `if (active)` para efectos de partículas del Capítulo 4.

```java
package mygame;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import com.jme3.scene.control.AbstractControl;

public class BlackHoleControl extends AbstractControl {

    private long spawnTime;
    private int hitpoints;

    public BlackHoleControl() {
        spawnTime = System.currentTimeMillis();
        hitpoints = 10;
    }

    @Override
    protected void controlUpdate(float tpf) {
        boolean active = (Boolean) spatial.getUserData("active");

        if (active) {
            // Lógica adicional en Capítulo 4 (partículas)
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
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public void wasShot() {
        hitpoints--;
    }

    public boolean isDead() {
        return hitpoints <= 0;
    }
}
```

> **📌 Aprendizaje:** El agujero negro usa el mismo patrón de `UserData("active")` que los enemigos, pero aquí además se aprovecha para animar la transparencia durante el fade in. `pic.getMaterial().setColor("Color", color)` modifica el canal alpha del material directamente en runtime — esto es posible porque usamos `BlendMode.AlphaAdditive` desde el Capítulo 1.

---

## 2. Spawn de agujeros negros

Implementado en: `MonkeyBlasterMain.java`

El spawn de agujeros negros es más restrictivo que el de enemigos — solo puede haber 2 en pantalla a la vez, y la probabilidad de que aparezca uno en cada ciclo es de 1 en 1000.

```java
private Node blackHoleNode;
private long spawnCooldownBlackHole;

// En simpleInitApp():
blackHoleNode = new Node("blackholes");
guiNode.attachChild(blackHoleNode);

// Métodos de spawn:
private void spawnBlackHoles() {
    if (blackHoleNode.getQuantity() < 2) {
        if (System.currentTimeMillis() - spawnCooldownBlackHole > 10f) {
            spawnCooldownBlackHole = System.currentTimeMillis();
            if (rand.nextInt(1000) == 0) {
                createBlackHole();
            }
        }
    }
}

private void createBlackHole() {
    Spatial blackHole = getSpatial("BlackHole");
    blackHole.setLocalTranslation(getSpawnPosition());
    blackHole.addControl(new BlackHoleControl());
    blackHole.setUserData("active", false);
    blackHoleNode.attachChild(blackHole);
    sound.spawn();
}
```

![[captura_cap3_1.png]]

> 📸 _Captura sugerida: agujero negro apareciendo en pantalla con el efecto de fade in, mientras hay enemigos y el jugador presentes._

> **📌 Aprendizaje:** Limitar la cantidad máxima de agujeros negros en pantalla (`< 2`) es una decisión de diseño de gameplay — demasiados agujeros negros simultáneos harían el juego injugable. `rand.nextInt(1000) == 0` da una probabilidad del 0.1% por ciclo de que aparezca uno, lo que los hace eventos relativamente raros pero inevitables con el tiempo.

---

## 3. Sistema de gravedad

Implementado en: `MonkeyBlasterMain.java`, `PlayerControl.java`, `BulletControl.java`, `SeekerControl.java`, `WandererControl.java`

El sistema de gravedad aplica fuerzas diferentes según el tipo de entidad:

|Entidad|Tipo de fuerza|Efecto|
|---|---|---|
|Jugador|Lineal (`250f/distance`)|Atraído hacia el agujero|
|Balas|Lineal negativa (`-0.8f`)|**Repelidas** del agujero|
|Seekers/Wanderers|Constante (`150000`)|Atraídos con fuerza fija|

```java
private void handleGravity(float tpf) {
    for (int i = 0; i < blackHoleNode.getQuantity(); i++) {
        Spatial blackHole = blackHoleNode.getChild(i);
        if (!(Boolean) blackHole.getUserData("active")) continue;

        int radius = 250;

        if (isNearby(player, blackHole, radius))
            applyGravity(blackHole, player, tpf);

        for (int j = 0; j < bulletNode.getQuantity(); j++)
            if (isNearby(bulletNode.getChild(j), blackHole, radius))
                applyGravity(blackHole, bulletNode.getChild(j), tpf);

        for (int j = 0; j < enemyNode.getQuantity(); j++) {
            Spatial enemy = enemyNode.getChild(j);
            if (!(Boolean) enemy.getUserData("active")) continue;
            if (isNearby(enemy, blackHole, radius))
                applyGravity(blackHole, enemy, tpf);
        }
    }
}

private boolean isNearby(Spatial a, Spatial b, float distance) {
    return a.getLocalTranslation().distanceSquared(
               b.getLocalTranslation()) <= distance * distance;
}

private void applyGravity(Spatial blackHole, Spatial target, float tpf) {
    Vector3f difference = blackHole.getLocalTranslation()
                              .subtract(target.getLocalTranslation());
    Vector3f gravity = difference.normalize().multLocal(tpf);
    float distance = difference.length();

    String name = target.getName();
    if (name.equals("Player")) {
        gravity.multLocal(250f / distance);
        target.getControl(PlayerControl.class).applyGravity(gravity.mult(80f));
    } else if (name.equals("Bullet")) {
        gravity.multLocal(250f / distance);
        target.getControl(BulletControl.class).applyGravity(gravity.mult(-0.8f));
    } else if (name.equals("Seeker")) {
        target.getControl(SeekerControl.class).applyGravity(gravity.mult(150000));
    } else if (name.equals("Wanderer")) {
        target.getControl(WandererControl.class).applyGravity(gravity.mult(150000));
    }
}
```

**Método `applyGravity()` agregado a cada control:**

```java
// PlayerControl
public void applyGravity(Vector3f gravity) {
    spatial.move(gravity);
}

// BulletControl
public void applyGravity(Vector3f gravity) {
    direction.addLocal(gravity);
}

// SeekerControl y WandererControl — ya existían desde el Capítulo 2
public void applyGravity(Vector3f gravity) {
    velocity.addLocal(gravity);
}
```

![[captura_cap3_2.png]]

> 📸 _Captura sugerida: jugador siendo atraído hacia un agujero negro, con balas siendo desviadas en dirección contraria._

> **📌 Aprendizaje:** Las tres funciones de fuerza usadas en este sistema corresponden a modelos físicos reales. La fuerza lineal (`250f/distance`) se hace más fuerte cuanto más cerca está la entidad — similar a la gravedad real. El valor negativo en las balas (`-0.8f`) invierte la dirección, convirtiéndola en repulsión. La fuerza constante en los enemigos ignora la distancia completamente, lo que hace que los enemigos lejanos se vean igual de afectados que los cercanos.

---

## 4. Colisiones con agujeros negros

Implementado en: `MonkeyBlasterMain.handleCollisions()`

```java
for (int i = blackHoleNode.getQuantity() - 1; i >= 0; i--) {
    Spatial blackHole = blackHoleNode.getChild(i);
    if (!(Boolean) blackHole.getUserData("active")) continue;

    float bhRadius = (Float) blackHole.getUserData("radius");

    // Jugador
    if (checkCollision(player, (Float) player.getUserData("radius"), blackHole, bhRadius)) {
        killPlayer();
    }

    // Enemigos
    for (int j = enemyNode.getQuantity() - 1; j >= 0; j--) {
        Spatial enemy = enemyNode.getChild(j);
        if (checkCollision(enemy, (Float) enemy.getUserData("radius"), blackHole, bhRadius)) {
            enemyNode.detachChildAt(j);
        }
    }

    // Balas
    for (int j = bulletNode.getQuantity() - 1; j >= 0; j--) {
        Spatial bullet = bulletNode.getChild(j);
        if (checkCollision(bullet, (Float) bullet.getUserData("radius"), blackHole, bhRadius)) {
            bulletNode.detachChildAt(j);
            BlackHoleControl bhc = blackHole.getControl(BlackHoleControl.class);
            bhc.wasShot();
            if (bhc.isDead()) {
                blackHoleNode.detachChildAt(i);
                sound.explosion();
                break;
            }
        }
    }
}
```

Y en `killPlayer()` se agregó:

```java
blackHoleNode.detachAllChildren();
```

> **📌 Aprendizaje:** El agujero negro es el único enemigo con un sistema de hitpoints — requiere 10 impactos para ser destruido. Esto lo convierte en un objetivo prioritario pero difícil, ya que las balas son repelidas por su gravedad. El jugador debe acercarse para tener un ángulo de disparo efectivo, pero al hacerlo queda expuesto a ser atraído hacia él.

---

## 5. HUD — Clase Hud

Archivo: `Hud.java`

El HUD maneja toda la información visible para el jugador: vidas, puntuación, multiplicador y pantalla de game over. También gestiona el sistema de puntuación con highscore persistente en archivo.

**Variables clave:**

|Variable|Tipo|Descripción|
|---|---|---|
|`lives`|`int`|Vidas actuales del jugador (inicia en 4)|
|`score`|`int`|Puntuación actual|
|`multiplier`|`int`|Multiplicador activo (máx. 25)|
|`multiplierExpiryTime`|`int`|Tiempo en ms antes de que expire el multiplicador (2000ms)|
|`scoreForExtraLife`|`int`|Puntos necesarios para vida extra (cada 2000)|
|`gameOver`|`boolean`|Estado de game over en `MonkeyBlasterMain`|

```java
package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.scene.Node;
import java.io.*;

public class Hud {
    private AssetManager assetManager;
    private Node guiNode;
    private int screenWidth, screenHeight;
    private final int fontSize = 30;
    private final int multiplierExpiryTime = 2000;
    private final int maxMultiplier = 25;
    public int lives;
    public int score;
    public int multiplier;
    private long multiplierActivationTime;
    private int scoreForExtraLife;
    private BitmapFont guiFont;
    private BitmapText livesText;
    private BitmapText scoreText;
    private BitmapText multiplierText;
    private Node gameOverNode;

    public Hud(AssetManager assetManager, Node guiNode, int screenWidth, int screenHeight) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        setupText();
    }

    private void setupText() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        livesText = new BitmapText(guiFont, false);
        livesText.setLocalTranslation(30, screenHeight - 30, 0);
        livesText.setSize(fontSize);
        livesText.setText("Lives: " + lives);
        guiNode.attachChild(livesText);

        scoreText = new BitmapText(guiFont, true);
        scoreText.setLocalTranslation(screenWidth - 200, screenHeight - 30, 0);
        scoreText.setSize(fontSize);
        scoreText.setText("Score: " + score);
        guiNode.attachChild(scoreText);

        multiplierText = new BitmapText(guiFont, true);
        multiplierText.setLocalTranslation(screenWidth - 200, screenHeight - 100, 0);
        multiplierText.setSize(fontSize);
        multiplierText.setText("Multiplier: " + multiplier);
        guiNode.attachChild(multiplierText);
    }

    public void reset() {
        score = 0;
        multiplier = 1;
        lives = 4;
        multiplierActivationTime = System.currentTimeMillis();
        scoreForExtraLife = 2000;
        updateHUD();
    }

    private void updateHUD() {
        livesText.setText("Lives: " + lives);
        scoreText.setText("Score: " + score);
        multiplierText.setText("Multiplier: " + multiplier);
    }

    public void addPoints(int basePoints) {
        score += basePoints * multiplier;
        if (score >= scoreForExtraLife) {
            scoreForExtraLife += 2000;
            lives++;
        }
        increaseMultiplier();
        updateHUD();
    }

    private void increaseMultiplier() {
        multiplierActivationTime = System.currentTimeMillis();
        if (multiplier < maxMultiplier) {
            multiplier++;
        }
    }

    public boolean removeLife() {
        if (lives == 0) { return false; }
        lives--;
        updateHUD();
        return true;
    }

    public void update() {
        if (multiplier > 1) {
            if (System.currentTimeMillis() - multiplierActivationTime > multiplierExpiryTime) {
                multiplier = 1;
                multiplierActivationTime = System.currentTimeMillis();
                updateHUD();
            }
        }
    }

    public void endGame() {
        gameOverNode = new Node();
        gameOverNode.setLocalTranslation(screenWidth / 2 - 180, screenHeight / 2 + 100, 0);
        guiNode.attachChild(gameOverNode);

        int highscore = loadHighscore();
        if (score > highscore) { saveHighscore(); }

        BitmapText gameOverText = new BitmapText(guiFont, false);
        gameOverText.setLocalTranslation(0, 0, 0);
        gameOverText.setSize(fontSize);
        gameOverText.setText("Game Over");
        gameOverNode.attachChild(gameOverText);

        BitmapText yourScoreText = new BitmapText(guiFont, false);
        yourScoreText.setLocalTranslation(0, -50, 0);
        yourScoreText.setSize(fontSize);
        yourScoreText.setText("Your Score: " + score);
        gameOverNode.attachChild(yourScoreText);

        BitmapText highscoreText = new BitmapText(guiFont, false);
        highscoreText.setLocalTranslation(0, -100, 0);
        highscoreText.setSize(fontSize);
        highscoreText.setText("Highscore: " + highscore);
        gameOverNode.attachChild(highscoreText);
    }

    private int loadHighscore() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File("highscore.txt")));
            String line = reader.readLine();
            reader.close();
            return Integer.valueOf(line);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private void saveHighscore() {
        try {
            FileWriter writer = new FileWriter(new File("highscore.txt"), false);
            writer.write(score + System.getProperty("line.separator"));
            writer.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}
```

**Integración en `MonkeyBlasterMain`:**

```java
private Hud hud;
private boolean gameOver = false;

// En simpleInitApp():
hud = new Hud(assetManager, guiNode, settings.getWidth(), settings.getHeight());
hud.reset();

// En simpleUpdate():
hud.update();

// En handleCollisions(), antes de detachChildAt(i) al matar enemigo:
if (enemy.getName().equals("Seeker"))   hud.addPoints(2);
else if (enemy.getName().equals("Wanderer")) hud.addPoints(1);

// En killPlayer():
if (!hud.removeLife()) {
    hud.endGame();
    gameOver = true;
}

// En simpleUpdate(), condición de respawn:
} else if (System.currentTimeMillis() - (Long) player.getUserData("dieTime") > 3000f && !gameOver) {
    respawnPlayer();
}
```

> ⚠️ **Nota:** El tutorial indica crear un archivo `highscore.txt` manualmente antes de correr el juego, o el `loadHighscore()` retornará 0 sin error gracias al bloque `catch`.

![[captura_cap3_3.png]]

> 📸 _Captura sugerida: HUD visible en esquinas de la pantalla mostrando Lives, Score y Multiplier durante el juego._

![[captura_cap3_4.png]]

> 📸 _Captura sugerida: pantalla de Game Over mostrando "Game Over", "Your Score" y "Highscore" centrados en pantalla._

> **📌 Aprendizaje:** El multiplicador de puntuación es un mecanismo de diseño que recompensa el juego agresivo — matar enemigos consecutivamente sin morir lo incrementa, mientras que no matar nada durante 2 segundos lo reinicia. Esto crea una tensión constante entre jugar seguro (evitar enemigos) y jugar agresivo (buscar multiplicadores altos). El highscore se guarda en texto plano con `FileWriter`, que es el método estándar de Java — el tutorial menciona que jME tiene un `assetManager` que podría usarse, pero la complejidad adicional no vale la pena para un archivo tan simple.

---

## 6. Cursor personalizado

El tutorial agrega un cursor personalizado con una sola línea en `simpleInitApp()`:

```java
inputManager.setMouseCursor((JmeCursor) assetManager.loadAsset("Textures/Cursor.png"));
```

> ⚠️ El archivo debe estar en `assets/Textures/Cursor.png`. En este proyecto el sprite ya estaba disponible desde el spritesheet original.

![[captura_cap3_5.png]]

> 📸 _Captura sugerida: cursor personalizado visible sobre el juego, diferente al cursor estándar del sistema._

---

## 7. Clases modificadas

|Clase|Cambios introducidos en este capítulo|
|---|---|
|`MonkeyBlasterMain`|Agregados: `blackHoleNode`, `spawnCooldownBlackHole`, `hud`, `gameOver`. Nuevos métodos: `spawnBlackHoles()`, `createBlackHole()`, `handleGravity()`, `isNearby()`, `applyGravity()`. Modificados: `simpleUpdate()`, `handleCollisions()`, `killPlayer()`, `respawnPlayer()`|
|`PlayerControl`|Agregado: `applyGravity(Vector3f)` + import de `Vector3f`|
|`BulletControl`|Agregado: `applyGravity(Vector3f)`|
|`BlackHoleControl`|Clase nueva|
|`Hud`|Clase nueva|

---

## 8. Estado al finalizar Capítulo 3

**Pruebas confirmadas ✅**

```
- Agujeros negros aparecen ocasionalmente (máx. 2 en pantalla)
- Fade in visual al aparecer
- Jugador es atraído hacia el agujero negro
- Balas son repelidas por el agujero negro
- Enemigos son atraídos hacia el agujero negro
- Balas destruyen el agujero negro tras 10 impactos
- Jugador muere al tocar el agujero negro
- HUD muestra vidas, puntuación y multiplicador
- Multiplicador aumenta al matar enemigos y expira tras 2 segundos sin kills
- Vida extra al alcanzar cada 2000 puntos
- Game over al perder todas las vidas
- Highscore guardado en archivo
- Cursor personalizado visible
```

**Errores encontrados y resueltos:**

```
- ClassFormatError: campo spawnCooldownBlackHole declarado dos veces → eliminar duplicado
- RuntimeException: cannot find symbol Vector3f en PlayerControl → agregar import
- RuntimeException: cannot find symbol respawnPlayer() → método no había sido agregado en Cap. 2
```

**Siguiente capítulo:** [[Capitulo_4_Particulas]]

---
