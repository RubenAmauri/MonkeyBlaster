# MonkeyBlaster — Documentación del Proyecto
> Basado en el tutorial: *"Make a Neon Vector Shooter in jMonkeyEngine"*  
> Tutorial de referencia: [Tuts+ GameDev — The Basics](https://code.tutsplus.com/make-a-neon-vector-shooter-in-jmonkeyengine-the-basics--gamedev-11616t)  
> Motor: jMonkeyEngine 3.8.0 | Lenguaje: Java
> - Notas:
> - Los sprites no tenían una fuente "ya lista para usar", tuve que descargarlos desde una imagen donde venían todos juntos, luego los recorté, pero salían en el juego con fondo:
> - ![[MonkeyBlaster_1_Sprites 1.png]]
> - Esto hacía que se vieran raros en el juego, con un marco morado, por lo que las pasé por un removedor de fondos online y quedaron así:
> - ![[MonkeyBlaster_1_Sprites_SinFondo.png]]

---

## Índice

1. [Estructura del proyecto](#1-estructura-del-proyecto)
2. [Capítulo 1 — Escena 2D, input y disparo](#2-capítulo-1)
   - [Configuración de la cámara 2D](#21-configuración-de-la-cámara-2d)
   - [Carga de entidades — getSpatial()](#22-carga-de-entidades--getspatial)
   - [El Scene Graph de jME](#23-concepto-clave-el-scene-graph-de-jme)
   - [UserData en Spatials](#24-concepto-clave-userdata-en-spatials)
   - [Controls — comportamiento de objetos](#25-concepto-clave-controls)
   - [PlayerControl — movimiento del jugador](#26-playercontrol)
   - [Input — teclado y ratón](#27-manejo-de-input)
   - [BulletControl — comportamiento de balas](#28-bulletcontrol)
   - [Helpers matemáticos — vectores y ángulos](#29-helpers-matemáticos)
3. [Clases implementadas](#3-clases-implementadas)
4. [Pendientes / Próximos capítulos](#4-pendientes)

---

## 1. Estructura del proyecto

```
MonkeyBlaster/
├── src/
│   └── monkeyblaster/
│       ├── MonkeyBlasterMain.java   ← Clase principal, game loop
│       ├── PlayerControl.java       ← Comportamiento del jugador
│       └── BulletControl.java       ← Comportamiento de las balas
└── assets/
    └── Textures/
        ├── Player.png
        └── Bullet.png
```

> **Nota:** Los sprites (Player.png, Bullet.png, etc.) provienen del zip de recursos del tutorial original.  
> Deben colocarse en `assets/Textures/` para que `assetManager` los encuentre correctamente.

---

## 2. Capítulo 1

### 2.1 Configuración de la cámara 2D

jMonkeyEngine es fundamentalmente un motor 3D. Para usarlo en 2D hay que ajustar la cámara manualmente:

```java
cam.setParallelProjection(true);       // Proyección ortográfica (sin perspectiva)
cam.setLocation(new Vector3f(0,0,0.5f));
getFlyByCamera().setEnabled(false);    // Desactivar cámara libre (molesta en 2D)
```

> **📌 Aprendizaje:** `setParallelProjection(true)` elimina el efecto de perspectiva — los objetos no se hacen más pequeños con la distancia. Esencial en juegos 2D.

---

### 2.2 Carga de entidades — `getSpatial()`

Método utilitario privado que encapsula la carga de cualquier sprite del juego:

```java
private Spatial getSpatial(String name) {
    Node node = new Node(name);
    Picture pic = new Picture(name);
    Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/" + name + ".png");
    pic.setTexture(assetManager, tex, true);

    float width  = tex.getImage().getWidth();
    float height = tex.getImage().getHeight();
    pic.setWidth(width);
    pic.setHeight(height);
    pic.move(-width/2f, -height/2f, 0);  // Centrar el pivote

    Material picMat = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");
    picMat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
    node.setMaterial(picMat);

    node.setUserData("radius", width/2);
    node.attachChild(pic);
    return node;
}
```

**¿Por qué mover la imagen `-width/2, -height/2`?**  
En jME, el pivote de rotación de una `Picture` está en su esquina inferior izquierda. Al moverla y envolverla en un `Node` padre, logramos que las rotaciones ocurran respecto al centro visual del sprite.

**¿Por qué `BlendMode.AlphaAdditive`?**  
Las partes transparentes superpuestas de múltiples imágenes se suman en brillo. Esto hace que explosiones y efectos de luz se vean más intensos y brillantes (efecto neon).

---

### 2.3 Concepto clave: El Scene Graph de jME

El scene graph es la estructura de árbol que organiza todos los objetos visibles en la escena.

```
guiNode  (raíz 2D)
├── player (Node)
│   └── Picture ("Player")
└── bulletNode (Node)
    ├── bullet1 (Node)
    └── bullet2 (Node)
```

- **`guiNode`** → nodo raíz para juegos 2D. Todo lo que se le adjunte es visible en pantalla.
- **`Node`** → contenedor que puede tener hijos (otros nodos, geometrías, imágenes).
- **`Spatial`** → clase base de todo objeto en la escena (Node y Geometry la extienden).
- `attachChild(spatial)` → agrega al árbol → se vuelve visible.
- `removeFromParent()` → lo quita del árbol → desaparece de la escena.

> **📌 Aprendizaje del tutorial:** Se usa un `bulletNode` separado para organizar todas las balas. Esto facilita iterarlas, contarlas o eliminarlas en grupo sin mezclarlas con el resto de la escena.

---

### 2.4 Concepto clave: UserData en Spatials

Cualquier `Spatial` puede almacenar datos arbitrarios con clave-valor:

```java
player.setUserData("alive", true);        // Guardar
player.setUserData("radius", width / 2);

boolean alive = (Boolean) player.getUserData("alive");  // Leer
float radius  = (Float)   player.getUserData("radius");
```

> **📌 Aprendizaje:** Es un sistema flexible para adjuntar metadatos a entidades sin necesidad de subclasificar. Se usa aquí para saber si el jugador está vivo y para el radio de colisión aproximado.

---

### 2.5 Concepto clave: Controls

Un `Control` encapsula el **comportamiento** de un objeto de la escena. Se adjunta a un `Spatial` y jME lo llama automáticamente cada frame.

```java
// Adjuntar un control a una entidad:
player.addControl(new PlayerControl(settings.getWidth(), settings.getHeight()));

// Recuperar un control desde cualquier parte:
player.getControl(PlayerControl.class).up = true;
```

Todo `Control` personalizado extiende `AbstractControl` e implementa:

| Método | Cuándo se llama | Para qué |
|---|---|---|
| `controlUpdate(float tpf)` | Cada frame | Lógica de movimiento, IA, colisiones |
| `controlRender(RenderManager, ViewPort)` | Cada frame (render) | Efectos visuales custom (raramente usado) |

> **📌 Aprendizaje del tutorial:** Los Controls permiten separar la lógica de comportamiento de la clase principal. Cada entidad maneja su propio comportamiento. Esto es equivalente al patrón **Component** en otros engines como Unity.

---

### 2.6 PlayerControl

Archivo: `PlayerControl.java`

**Responsabilidades:**
- Mover al jugador según las teclas presionadas.
- Rotarlo para que "mire" en la dirección de movimiento.
- Evitar que salga de los límites de pantalla.

**Variables clave:**

| Variable | Tipo | Descripción |
|---|---|---|
| `up/down/left/right` | `boolean` | Estado actual de cada tecla (público, lo escribe el input) |
| `speed` | `float` | Velocidad en píxeles por segundo (`800f`) |
| `lastRotation` | `float` | Última rotación aplicada, para poder revertirla |
| `screenWidth/Height` | `int` | Límites de pantalla para el chequeo de bordes |

**Lógica de rotación:**
```java
// Se revierte la rotación anterior y se aplica la nueva
spatial.rotate(0, 0, -lastRotation + FastMath.PI/2);
lastRotation = FastMath.PI/2;
```

> **📌 Aprendizaje:** El parámetro `tpf` (*time per frame*) es fundamental para movimiento independiente del framerate. `spatial.move(0, tpf * speed, 0)` garantiza la misma velocidad en cualquier PC.

---

### 2.7 Manejo de Input

jME usa un sistema de **mappings + listeners**:

```java
// 1. Registrar mapping (nombre → tecla física)
inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_UP));
inputManager.addMapping("mousePick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

// 2. Registrar listener
inputManager.addListener(this, "up", "mousePick");
```

**Dos tipos de listener usados:**

| Interfaz | Método | Cuándo se activa |
|---|---|---|
| `ActionListener` | `onAction(name, isPressed, tpf)` | Al presionar O soltar una tecla |
| `AnalogListener` | `onAnalog(name, value, tpf)` | Continuamente mientras se mantiene |

Se usa `ActionListener` para el teclado (detectar press/release) y `AnalogListener` para el ratón (disparar mientras se mantiene el clic).

**Cooldown de disparo:**
```java
if (System.currentTimeMillis() - bulletCooldown > 83f) {  // ~12 disparos/segundo
    bulletCooldown = System.currentTimeMillis();
    // ... crear balas
}
```

---

### 2.8 BulletControl

Archivo: `BulletControl.java`

**Responsabilidades:**
- Mover la bala en su dirección a velocidad constante.
- Rotarla para alinearse con su trayectoria.
- Eliminarla cuando sale de pantalla.

**Doble bala (twin shot):**  
El tutorial dispara dos balas paralelas con un offset ortogonal a la dirección de apuntado:

```java
Vector3f offset = new Vector3f(aim.y/3, -aim.x/3, 0); // Perpendicular al aim

// Bala 1: offset positivo
bullet.setLocalTranslation(player.getLocalTranslation().add(aim.add(offset).mult(30)));

// Bala 2: offset negativo (espejo)
bullet2.setLocalTranslation(player.getLocalTranslation().add(aim.add(offset.negate()).mult(30)));
```

> **📌 Aprendizaje:** Un vector perpendicular a `(x, y)` es `(-y, x)` o `(y, -x)`. Este truco geométrico básico es muy útil en juegos 2D.

---

### 2.9 Helpers matemáticos

Dos métodos estáticos en `MonkeyBlasterMain` para conversión entre vectores y ángulos:

```java
// Vector de dirección → ángulo en radianes
public static float getAngleFromVector(Vector3f vec) {
    return new Vector2f(vec.x, vec.y).getAngle();
}

// Ángulo en radianes → vector de dirección unitario
public static Vector3f getVectorFromAngle(float angle) {
    return new Vector3f(FastMath.cos(angle), FastMath.sin(angle), 0);
}
```

**Cálculo de dirección de apuntado (mouse → jugador):**
```java
private Vector3f getAimDirection() {
    Vector2f mouse     = inputManager.getCursorPosition();
    Vector3f playerPos = player.getLocalTranslation();
    Vector3f dif = new Vector3f(mouse.x - playerPos.x, mouse.y - playerPos.y, 0);
    return dif.normalizeLocal(); // Vector unitario (longitud = 1)
}
```

> **📌 Aprendizaje:** Al usar `guiNode`, las unidades de traslación equivalen a píxeles, lo que hace que la posición del cursor (también en píxeles) sea directamente comparable con la posición del jugador. `normalizeLocal()` convierte cualquier vector a longitud 1, lo que permite multiplicarlo por una velocidad sin que la distancia afecte la rapidez.

---

## 3. Clases implementadas

### `MonkeyBlasterMain.java`
| Elemento | Tipo | Descripción |
|---|---|---|
| `simpleInitApp()` | Método override | Inicialización de cámara, entidades e input |
| `simpleUpdate(float tpf)` | Método override | Game loop principal (aún vacío en Cap. 1) |
| `onAction()` | ActionListener | Maneja press/release de teclas → actualiza PlayerControl |
| `onAnalog()` | AnalogListener | Maneja clic sostenido → instancia balas |
| `getSpatial(String)` | Método privado | Factory de entidades a partir de PNG |
| `getAimDirection()` | Método privado | Calcula vector normalizado ratón→jugador |
| `getAngleFromVector()` | Método estático | Conversión vector → ángulo (usado por BulletControl) |
| `getVectorFromAngle()` | Método estático | Conversión ángulo → vector (reservado para caps. futuros) |

### `PlayerControl.java`
Extiende `AbstractControl`. Controla movimiento, rotación y límites de pantalla del jugador.

### `BulletControl.java`
Extiende `AbstractControl`. Controla movimiento, rotación y eliminación de cada bala.
- Nave visible en el centro de pantalla
- Movimiento con flechas (arriba/abajo/izquierda/derecha)
- Rotación hacia la dirección de movimiento
- Doble bala disparada hacia el cursor al hacer clic izquierdo
- Balas eliminadas al salir de pantalla
- Assets: sprites recortados manualmente del spritesheet MonkeyBlaster_1_Sprites.png

---

## 4. Pendientes

### Capítulo 2 — Enemigos, colisiones y sonido
- [x] Implementar clase `EnemyControl`
- [x] Sistema de detección de colisiones (bala vs enemigo, enemigo vs jugador)
- [x] Integrar efectos de sonido con `AudioNode`

### Capítulo 3 — GUI y agujeros negros
- [x] HUD (puntuación, vidas)
- [x] Entidad "black hole" con comportamiento de atracción

### Capítulo 4 — Efectos de partículas
- [x] Sistema de partículas para explosiones (`ParticleEmitter`)

### Capítulo 5 — Grid de fondo
- [x] Malla deformable tipo "warp grid"

### Assets pendientes
- [x] Agregar zip de sprites al proyecto (`Player.png`, `Bullet.png`, etc.)

---

