package in.ac.iitm.guide.media;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.springframework.core.io.ClassPathResource;

/**
 * Files for the media tests, made in code so a test names the size and kind it needs. The two that
 * the JDK cannot make, a WebP and a JPEG with an EXIF orientation, are in {@code src/test/resources/media}.
 */
public final class MediaTestFiles {

    private MediaTestFiles() {}

    /**
     * The media root every test shares, with NFR-001's limits made small (64 KB a photo, 16 KB a
     * document, 32 KB a video, 256 KB the volume): both in {@code src/test/resources/config/application.yml}.
     */
    public static final Path ROOT = Path.of("target/test-media");

    public static List<Path> files(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).toList();
        }
    }

    public static void empty(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(root)) {
                    Files.delete(path);
                }
            }
        }
    }

    public static byte[] jpeg(int width, int height) {
        return image("jpg", filled(width, height));
    }

    public static byte[] png(int width, int height) {
        return image("png", filled(width, height));
    }

    /** Random pixels, which PNG cannot compress: the file is about three bytes a pixel. */
    public static byte[] noisyPng(int width, int height) {
        var random = new Random(7);
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (var x = 0; x < width; x++) {
            for (var y = 0; y < height; y++) {
                image.setRGB(x, y, random.nextInt(0x1000000));
            }
        }
        return image("png", image);
    }

    /** A file Tika reads as {@code application/pdf}, padded to exactly {@code size} bytes. */
    public static byte[] pdf(int size) {
        return padded("%PDF-1.7\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n".getBytes(), size);
    }

    /** An ISO base media header with the {@code mp42} brand, which Tika reads as {@code video/mp4}. */
    public static byte[] mp4(int size) {
        var header =
                ByteBuffer.allocate(24).putInt(24).put("ftypmp42".getBytes()).putInt(0);
        header.put("mp42isom".getBytes());
        return padded(header.array(), size);
    }

    /** A ZIP holding empty entries of these names. */
    public static byte[] zip(String... entries) {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            for (var entry : entries) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write("<x/>".getBytes());
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    public static byte[] resource(String path) {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BufferedImage filled(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new Color(200, 30, 30));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private static byte[] image(String format, BufferedImage image) {
        var out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, format, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static byte[] padded(byte[] start, int size) {
        var bytes = new byte[size];
        System.arraycopy(start, 0, bytes, 0, Math.min(start.length, size));
        return bytes;
    }
}
