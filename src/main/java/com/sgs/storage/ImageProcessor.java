package com.sgs.storage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Pure-Java image pipeline (no Spring): validates by actually decoding, fixes EXIF rotation,
 * resizes, and re-encodes. Re-encoding strips metadata and any payload hidden in the upload.
 * Throws IllegalArgumentException with a user-safe message when the file is rejected.
 */
public final class ImageProcessor {
    public static final long MAX_PIXELS = 40_000_000L;
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "png", "gif", "webp");

    public record Processed(byte[] main, byte[] thumb, String extension) {}

    private ImageProcessor() {}

    public static Processed process(byte[] data, int maxWidth, int thumbWidth) {
        BufferedImage decoded = decode(data);
        int orientation = readExifOrientation(data);

        BufferedImage main = resize(decoded, maxWidth);
        main = applyOrientation(main, orientation);

        boolean transparent = main.getColorModel().hasAlpha() && usesTransparency(main);
        if (!transparent) main = flattenToRgb(main);

        BufferedImage thumb = resize(main, thumbWidth);
        if (!transparent) thumb = flattenToRgb(thumb);

        try {
            if (transparent) {
                return new Processed(encodePng(main), encodePng(thumb), "png");
            }
            return new Processed(encodeJpeg(main, 0.84f), encodeJpeg(thumb, 0.80f), "jpg");
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not process the image. Try another file.");
        }
    }

    // ---------- decode ----------

    public static BufferedImage decode(byte[] data) {
        if (data == null || data.length < 16) {
            throw new IllegalArgumentException("The uploaded file is empty or not an image.");
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Unsupported or corrupt image. Please upload JPG, PNG or WebP.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName() == null ? "" : reader.getFormatName().toLowerCase();
                if (!ALLOWED_FORMATS.contains(format)) {
                    throw new IllegalArgumentException("Unsupported image format. Please upload JPG, PNG or WebP.");
                }
                reader.setInput(in, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || (long) w * h > MAX_PIXELS) {
                    throw new IllegalArgumentException("Image dimensions are too large (max 40 megapixels).");
                }
                BufferedImage img = reader.read(0);
                if (img == null) throw new IllegalArgumentException("Unsupported or corrupt image.");
                return img;
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("Unsupported or corrupt image. Please upload JPG, PNG or WebP.");
        }
    }

    // ---------- resize ----------

    /** Downscales to at most maxWidth (never upscales), halving step by step for quality. */
    public static BufferedImage resize(BufferedImage src, int maxWidth) {
        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage cur = redraw(src, src.getWidth(), src.getHeight(), type);
        while (cur.getWidth() > maxWidth) {
            int nw = Math.max(maxWidth, cur.getWidth() / 2);
            int nh = Math.max(1, (int) Math.round(cur.getHeight() * (nw / (double) cur.getWidth())));
            cur = redraw(cur, nw, nh, type);
        }
        return cur;
    }

    private static BufferedImage redraw(BufferedImage src, int w, int h, int type) {
        BufferedImage out = new BufferedImage(w, h, type);
        Graphics2D g = out.createGraphics();
        try {
            if (type == BufferedImage.TYPE_INT_RGB) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, w, h);
            }
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    public static boolean usesTransparency(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                if ((row[x] >>> 24) != 0xFF) return true;
            }
        }
        return false;
    }

    private static BufferedImage flattenToRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        return redraw(src, src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    // ---------- EXIF orientation ----------

    /** Returns EXIF orientation 1..8 for JPEG data, or 1 when absent/unknown. */
    public static int readExifOrientation(byte[] d) {
        if (d == null || d.length < 4 || (d[0] & 0xFF) != 0xFF || (d[1] & 0xFF) != 0xD8) return 1;
        int p = 2;
        while (p + 4 <= d.length) {
            if ((d[p] & 0xFF) != 0xFF) return 1;
            int marker = d[p + 1] & 0xFF;
            if (marker == 0xDA || marker == 0xD9) return 1;                 // start of scan / end of image
            int len = ((d[p + 2] & 0xFF) << 8) | (d[p + 3] & 0xFF);
            if (len < 2) return 1;
            if (marker == 0xE1 && p + 10 <= d.length
                    && d[p + 4] == 'E' && d[p + 5] == 'x' && d[p + 6] == 'i' && d[p + 7] == 'f'
                    && d[p + 8] == 0 && d[p + 9] == 0) {
                int t = p + 10;                                             // TIFF header
                if (t + 8 > d.length) return 1;
                boolean le = d[t] == 'I' && d[t + 1] == 'I';
                boolean be = d[t] == 'M' && d[t + 1] == 'M';
                if (!le && !be) return 1;
                long ifdOffset = u32(d, t + 4, le);
                long ifd = t + ifdOffset;
                if (ifd < 0 || ifd + 2 > d.length) return 1;
                int n = u16(d, (int) ifd, le);
                for (int i = 0; i < n; i++) {
                    int e = (int) ifd + 2 + i * 12;
                    if (e + 12 > d.length) return 1;
                    if (u16(d, e, le) == 0x0112) {
                        int v = u16(d, e + 8, le);
                        return (v >= 1 && v <= 8) ? v : 1;
                    }
                }
                return 1;
            }
            p += 2 + len;
        }
        return 1;
    }

    private static int u16(byte[] d, int o, boolean le) {
        int a = d[o] & 0xFF;
        int b = d[o + 1] & 0xFF;
        return le ? (b << 8) | a : (a << 8) | b;
    }

    private static long u32(byte[] d, int o, boolean le) {
        long b0 = d[o] & 0xFF, b1 = d[o + 1] & 0xFF, b2 = d[o + 2] & 0xFF, b3 = d[o + 3] & 0xFF;
        return le ? (b3 << 24) | (b2 << 16) | (b1 << 8) | b0 : (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    /** Pixel-exact orientation fix. Maps each source pixel (x,y) to its upright position. */
    public static BufferedImage applyOrientation(BufferedImage src, int orientation) {
        if (orientation < 2 || orientation > 8) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        boolean swap = orientation >= 5;
        int nw = swap ? h : w;
        int nh = swap ? w : h;
        int[] in = src.getRGB(0, 0, w, h, null, 0, w);
        int[] out = new int[nw * nh];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int nx;
                int ny;
                switch (orientation) {
                    case 2 -> { nx = w - 1 - x; ny = y; }
                    case 3 -> { nx = w - 1 - x; ny = h - 1 - y; }
                    case 4 -> { nx = x; ny = h - 1 - y; }
                    case 5 -> { nx = y; ny = x; }
                    case 6 -> { nx = h - 1 - y; ny = x; }
                    case 7 -> { nx = h - 1 - y; ny = w - 1 - x; }
                    default -> { nx = y; ny = w - 1 - x; }   // 8
                }
                out[ny * nw + nx] = in[y * w + x];
            }
        }
        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage dst = new BufferedImage(nw, nh, type);
        dst.setRGB(0, 0, nw, nh, out, 0, nw);
        return dst;
    }

    // ---------- encode ----------

    public static byte[] encodeJpeg(BufferedImage rgb, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ImageOutputStream out = ImageIO.createImageOutputStream(bos)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            writer.setOutput(out);
            writer.write(null, new IIOImage(rgb, null, null), param);
            out.flush();
            return bos.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    public static byte[] encodePng(BufferedImage img) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (!ImageIO.write(img, "png", bos)) throw new IOException("No PNG writer");
            return bos.toByteArray();
        }
    }
}
