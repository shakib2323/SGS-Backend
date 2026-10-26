package com.sgs.storage;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageProcessorTest {

    private static BufferedImage marker(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.RED);
        g.fillRect(0, 0, w / 4, h / 4);   // red block in the top-left corner
        g.dispose();
        return img;
    }

    private static byte[] jpeg(BufferedImage img) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", bos);
        return bos.toByteArray();
    }

    /** Inserts an EXIF APP1 segment (big-endian) with the given orientation right after SOI. */
    private static byte[] withOrientation(byte[] jpeg, int orientation) {
        byte[] app1 = {
                (byte) 0xFF, (byte) 0xE1, 0x00, 0x22,
                'E', 'x', 'i', 'f', 0, 0,
                'M', 'M', 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08,
                0x00, 0x01,
                0x01, 0x12, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, (byte) orientation, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        };
        byte[] out = new byte[jpeg.length + app1.length];
        out[0] = jpeg[0];
        out[1] = jpeg[1];
        System.arraycopy(app1, 0, out, 2, app1.length);
        System.arraycopy(jpeg, 2, out, 2 + app1.length, jpeg.length - 2);
        return out;
    }

    private static boolean isRed(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return r > 200 && g < 80 && b < 80;
    }

    @Test
    void readsExifOrientation() throws Exception {
        byte[] plain = jpeg(marker(80, 40));
        assertEquals(1, ImageProcessor.readExifOrientation(plain));
        assertEquals(6, ImageProcessor.readExifOrientation(withOrientation(plain, 6)));
        assertEquals(8, ImageProcessor.readExifOrientation(withOrientation(plain, 8)));
    }

    @Test
    void orientation6RotatesClockwise() throws Exception {
        byte[] data = withOrientation(jpeg(marker(400, 200)), 6);
        ImageProcessor.Processed p = ImageProcessor.process(data, 1600, 480);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(p.main()));
        assertEquals(200, out.getWidth());
        assertEquals(400, out.getHeight());
        assertTrue(isRed(out.getRGB(out.getWidth() - 5, 5)), "red corner should move to top-right");
    }

    @Test
    void orientation8RotatesCounterClockwise() throws Exception {
        byte[] data = withOrientation(jpeg(marker(400, 200)), 8);
        ImageProcessor.Processed p = ImageProcessor.process(data, 1600, 480);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(p.main()));
        assertEquals(200, out.getWidth());
        assertEquals(400, out.getHeight());
        assertTrue(isRed(out.getRGB(5, out.getHeight() - 5)), "red corner should move to bottom-left");
    }

    @Test
    void downscalesButNeverUpscales() throws Exception {
        ImageProcessor.Processed big = ImageProcessor.process(jpeg(marker(4000, 3000)), 1600, 480);
        BufferedImage m = ImageIO.read(new ByteArrayInputStream(big.main()));
        BufferedImage t = ImageIO.read(new ByteArrayInputStream(big.thumb()));
        assertEquals(1600, m.getWidth());
        assertEquals(1200, m.getHeight());
        assertEquals(480, t.getWidth());
        assertEquals("jpg", big.extension());

        ImageProcessor.Processed small = ImageProcessor.process(jpeg(marker(300, 200)), 1600, 480);
        assertEquals(300, ImageIO.read(new ByteArrayInputStream(small.main())).getWidth());
    }

    @Test
    void keepsTransparentPngAsPng() throws Exception {
        BufferedImage img = new BufferedImage(100, 60, BufferedImage.TYPE_INT_ARGB); // fully transparent
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        ImageProcessor.Processed p = ImageProcessor.process(bos.toByteArray(), 600, 240);
        assertEquals("png", p.extension());
    }

    @Test
    void opaquePngBecomesJpeg() throws Exception {
        BufferedImage img = new BufferedImage(100, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 100, 60);
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        assertEquals("jpg", ImageProcessor.process(bos.toByteArray(), 600, 240).extension());
    }

    @Test
    void rejectsNonImages() {
        assertThrows(IllegalArgumentException.class,
                () -> ImageProcessor.process("<?php echo 'hi'; ?> this is definitely not an image".getBytes(), 1600, 480));
        assertThrows(IllegalArgumentException.class, () -> ImageProcessor.process(new byte[0], 1600, 480));
    }
}
