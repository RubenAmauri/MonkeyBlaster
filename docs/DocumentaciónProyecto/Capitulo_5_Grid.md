# Capítulo 5 — Grid de Fondo
> Continuación de [[Capitulo_4_Particulas]]  
> Basado en: [Make a Neon Vector Shooter in jMonkeyEngine — Warping Grid](https://code.tutsplus.com/make-a-neon-vector-shooter-in-jmonkeyengine-the-basics--gamedev-11616t)  
> Motor: jMonkeyEngine 3.8.0 | Lenguaje: Java

---

## Índice

1. [Concepto: Simulación de resortes](#1-concepto-simulación-de-resortes)
2. [PointMass — Masa puntual](#2-pointmass)
3. [Spring — Resorte](#3-spring)
4. [LineControl y AdditionalLineControl](#4-linecontrol-y-additionallinecontrol)
5. [Grid — La cuadrícula](#5-grid)
6. [Integración en MonkeyBlasterMain](#6-integración-en-monkeyblastermain)
7. [Clases modificadas](#7-clases-modificadas)
8. [Estado al finalizar Capítulo 5](#8-estado-al-finalizar-capítulo-5)

---

## 1. Concepto: Simulación de resortes

El grid de fondo no usa texturas ni sprites — se construye con una simulación física de masas y resortes. En cada intersección del grid hay una **masa puntual** (`PointMass`), y cada masa está conectada a sus vecinas por **resortes** (`Spring`). Las masas del borde están ancladas y no se mueven.

![[Pasted image 20260602141033.png]]

Cuando algo empuja o jala una masa, los resortes transmiten la fuerza a las masas vecinas, creando un efecto de onda que se propaga y se disipa gradualmente.

**Tres tipos de fuerza aplicados al grid:**

| Método | Efecto | Usado cuando |
|---|---|---|
| `applyDirectedForce()` | Empuja en una dirección | Jugador respawnea |
| `applyExplosiveForce()` | Empuja hacia afuera desde un punto | Balas se mueven |
| `applyImplosiveForce()` | Jala hacia adentro desde un punto | Agujero negro activo |

> **📌 Aprendizaje:** Usar una simulación física para efectos visuales es un patrón común en videojuegos. La ventaja es que el comportamiento emerge naturalmente de las ecuaciones — no hay que programar cómo se ve la onda, simplemente se aplica una fuerza y la física hace el resto.

---

## 2. PointMass

Archivo: `PointMass.java`

Representa cada intersección del grid. Almacena posición, velocidad y aceleración, y se actualiza cada frame con integración de Euler simpléctica.

**Variables clave:**

| Variable | Descripción |
|---|---|
| `inverseMass` | `1/masa` — si es 0, la masa es infinita e inamovible (anclas) |
| `damping` | Factor de amortiguamiento (`0.98f`) — simula fricción/resistencia del aire |
| `acceleration` | Se resetea a cero cada frame tras aplicarse |

```java
public void update(float tpf) {
    velocity.addLocal(acceleration.mult(1f));  // v += a
    position.addLocal(velocity.mult(0.6f));    // p += v
    acceleration = Vector3f.ZERO.clone();      // reset aceleración

    if (velocity.lengthSquared() < 0.0001f)
        velocity = Vector3f.ZERO.clone();      // evitar números denormalizados

    velocity.multLocal(damping);
    damping = 0.98f;
    damping = 0.8f;                            // valor final usado

    position.z *= 0.9f;                        // amortiguamiento en Z
    if (position.z < 0.01f) position.z = 0;
}
```

> **📌 Aprendizaje:** `inverseMass = 0` representa una masa infinitamente pesada — ninguna fuerza puede moverla. Es un truco elegante para las anclas del borde: en lugar de tener un tipo especial de objeto, simplemente se usa masa infinita. La integración simpléctica de Euler (`v += a` antes que `p += v`) conserva energía mejor que la Euler estándar, evitando que los resortes ganen energía indefinidamente.

---

## 3. Spring

Archivo: `Spring.java`

Conecta dos `PointMass` y aplica fuerzas cuando la distancia entre ellas supera la longitud natural del resorte. Implementa una versión modificada de la Ley de Hooke con amortiguamiento.

```java
public void update(float tpf) {
    Vector3f x = end1.getPosition().subtract(end2.getPosition());
    float length = x.length();

    if (length > targetLength) {
        x.normalizeLocal().multLocal(length - targetLength);
        Vector3f dv    = end2.getVelocity().subtract(end1.getVelocity());
        Vector3f force = x.mult(stiffness).subtract(dv.mult(damping / 10f));
        end1.applyForce(force.negate());
        end2.applyForce(force);
    }
}
```

Los resortes **solo jalan, nunca empujan** — si la distancia es menor que la longitud natural, no hacen nada. La longitud natural se establece al 95% de la distancia inicial para mantener el grid tenso.

> **📌 Aprendizaje:** La Ley de Hooke dice que la fuerza de un resorte es proporcional a su deformación (`F = k * x`). El término de amortiguamiento (`-damping * velocidad_relativa`) evita que el sistema oscile indefinidamente — es el equivalente físico de la resistencia del aire en el resorte.

---

## 4. LineControl y AdditionalLineControl

Archivos: `LineControl.java`, `AdditionalLineControl.java`

Estos controles actualizan la posición, escala y rotación de cada segmento de línea visible del grid cada frame.

**`LineControl`** — línea entre dos `PointMass`:
```java
protected void controlUpdate(float tpf) {
    spatial.setLocalTranslation(end1.getPosition());
    Vector3f dif = end2.getPosition().subtract(end1.getPosition());
    spatial.setLocalScale(dif.length());
    spatial.lookAt(end2.getPosition(), new Vector3f(1, 0, 0));
}
```

**`AdditionalLineControl`** — línea de interpolación entre puntos medios de dos aristas:
```java
private Vector3f position1() {
    return end11.getPosition().clone().interpolateLocal(
            end12.getPosition(), 0.5f);
}
```

**Dificultad encontrada:**
El tutorial usa `new Vector3f().interpolate(v1, v2, 0.5f)` pero en jME 3.8.0 este método no existe con esa firma. Se resolvió usando `v1.clone().interpolateLocal(v2, 0.5f)`.

> **📌 Aprendizaje:** `lookAt(target, upVector)` rota un spatial para que mire hacia un punto. Combinado con `setLocalScale(longitud)` sobre una línea unitaria, este es el patrón estándar en jME para dibujar segmentos de línea dinámicos entre dos puntos arbitrarios.

---

## 5. Grid

Archivo: `Grid.java`

Construye la cuadrícula creando masas y resortes en cada intersección, y expone métodos para deformarla.

**Tipos de línea:**

| Tipo | Grosor | Cuándo se usa |
|---|---|---|
| `defaultLine` | 1px | Líneas normales del grid |
| `thickLine` | 3px | Cada 3 filas/columnas — da estructura visual |

**Líneas de interpolación:**
Además de las líneas principales, se agregan líneas adicionales entre los puntos medios de cada celda para densificar el grid visualmente sin aumentar la cantidad de masas:

```java
if (x > 0 && y > 0) {
    Geometry addLine1 = defaultLine.clone();
    addLine1.addControl(new AdditionalLineControl(
            points[x-1][y], points[x][y],
            points[x-1][y-1], points[x][y-1]));
    gridNode.attachChild(addLine1);

    Geometry addLine2 = defaultLine.clone();
    addLine2.addControl(new AdditionalLineControl(
            points[x][y-1], points[x][y],
            points[x-1][y-1], points[x-1][y]));
    gridNode.attachChild(addLine2);
}
```

**Color del grid:**
```java
mat.setColor("Color", new ColorRGBA(0.118f, 0.118f, 0.545f, 0.25f));
```
Azul oscuro semitransparente con `BlendMode.AlphaAdditive` — se suma al color del fondo negro para dar el efecto neon.

**Incompatibilidad con jME 3.8.0:**
El tutorial usa `Rectangle` de jME con constructor de 4 floats (`Rectangle(x, y, width, height)`), pero en jME 3.8.0 `Rectangle` solo acepta 3 vectores. Se resolvió eliminando `Rectangle` del constructor de `Grid` y pasando `width` y `height` directamente como `int`.

> **📌 Aprendizaje:** Crear líneas en jME requiere construir un `Mesh` manualmente con vértices e índices — es más complejo que en motores 2D porque jME está optimizado para modelos 3D importados, no geometría primitiva generada en código. El proceso: crear vértices → crear índices → asignar buffers al mesh → crear geometría → asignar material.

---

## 6. Integración en MonkeyBlasterMain

```java
// En simpleInitApp() — antes de todo lo demás:
grid = new Grid(settings.getWidth(), settings.getHeight(),
        new Vector2f(25, 25), guiNode, assetManager);

// En simpleUpdate():
grid.update(tpf);

// En respawnPlayer():
grid.applyDirectedForce(
        new Vector3f(0, 0, 5000),
        player.getLocalTranslation(), 100);
```

El grid se inicializa primero en `simpleInitApp()` para que su nodo quede en el fondo del scene graph — todo lo demás (partículas, enemigos, jugador) se dibuja encima.

---

## 7. Clases modificadas

| Clase | Cambios |
|---|---|
| `MonkeyBlasterMain` | Agregado `grid`. Modificados: `simpleInitApp()`, `simpleUpdate()`, `respawnPlayer()` |
| `BulletControl` | Agregado `grid` + llamada a `applyExplosiveForce()` en `controlUpdate()` |
| `BlackHoleControl` | Agregado `grid` + llamada a `applyImplosiveForce()` en bloque `if (active)` |
| `PointMass` | Clase nueva |
| `Spring` | Clase nueva |
| `LineControl` | Clase nueva |
| `AdditionalLineControl` | Clase nueva |
| `Grid` | Clase nueva |

---

## 8. Estado al finalizar Capítulo 5

**Pruebas confirmadas ✅**
```
- Grid azul visible como fondo
- Grid se deforma al disparar balas
- Grid se contrae alrededor de agujeros negros activos
- Grid emite onda al respawnear el jugador
- Líneas gruesas cada 3 filas/columnas visibles
- Líneas de interpolación densifican el grid
- Grid regresa gradualmente a su posición original
```

**Errores encontrados y resueltos:**
```
- Rectangle(int,int,int,int) no existe en jME 3.8.0 → pasar width y height directamente
- Vector3f.interpolate() no existe en jME 3.8.0 → usar clone().interpolateLocal()
- NullPointerException en grid.update() → grid no se estaba inicializando por línea incompleta
```

**El juego está completo. ✅**

---