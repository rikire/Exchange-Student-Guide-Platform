package in.ac.iitm.guide.media.internal;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Component;

/**
 * Re-encodes a photo (security.md): the pixels are decoded and written anew, so nothing that rode
 * behind a valid header survives, and neither does the EXIF block with its GPS position. The EXIF
 * orientation is applied first, because a phone stores a portrait photo on its side and relies on it.
 */
// trace:FR-010
@Component
public class PhotoEncoder {

    /**
     * Decoding allocates four bytes a pixel whatever the file's size, so a small file declaring huge
     * dimensions would exhaust the memory. 50 megapixels holds a 48-megapixel phone photo.
     */
    static final long LARGEST_PIXELS = 50_000_000L;

    private static final float JPEG_QUALITY = 0.9f;

    PhotoEncoder() {
        // ImageIO finds its plug-ins once, through the class loader of whoever touched it first; in
        // the packaged application that need not be the one holding TwelveMonkeys' WebP reader, which
        // is what its authors say to scan for again.
        ImageIO.scanForPlugins();
    }

    /**
     * @return the photo written as {@code type}, upright
     * @throws UnreadablePhotoException if the bytes do not decode as a picture ImageIO can read
     */
    public byte[] encode(InputStreamSource photo, AcceptedType type) throws IOException {
        var image = upright(decode(photo), orientation(photo));
        var out = new ByteArrayOutputStream();
        if (type.stored().equals("image/jpeg")) {
            writeJpeg(withoutAlpha(image), out);
        } else {
            ImageIO.write(image, "png", out);
        }
        return out.toByteArray();
    }

    private static BufferedImage decode(InputStreamSource photo) throws IOException {
        try (var in = ImageIO.createImageInputStream(photo.getInputStream())) {
            var readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new UnreadablePhotoException();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > LARGEST_PIXELS) {
                    throw new UnreadablePhotoException();
                }
                return reader.read(0);
            } catch (IIOException e) {
                throw new UnreadablePhotoException();
            } finally {
                reader.dispose();
            }
        }
    }

    /** @return the EXIF orientation, 1 to 8; 1, "as stored", when the photo carries none */
    private static int orientation(InputStreamSource photo) throws IOException {
        try (var in = photo.getInputStream()) {
            var exif = ImageMetadataReader.readMetadata(in).getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exif == null || !exif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return 1;
            }
            return exif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (ImageProcessingException | MetadataException e) {
            // The pixels decoded; metadata the reader cannot parse leaves the photo as stored, which is
            // what every viewer that ignores EXIF shows as well.
            return 1;
        }
    }

    /** The eight EXIF orientations as the transform that undoes each (TIFF 6.0, tag 274). */
    private static BufferedImage upright(BufferedImage image, int orientation) {
        if (orientation < 2 || orientation > 8) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        var t = new AffineTransform();
        switch (orientation) {
            case 2 -> {
                t.scale(-1, 1);
                t.translate(-w, 0);
            }
            case 3 -> {
                t.translate(w, h);
                t.rotate(Math.PI);
            }
            case 4 -> {
                t.scale(1, -1);
                t.translate(0, -h);
            }
            case 5 -> {
                t.rotate(-Math.PI / 2);
                t.scale(-1, 1);
            }
            case 6 -> {
                t.translate(h, 0);
                t.rotate(Math.PI / 2);
            }
            case 7 -> {
                t.scale(-1, 1);
                t.translate(-h, 0);
                t.translate(0, w);
                t.rotate(3 * Math.PI / 2);
            }
            default -> {
                t.translate(0, w);
                t.rotate(3 * Math.PI / 2);
            }
        }
        var turned = orientation >= 5;
        var target = new BufferedImage(
                turned ? h : w, turned ? w : h, image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType());
        return new AffineTransformOp(t, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(image, target);
    }

    private static BufferedImage withoutAlpha(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return image;
        }
        var rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = rgb.createGraphics();
        graphics.drawImage(image, 0, 0, Color.WHITE, null);
        graphics.dispose();
        return rgb;
    }

    private static void writeJpeg(BufferedImage image, ByteArrayOutputStream out) throws IOException {
        var writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try (var target = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(target);
            var parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }

    /** Bytes that carry a photo's signature and do not decode as one. */
    public static class UnreadablePhotoException extends IOException {}
}
