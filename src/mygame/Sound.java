package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import java.util.Random;

public class Sound {

    private AssetManager assetManager;
    private static final Random rand = new Random();

    // Arrays de sonidos
    private AudioNode[] explosions;
    private AudioNode[] shots;
    private AudioNode[] spawns;
    private AudioNode music;
    private AudioNode menuMusic;
    private AudioNode bossMusic;

    public Sound(AssetManager assetManager) {
        this.assetManager = assetManager;
        load();
    }

    private void load() {
        // Explosiones
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

        // Disparos
        shots = new AudioNode[]{
            loadEffect("Sounds/Shooting/alienshoot1.ogg"),
            loadEffect("Sounds/Shooting/alienshoot2.ogg"),
            loadEffect("Sounds/Shooting/alienshoot3.ogg"),
            loadEffect("Sounds/Shooting/NovaShot.ogg")
        };

        // Spawns
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

        // Música en juego
        music = new AudioNode(assetManager,
                "Sounds/Music/n-Dimensions.ogg", DataType.Stream);
        music.setPositional(false);
        music.setLooping(true);
        music.setVolume(0.5f);
        
        // Música en menú
        menuMusic = new AudioNode(assetManager,
                "Sounds/Music/Space Sprinkles.ogg", DataType.Stream);
        menuMusic.setPositional(false);
        menuMusic.setLooping(true);
        menuMusic.setVolume(0.5f);

        // Música del jefe
        bossMusic = new AudioNode(assetManager,
                "Sounds/Music/Orbital_Colossus.ogg", DataType.Stream);
        bossMusic.setPositional(false);
        bossMusic.setLooping(true);
        bossMusic.setVolume(0.6f);
    }

    // Helper para cargar efectos de sonido
    private AudioNode loadEffect(String path) {
        AudioNode node = new AudioNode(assetManager, path, DataType.Buffer);
        node.setPositional(false);
        node.setLooping(false);
        node.setVolume(0.5f);
        return node;
    }

    // Reproduce un sonido aleatorio de cada categoría — igual que en XNA
    public void explosion() {
        explosions[rand.nextInt(explosions.length)].playInstance();
    }

    public void shot() {
        shots[rand.nextInt(shots.length)].playInstance();
    }

    public void spawn() {
        spawns[rand.nextInt(spawns.length)].playInstance();
    }

    public void playMusic() {
        music.play();
    }

    public void stopMusic() {
        music.stop();
    }

    public void playMenuMusic() {
        // detener música de juego si estuviera sonando
        music.stop();
        menuMusic.play();
    }
    
    public void stopMenuMusic() {
        menuMusic.stop();
    }

    public void playBossMusic() {
        music.stop();
        menuMusic.stop();
        bossMusic.play();
    }

    public void stopBossMusic() {
        bossMusic.stop();
    }
}
