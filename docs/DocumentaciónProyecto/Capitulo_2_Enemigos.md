# Capítulo 2 — Enemigos, Colisiones y Sonido
> Continuación de [[Capitulo_1_Basicos]]  
> Basado en: [Make a Neon Vector Shooter in XNA — Enemies & Collisions](https://dev.tutsplus.com/series/vector-shooter-xna--gamedev-10559) (traducido a jMonkeyEngine)  
> Motor: jMonkeyEngine 3.8.0 | Lenguaje: Java
> El primer reto de este capítulo, fue que el tutorial base se saltó este capítulo. Al parecer el tutorial fue hecho principalmente para otro motor llamado XNA, luego usaron el mismo tutorial para jmonkey, pero no encontré el segundo capítulo de jmonkey.
> Tuve que pedirle a la IA que me ayudara con las traducciones de código XNA (que usa c# con directx) a java con jmonkey para las dificultades futuras.

---

## Índice

1. [Concepto: Behaviours → Controls](#1-concepto-behaviours--controls)
2. [SeekerControl — Enemigo perseguidor](#2-seekercontrol)
3. [WandererControl — Enemigo errante](#3-wanderercontrol)
4. [EnemySpawner — Generación de enemigos](#4-enemyspawner)
5. [Colisiones](#5-colisiones)
6. [Muerte y respawn del jugador](#6-muerte-y-respawn-del-jugador)
7. [Sonido — Clase Sound](#7-sonido)
8. [Clases modificadas](#8-clases-modificadas)
9. [Estado al finalizar Capítulo 2](#9-estado-al-finalizar-capítulo-2)

---

## 1. Concepto: Behaviours → Controls

En XNA el tutorial crea una clase `Enemy` base y le asigna behaviours usando iteradores de C#. En jME ya tenemos el equivalente perfecto — los `AbstractControl` del Capítulo 1.

La traducción queda así:
![[Pasted image 20260425150859.png|601]]

| XNA | jME | Notas |
|---|---|---|
| Clase `Enemy` base | `AbstractControl` | Ya existía del Cap. 1 |
| `FollowPlayer()` behaviour | `SeekerControl` | Clase nueva |
| `MoveRandomly()` behaviour | `WandererControl` | Clase nueva |
| `timeUntilStart` (fade in) | `spawnTime` + `UserData("active")` | Mismo concepto, distinta API |
| `Velocity *= 0.8f` (fricción) | `velocity.multLocal(0.98f)` | Ajustado para jME |
| `enemy.AddBehaviour()` | `spatial.addControl()` | Patrón Component |
| `ApplyBehaviours()` cada frame | `controlUpdate(float tpf)` automático | jME lo llama solo |

El tutorial XNA argumenta usar **composición sobre herencia** — en lugar de crear subclases `SeekerEnemy` y `WandererEnemy`, se crean behaviours reutilizables que se adjuntan a cualquier entidad. En jME esto es natural gracias a los `AbstractControl`.

> **📌 Aprendizaje:** La ausencia del Capítulo 2 en el tutorial de jME obligó a traducir el código de C#/XNA a Java/jME manualmente. Esto fue un ejercicio útil para entender las diferencias entre motores: XNA usa herencia y clases estáticas, mientras que jME favorece el patrón Component con Controls. Ambos logran el mismo resultado, pero jME resulta más flexible para añadir comportamientos en runtime.

---

## 2. SeekerControl

El Seeker simplemente acelera hacia el jugador cada frame. La fricción (`0.98f`) hace que naturalmente alcance una velocidad máxima sin necesidad de limitarla manualmente.

**Responsabilidades:**
- Permanecer inactivo durante 1 segundo tras el spawn (fade in lógico).
- Calcular la dirección hacia el jugador cada frame.
- Acumular velocidad en esa dirección con fricción para simular una velocidad máxima natural.
- Rotarse para mirar hacia su dirección de movimiento.
- Exponer `applyGravity()` para el sistema de agujeros negros del Capítulo 3.

```java
package mygame;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

public class SeekerControl extends AbstractControl {

    private Spatial player;
    private float speed = 150f;
    private Vector3f velocity = new Vector3f();
    private long spawnTime;

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
            if (elapsed >= 1000) {
                spatial.setUserData("active", true);
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
            spatial.setLocalRotation(
                new com.jme3.math.Quaternion().fromAngles(0, 0, angle)
            );
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public void applyGravity(Vector3f gravity) {
        velocity.addLocal(gravity);
    }
}
```

> **📌 Aprendizaje:** La fricción multiplicativa (`velocity * 0.98f` cada frame) es un truco clásico en videojuegos para simular una velocidad máxima sin necesidad de calcularla explícitamente. Si la aceleración aporta más de lo que la fricción resta, la velocidad sube hasta que se equilibran. Al probarlo en juego, los Seekers se sintieron demasiado lentos — el valor de `speed` se puede ajustar al finalizar el Capítulo 5 cuando todo esté implementado.

---

## 3. WandererControl

Archivo: `WandererControl.java`

El Wanderer se mueve aleatoriamente — elige una dirección y le hace pequeños ajustes periódicos. Si choca con el borde de la pantalla, redirige hacia el centro.

**Responsabilidades:**
- Permanecer inactivo durante 1 segundo tras el spawn.
- Elegir una dirección aleatoria inicial en radianes.
- Ajustar ligeramente la dirección cada 6 frames para simular movimiento orgánico.
- Redirigir hacia el centro si se acerca demasiado a los bordes.
- Rotar visualmente de forma continua como efecto estético.
- Exponer `applyGravity()` para el sistema de agujeros negros.

```java
package mygame;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
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
            if (elapsed >= 1000) {
                spatial.setUserData("active", true);
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
```
![[Pasted image 20260425154940.png]]

**Diferencias clave respecto al XNA:**

| XNA | jME | Por qué |
|---|---|---|
| Ajuste cada 6 `yield return 0` | `frameCount % 6 == 0` | Sin iteradores, contamos frames manualmente |
| `MathHelper.WrapAngle()` | `% FastMath.TWO_PI` | Equivalente en Java |
| `rand.NextFloat(0, TwoPi)` | `rand.nextFloat() * FastMath.TWO_PI` | API de Java |
| `bounds.Inflate(-width, -height)` | Margen manual de `50f` | jME no tiene bounds inflate |

> **📌 Aprendizaje:** Al probarlo en juego, los Wanderers se sintieron demasiado rápidos en comparación con los Seekers — el multiplicador `tpf * 60f` en el movimiento es el responsable. Esto se dejó pendiente de ajuste para cuando todo el juego esté completo, ya que el balance de velocidades afecta directamente la dificultad y es mejor calibrarlo con todos los sistemas activos.

---

## 4. EnemySpawner

En XNA era una clase estática separada. En jME se integró directamente en `MonkeyBlasterMain` — es más limpio dado que ya tenemos ahí el `guiNode`, `settings` y el `player`.

**Responsabilidades:**
- Generar Seekers y Wanderers aleatoriamente cada frame.
- Garantizar que los enemigos aparezcan a más de 250px del jugador.
- Aumentar gradualmente la dificultad reduciendo `inverseSpawnChance`.
- Reiniciar la dificultad cuando el jugador muere.

```java
private void spawnEnemies() {
    if ((Boolean) player.getUserData("alive")) {
        if (System.currentTimeMillis() - spawnCooldown > 83f) {
            spawnCooldown = System.currentTimeMillis();

            if (rand.nextInt((int) inverseSpawnChance) == 0) {
                createEnemy("Seeker");
            }
            if (rand.nextInt((int) inverseSpawnChance) == 0) {
                createEnemy("Wanderer");
            }
        }

        // Dificultad gradual — igual que en XNA
        if (inverseSpawnChance > 20f) {
            inverseSpawnChance -= 0.005f;
        }
    }
}

private void createEnemy(String type) {
    Spatial enemy = getSpatial(type);
    enemy.setLocalTranslation(getSpawnPosition());
    enemy.setUserData("active", false);

    if (type.equals("Seeker")) {
        enemy.addControl(new SeekerControl(player));
    } else if (type.equals("Wanderer")) {
        enemy.addControl(new WandererControl(
                settings.getWidth(), settings.getHeight()));
    }

    enemyNode.attachChild(enemy);
}

private Vector3f getSpawnPosition() {
    Vector3f pos;
    do {
        pos = new Vector3f(
                rand.nextInt(settings.getWidth()),
                rand.nextInt(settings.getHeight()),
                0);
    } while (pos.distanceSquared(player.getLocalTranslation()) < 250 * 250);
    return pos;
}
```

### ⚠️ Nota sobre el while loop de getSpawnPosition()

> El `do-while` funciona bien mientras el área donde pueden spawnear enemigos sea mayor al área prohibida (250px alrededor del jugador). Si reduces la pantalla o aumentas esa distancia demasiado, podrías causar un loop infinito.

> **📌 Aprendizaje:** Al integrar el spawner en `MonkeyBlasterMain` en lugar de crear una clase separada, se simplificó el código al evitar pasar referencias entre clases. Es una decisión de diseño válida para proyectos pequeños — en un juego más grande sería recomendable separarlo para mantener el principio de responsabilidad única.

---

## 5. Colisiones

Implementado en: `MonkeyBlasterMain.java`

**Tipos de colisión manejados:**

| Entidad A | Entidad B | Resultado |
|---|---|---|
| Bala | Enemigo | Ambos desaparecen de la escena |
| Jugador | Enemigo | Jugador muere, todos los enemigos desaparecen |
| Enemigo | Enemigo | Se empujan entre sí proporcionalmente a su cercanía |

```java
private void handleCollisions() {
    float playerRadius = (Float) player.getUserData("radius");

    // --- Bala vs Enemigo ---
    for (int i = enemyNode.getQuantity() - 1; i >= 0; i--) {
        Spatial enemy = enemyNode.getChild(i);
        if (!(Boolean) enemy.getUserData("active")) continue;
        float enemyRadius = (Float) enemy.getUserData("radius");

        for (int j = bulletNode.getQuantity() - 1; j >= 0; j--) {
            Spatial bullet = bulletNode.getChild(j);
            float bulletRadius = (Float) bullet.getUserData("radius");
            if (checkCollision(enemy, enemyRadius, bullet, bulletRadius)) {
                bulletNode.detachChildAt(j);
                enemyNode.detachChildAt(i);
                sound.explosion();
                break;
            }
        }
    }

    // --- Jugador vs Enemigo ---
    if ((Boolean) player.getUserData("alive")) {
        for (int i = 0; i < enemyNode.getQuantity(); i++) {
            Spatial enemy = enemyNode.getChild(i);
            if (!(Boolean) enemy.getUserData("active")) continue;
            float enemyRadius = (Float) enemy.getUserData("radius");
            if (checkCollision(player, playerRadius, enemy, enemyRadius)) {
                killPlayer();
                break;
            }
        }
    }

    // --- Enemigo vs Enemigo ---
    for (int i = 0; i < enemyNode.getQuantity(); i++) {
        for (int j = i + 1; j < enemyNode.getQuantity(); j++) {
            Spatial a = enemyNode.getChild(i);
            Spatial b = enemyNode.getChild(j);
            float ra = (Float) a.getUserData("radius");
            float rb = (Float) b.getUserData("radius");
            if (checkCollision(a, ra, b, rb)) {
                pushApart(a, b);
            }
        }
    }
}

private boolean checkCollision(Spatial a, float ra, Spatial b, float rb) {
    float radiiSum = ra + rb;
    return a.getLocalTranslation().distanceSquared(
               b.getLocalTranslation()) < radiiSum * radiiSum;
}

private void pushApart(Spatial a, Spatial b) {
    Vector3f d = a.getLocalTranslation().subtract(b.getLocalTranslation());
    float pushStrength = 10f / (d.lengthSquared() + 1);
    Vector3f push = d.normalize().mult(pushStrength);

    if (a.getControl(SeekerControl.class) != null)
        a.getControl(SeekerControl.class).applyGravity(push);
    else if (a.getControl(WandererControl.class) != null)
        a.getControl(WandererControl.class).applyGravity(push);

    if (b.getControl(SeekerControl.class) != null)
        b.getControl(SeekerControl.class).applyGravity(push.negate());
    else if (b.getControl(WandererControl.class) != null)
        b.getControl(WandererControl.class).applyGravity(push.negate());
}
```

> **📌 Aprendizaje:** La detección de colisiones circular usa `distanceSquared` en lugar de `distance` — esto evita calcular una raíz cuadrada, operación costosa que se ejecuta miles de veces por segundo. Si `d² < (r1+r2)²`, hay colisión. Los loops de eliminación se recorren en orden inverso para evitar saltar índices al hacer `detachChildAt` — un error clásico al modificar colecciones mientras se itera sobre ellas.

---

## 6. Muerte y respawn del jugador

Implementado en: `MonkeyBlasterMain.java` y `PlayerControl.java`

Cuando el jugador muere, se oculta la nave, se eliminan todos los enemigos y se reinicia la dificultad. El respawn completo con vidas y game over se implementará en el Capítulo 3 junto con `PlayerStatus`.

```java
private void killPlayer() {
    player.setUserData("alive", false);
    player.setUserData("dieTime", System.currentTimeMillis());
    player.getControl(PlayerControl.class).reset();
    enemyNode.detachAllChildren();
    player.setCullHint(Spatial.CullHint.Always);
    sound.explosion();
}

private void respawnPlayer() {
    player.setLocalTranslation(
            settings.getWidth() / 2f,
            settings.getHeight() / 2f, 0);
    player.setUserData("alive", true);
    player.setCullHint(Spatial.CullHint.Inherit);
    inverseSpawnChance = 60f;
}
```

> **📌 Aprendizaje:** `CullHint.Always` oculta un spatial sin eliminarlo del scene graph — sigue existiendo en memoria y puede volverse visible con `CullHint.Inherit`. Es más eficiente que eliminar y recrear el objeto del jugador en cada muerte.

---

## 7. Sonido — Clase Sound

Archivo: `Sound.java`

**Efectos cargados:**

| Categoría | Archivos | Créditos |
|---|---|---|
| Explosiones | Chunky_Explosion, DeathFlash, explosion, explosion1, explosion2, explosion_somewhere_far, rumble, synthetic_explosion_1 | [[creditos]] |
| Disparos | alienshoot1, alienshoot2, alienshoot3, NovaShot | [[creditos]] |
| Spawns | enemy_sounds ~ enemy_sounds_7 | [[creditos]] |
| Música | n-Dimensions — Matthew Pablo | [[creditos]] |

```java
package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import java.util.Random;

public class Sound {

    private AssetManager assetManager;
    private static final Random rand = new Random();
    private AudioNode[] explosions;
    private AudioNode[] shots;
    private AudioNode[] spawns;
    private AudioNode music;

    public Sound(AssetManager assetManager) {
        this.assetManager = assetManager;
        load();
    }

    private void load() {
        explosions = new AudioNode[]{
            loadEffect("Sounds/Explosions/Chunky_Explosion.ogg"),
            loadEffect("Sounds/Explosions/DeathFlash.ogg"),
            loadEffect("Sounds/Explosions/explosion.ogg"),
            loadEffect("Sounds/Explosions/explosion1.ogg"),
            loadEffect("Sounds/Explosions/explosion2.ogg"),
            loadEffect("Sounds/Explosions/explosion_somewhere_far.ogg"),
            loadEffect("Sounds/Explosions/rumble.ogg"),
            loadEffect("Sounds/Explosions/synthetic_explosion_1.ogg")
        };
        shots = new AudioNode[]{
            loadEffect("Sounds/Shooting/alienshoot1.ogg"),
            loadEffect("Sounds/Shooting/alienshoot2.ogg"),
            loadEffect("Sounds/Shooting/alienshoot3.ogg"),
            loadEffect("Sounds/Shooting/NovaShot.ogg")
        };
        spawns = new AudioNode[]{
            loadEffect("Sounds/Spawns/Spawns/enemy_sounds.ogg"),
            loadEffect("Sounds/Spawns/Spawns/enemy_sounds_1.ogg"),
            loadEffect("Sounds/Spawns/Spawns/enemy_sounds_2.ogg"),
            loadEffect("Sounds/Spawns/Spawns/enemy_sounds_3.ogg"),
            loadEffect("Sounds/Spawns/Spawns/enemy_sounds_4.ogg"),
            loadEffect("Sounds/Spawns/Spawns/enemy_sounds_5.ogg"),
            loadEffect("Sounds/Spawns/Spawns/enemy_sounds_6.ogg"),
            loadEffect("Sounds/Spawns/Spawns/enemy_sounds_7.ogg")
        };
        music = new AudioNode(assetManager,
                "Sounds/Music/n-Dimensions.ogg", DataType.Stream);
        music.setPositional(false); // Requerido para audio estéreo
        music.setLooping(true);
        music.setVolume(0.5f);
    }

    private AudioNode loadEffect(String path) {
        AudioNode node = new AudioNode(assetManager, path, DataType.Buffer);
        node.setPositional(false);
        node.setLooping(false);
        node.setVolume(0.5f);
        return node;
    }

    public void explosion() { explosions[rand.nextInt(explosions.length)].playInstance(); }
    public void shot()      { shots[rand.nextInt(shots.length)].playInstance(); }
    public void spawn()     { spawns[rand.nextInt(spawns.length)].playInstance(); }
    public void playMusic() { music.play(); }
    public void stopMusic() { music.stop(); }
}
```

### Dificultades encontradas con el audio

**Crash al iniciar — audio posicional:**
Al llamar `music.play()` el juego crasheó con:
```
java.lang.IllegalStateException: Only mono audio is supported for positional audio nodes
```
jME trata los `AudioNode` como posicionales (3D) por defecto. El audio posicional requiere que el archivo sea mono, pero la música es estéreo. Se resolvió agregando `music.setPositional(false)`.

**Formato de audio — .ogg vs .wav:**
Varios archivos descargados estaban en `.flac` o `.wav`. Se convirtieron a `.ogg` porque jME lo soporta nativamente y el tamaño de archivo es significativamente menor que `.wav`, evitando lag al cargar. La conversión se hizo con Audacity y herramientas online.

**Nombres de archivos con espacios:**
jME puede fallar al cargar assets con espacios en el nombre. Se renombraron: `Chunky Explosion.ogg` → `Chunky_Explosion.ogg` y `n-Dimensions (Main Theme).ogg` → `n-Dimensions.ogg`.

**Sonidos sin correspondencia temática:**
Al descargar muchos assets de opengameart sin escucharlos primero, algunos efectos no corresponden bien al evento que los dispara. Se dejó pendiente para ajustar al finalizar el Capítulo 5.

> **📌 Aprendizaje:** En jME, todo `AudioNode` es posicional por defecto — útil en 3D donde el sonido viene de una dirección, pero en 2D con cámara fija todos los sonidos deben ser `setPositional(false)`. La música usa `DataType.Stream` (carga progresiva desde disco) mientras los efectos usan `DataType.Buffer` (todo en memoria) para reproducirse sin latencia.

---

## 8. Clases modificadas

| Clase | Cambios introducidos en este capítulo |
|---|---|
| `MonkeyBlasterMain` | Agregados: `enemyNode`, `spawnCooldown`, `inverseSpawnChance`, `rand`, `sound`. Nuevos métodos: `spawnEnemies()`, `createEnemy()`, `getSpawnPosition()`, `handleCollisions()`, `checkCollision()`, `pushApart()`, `killPlayer()`, `respawnPlayer()`. Modificados: `simpleUpdate()`, `onAnalog()` |
| `PlayerControl` | Usado `reset()` al morir — ya existía del Capítulo 1, sin cambios |
| `BulletControl` | Sin cambios directos — interactúa con colisiones vía `bulletNode` |

---

## 9. Estado al finalizar Capítulo 2

**Pruebas confirmadas ✅**
```
- Seekers persiguen al jugador con fricción
- Wanderers se mueven aleatoriamente y rebotan en los bordes
- Enemigos spawnean fuera del radio del jugador
- Dificultad aumenta gradualmente con el tiempo
- Balas eliminan enemigos al contacto
- Jugador muere al tocar un enemigo, enemigos desaparecen
- Jugador reaparece en el centro tras 3 segundos
- Dificultad se reinicia al morir
- Música en loop al iniciar
- Efectos de sonido en disparos, explosiones y spawns
```

**Pendientes de ajuste (post Capítulo 5):**
```
- Velocidad del Seeker (muy lento)
- Velocidad del Wanderer (muy rápido)  
- Algunos SFX no corresponden bien al evento que los dispara
- Completar créditos de SFX en [[creditos]]
```

**Siguiente capítulo:** [[Capitulo_3_HUD_Hoyos]]

---

