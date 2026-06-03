# Capítulo 4 — Efectos de Partículas
> Continuación de [[Capitulo_3_HUD_Hoyos]]  
> Basado en: [Make a Neon Vector Shooter in jMonkeyEngine — Particle Effects](https://code.tutsplus.com/make-a-neon-vector-shooter-in-jmonkeyengine-the-basics--gamedev-11616t)  
> Motor: jMonkeyEngine 3.8.0 | Lenguaje: Java

---

## Índice

1. [Concepto: Sistema de partículas](#1-concepto-sistema-de-partículas)
2. [ParticleControl — Comportamiento de cada partícula](#2-particlecontrol)
3. [ParticleManager — Tipos de explosión](#3-particlemanager)
4. [Integración en MonkeyBlasterMain](#4-integración-en-monkeyblastermain)
5. [Cursor personalizado](#5-cursor-personalizado)
6. [Clases modificadas](#6-clases-modificadas)
7. [Estado al finalizar Capítulo 4](#7-estado-al-finalizar-capítulo-4)

---

## 1. Concepto: Sistema de partículas

El sistema de partículas de este capítulo no usa el sistema nativo de jME (`ParticleEmitter`) — en cambio, cada partícula es un sprite independiente con su propio `AbstractControl`. Esto da más control sobre el comportamiento individual de cada partícula.

| Componente | Responsabilidad |
|---|---|
| `ParticleControl` | Movimiento, rotación, escala, alpha y vida de cada partícula |
| `ParticleManager` | Factory de explosiones — crea y configura grupos de partículas |

**Assets usados:**
A diferencia del tutorial original que usa `Laser.png` y `Glow.png`, en este proyecto se usó `Bullet.png` para ambos tipos de partícula por falta de los assets originales. El efecto visual es similar gracias al `BlendMode.AlphaAdditive` establecido desde el Capítulo 1.

> **📌 Aprendizaje:** Implementar partículas como sprites individuales con Controls es más flexible que usar el `ParticleEmitter` nativo de jME — permite comportamientos únicos por partícula como la gravedad de los agujeros negros. El costo es mayor uso de memoria con explosiones grandes (1200 partículas en la explosión del jugador).

---

## 2. ParticleControl

Archivo: `ParticleControl.java`

**Responsabilidades:**
- Mover la partícula según su velocidad, aplicando fricción cada frame.
- Rotarla hacia su dirección de movimiento.
- Escalarla proporcionalmente a su velocidad — más rápida = más grande y brillante.
- Reducir el alpha conforme se acerca al fin de su vida.
- Rebotar en los bordes de pantalla en lugar de desaparecer.
- Eliminarse al expirar su `lifespan`.
- Exponer `applyGravity()` para ser atraída por agujeros negros.

```java
@Override
protected void controlUpdate(float tpf) {
    // Movimiento con fricción
    spatial.move(velocity.mult(tpf * 3f));
    velocity.multLocal(1 - 3f * tpf);

    // Rotación hacia dirección de movimiento
    if (velocity != Vector3f.ZERO) {
        spatial.rotateUpTo(velocity.normalize());
        spatial.rotate(0, 0, FastMath.PI / 2f);
    }

    // Rebote en bordes
    Vector3f loc = spatial.getLocalTranslation();
    if (loc.x < 0)             velocity.x =  Math.abs(velocity.x);
    else if (loc.x > screenWidth)  velocity.x = -Math.abs(velocity.x);
    if (loc.y < 0)             velocity.y =  Math.abs(velocity.y);
    else if (loc.y > screenHeight) velocity.y = -Math.abs(velocity.y);

    // Escala y alpha según velocidad y tiempo de vida
    float speed = velocity.length();
    float percentLife = 1 - (System.currentTimeMillis() - spawnTime) / lifespan;
    float alpha = lesserValue(1.5f, lesserValue(percentLife * 2, speed));
    alpha *= alpha;
    setAlpha(alpha);
    spatial.setLocalScale(0.3f + lesserValue(lesserValue(1.5f, 0.02f * speed + 0.1f), alpha));
    spatial.scale(0.65f);

    if (System.currentTimeMillis() - spawnTime > lifespan)
        spatial.removeFromParent();
}
```

> **📌 Aprendizaje:** Hacer que las partículas sean más brillantes cuando son rápidas (`escala ∝ velocidad`) refuerza visualmente la física — una explosión parece más energética al inicio y se disipa naturalmente. El truco `alpha *= alpha` aplica una curva cuadrática que hace la desaparición más suave que un fade lineal.

---

## 3. ParticleManager

Archivo: `ParticleManager.java`

**Tipos de explosión implementados:**

| Método | Partículas | Colores | Lifespan |
|---|---|---|---|
| `enemyExplosion()` | 120 | Dos colores HSV aleatorios interpolados | 3100ms |
| `bulletExplosion()` | 30 | Azul fijo (`0.676, 0.844, 0.898`) | 1000ms |
| `playerExplosion()` | 1200 | Blanco/Amarillo interpolado | 2800ms |
| `blackHoleExplosion()` | 150 | HSV basado en tiempo transcurrido | 1000ms |
| `sprayParticle()` | 1 por llamada | Púrpura fijo (`0.8, 0.4, 0.8`) | 3500ms |
| `makeExhaustFire()` | 6 por frame | Amarillo (centro) y rojo (lados) | 800ms |

**Colores HSV:**
El tutorial usa el espacio de color HSV en lugar de RGB para generar colores "neon" — saturación y valor fijos, matiz aleatorio. Esto garantiza colores brillantes sin importar el valor aleatorio.

```java
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
```

**Fuego del escape:**
El efecto usa una función seno para hacer que los streams laterales se crucen en un patrón oscilante:

```java
Vector3f perpVel = new Vector3f(baseVel.y, -baseVel.x, 0)
        .multLocal(2f * FastMath.sin(t * 10f));
```

> **📌 Aprendizaje:** La explosión del agujero negro usa el tiempo transcurrido para determinar el matiz (`hue = elapsedTime * 0.003f % 6`), lo que hace que disparos consecutivos tengan colores progresivamente diferentes — más interesante visualmente que colores completamente aleatorios.

---

## 4. Integración en MonkeyBlasterMain

**Inicialización** — `particleManager` debe crearse antes del jugador para que el nodo de partículas quede detrás en el scene graph:

```java
particleManager = new ParticleManager(guiNode,
        getSpatial("Bullet"), getSpatial("Bullet"),
        settings.getWidth(), settings.getHeight());

// Luego el jugador...
player = getSpatial("Player");
player.addControl(new PlayerControl(
        settings.getWidth(), settings.getHeight(), particleManager));
```

**Conexión de explosiones:**

| Evento | Llamada |
|---|---|
| Enemigo muere por bala | `particleManager.enemyExplosion(enemy.getLocalTranslation())` |
| Enemigo muere por agujero negro | `particleManager.enemyExplosion(enemy.getLocalTranslation())` |
| Jugador muere | `particleManager.playerExplosion(player.getLocalTranslation())` |
| Agujero negro destruido | `particleManager.blackHoleExplosion(blackHole.getLocalTranslation())` |
| Bala sale de pantalla | `particleManager.bulletExplosion(loc)` — en `BulletControl` |
| Jugador se mueve | `particleManager.makeExhaustFire(...)` — en `PlayerControl` |
| Agujero negro activo | `particleManager.sprayParticle(...)` — en `BlackHoleControl` |

**Gravedad en partículas** — agregado a `handleGravity()`:

```java
Node particleNode = particleManager.getParticleNode();
for (int j = 0; j < particleNode.getQuantity(); j++) {
    Spatial particle = particleNode.getChild(j);
    Boolean affected = particle.getUserData("affectedByGravity");
    if (affected != null && affected) {
        applyGravityToParticle(blackHole, particle, tpf);
    }
}
```

> **📌 Aprendizaje:** El orden de `attachChild` en el scene graph determina qué se dibuja encima. Al inicializar `particleManager` antes que el jugador, el nodo de partículas queda detrás — las explosiones aparecen bajo la nave y los enemigos, no sobre ellos.

---

## 5. Cursor personalizado

Agregado en `simpleInitApp()` de `MonkeyBlasterMain`:

```java
inputManager.setMouseCursor((JmeCursor) assetManager.loadAsset("Textures/Cursor.ico"));
```

**Dificultades encontradas:**
- jME no acepta `.png` directamente como cursor — requiere `.ico` o `.cur`. Intentar cargar un `.png` con `setMouseCursor` causó un `ClassCastException`.
- Al convertir el `Cursor.png` a `.ico` con conversores online, el fondo transparente se rellenó con negro. Se resolvió usando **GIMP** que preserva el canal alpha correctamente al exportar como `.ico`.

> **📌 Aprendizaje:** jME maneja los cursores como assets especiales de tipo `JmeCursor` — no son texturas normales y no pueden cargarse con `loadTexture()`. El cast explícito `(JmeCursor)` es necesario porque `loadAsset()` devuelve `Object`.

---

## 6. Clases modificadas

| Clase | Cambios introducidos en este capítulo |
|---|---|
| `MonkeyBlasterMain` | Agregado `particleManager`. Modificados: `simpleInitApp()`, `handleCollisions()`, `handleGravity()`, `killPlayer()` |
| `PlayerControl` | Agregado `particleManager` + llamada a `makeExhaustFire()` al moverse |
| `BulletControl` | Agregado `particleManager` + llamada a `bulletExplosion()` al salir de pantalla |
| `BlackHoleControl` | Agregado `particleManager` + spray de partículas en bloque `if (active)` |
| `ParticleControl` | Clase nueva |
| `ParticleManager` | Clase nueva |

---

## 7. Estado al finalizar Capítulo 4

**Pruebas confirmadas ✅**
- Enemigos explotan con colores aleatorios al morir
- Jugador explota en dorado al morir
- Balas explotan en azul al salir de pantalla
- Agujero negro emite partículas púrpuras continuamente
- Agujero negro emite explosión de color al recibir impacto
- Partículas son atraídas hacia agujeros negros
- Fuego del escape visible al mover la nave
- Cursor personalizado visible (Cursor.ico con transparencia)
**Errores encontrados y resueltos:**
- Hud.java contenía BlackHoleControl por error de pegado → recrear archivo
- MonkeyBlasterMain desactualizado → reemplazar archivo completo
- ClassCastException al cargar cursor como .png → convertir a .ico
- Fondo negro en .ico → usar GIMP para preservar transparencia
- IndexOutOfBoundsException al morir por agujero negro → mover killPlayer() fuera del loop

**Siguiente capítulo:** [[Capitulo_5_Grid]]

---
