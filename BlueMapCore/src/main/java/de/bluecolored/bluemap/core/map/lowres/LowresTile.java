package de.bluecolored.bluemap.core.map.lowres;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.util.math.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LowresTile {

    public static final int HEIGHT_UNDEFINED = Integer.MIN_VALUE;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final BufferedImage texture;
    private final Vector2i size;

    public LowresTile(Vector2i tileSize) {
        this.size = tileSize.add(1, 1); // add 1 for seamless edges
        this.texture = new BufferedImage(this.size.getX(), this.size.getY() * 2, BufferedImage.TYPE_INT_ARGB);
    }

    public LowresTile(Vector2i tileSize, InputStream in) throws IOException {
        this.size = tileSize.add(1, 1); // add 1 for seamless edges
        this.texture = ImageIO.read(in);

        if (this.texture == null) {
            throw new IOException("No registered ImageReader is able to read the image-stream");
        }

        if (this.texture.getWidth() != this.size.getX() || this.texture.getHeight() != this.size.getY() * 2) {
            throw new IOException("Size of tile does not match");
        }
    }

    public void set(int x, int z, Color color, int height, int blockLight) {
        lock.readLock().lock();
        try {
            texture.setRGB(x, z, color.straight().getInt());
            texture.setRGB(x, size.getY() + z,
                    (height & 0x0000FFFF) |
                            ((blockLight << 16) & 0x00FF0000) |
                            0xFF000000
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    public Color getColor(int x, int z, Color target) {
        return target.set(texture.getRGB(x, z));
    }

    public int getHeight(int x, int z) {
        int height = texture.getRGB(x, size.getY() + z) & 0x0000FFFF;
        if (height > 0x00008000)
            return height | 0xFFFF0000;
        return height;
    }

    public int getBlockLight(int x, int z) {
        return (texture.getRGB(x, size.getY() + z) & 0x00FF0000) >> 16;
    }

    public void save(OutputStream out) throws IOException {
        lock.writeLock().lock();
        try {
            ImageIO.write(texture, "png", out);
        } finally {
            lock.writeLock().unlock();
        }
    }

}
