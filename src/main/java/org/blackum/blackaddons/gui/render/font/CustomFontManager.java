package org.blackum.blackaddons.gui.render.font;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class CustomFontManager {
    private final STBTTFontinfo fontInfo;
    private final ByteBuffer fontBuffer;

    private final Map<Integer, GlyphData> glyphDataCache = new HashMap<>();
    private int cachedAscent = Integer.MIN_VALUE;

    public static class Curve {
        public float x0, y0;
        public float x1, y1;
        public float x2, y2;

        public Curve(float x0, float y0, float x1, float y1, float x2, float y2) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    public static class GlyphData {
        public final List<Curve> curves = new ArrayList<>();
        public int advance;
        public int leftSideBearing;
        public int x0, y0, x1, y1;

        public GlyphData() {}
    }

    public static class SdfGlyphData {
        public final byte[] pixels;
        public final int width;
        public final int height;
        public final int xoff;
        public final int yoff;

        public SdfGlyphData(byte[] pixels, int width, int height, int xoff, int yoff) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
            this.xoff = xoff;
            this.yoff = yoff;
        }
    }

    public CustomFontManager(ByteBuffer fontBuffer) {
        this.fontBuffer = fontBuffer;
        this.fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
            throw new RuntimeException("Failed to initialize font");
        }
    }

    public boolean hasGlyph(int codepoint) {
        return STBTruetype.stbtt_FindGlyphIndex(fontInfo, codepoint) != 0;
    }

    public float getScaleForPixelHeight(float pixels) {
        return STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, pixels);
    }

    public int getAscent() {
        if (cachedAscent == Integer.MIN_VALUE) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer ascent = stack.mallocInt(1);
                STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
                cachedAscent = ascent.get(0);
            }
        }
        return cachedAscent;
    }

    public GlyphData getGlyphData(int codepoint) {
        if (glyphDataCache.containsKey(codepoint)) {
            return glyphDataCache.get(codepoint);
        }
        GlyphData data = loadGlyphData(codepoint);
        glyphDataCache.put(codepoint, data);
        return data;
    }

    private GlyphData loadGlyphData(int codepoint) {
        int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(fontInfo, codepoint);
        if (glyphIndex == 0) return null;

        GlyphData data = new GlyphData();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advance = stack.mallocInt(1);
            IntBuffer lsb = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advance, lsb);
            data.advance = advance.get(0);
            data.leftSideBearing = lsb.get(0);

            IntBuffer x0 = stack.mallocInt(1);
            IntBuffer y0 = stack.mallocInt(1);
            IntBuffer x1 = stack.mallocInt(1);
            IntBuffer y1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphBox(fontInfo, glyphIndex, x0, y0, x1, y1);
            data.x0 = x0.get(0);
            data.y0 = y0.get(0);
            data.x1 = x1.get(0);
            data.y1 = y1.get(0);

            STBTTVertex.Buffer vertices = STBTruetype.stbtt_GetGlyphShape(fontInfo, glyphIndex);
            if (vertices != null) {
                float lastX = 0;
                float lastY = 0;
                float startX = 0;
                float startY = 0;

                for (int i = 0; i < vertices.remaining(); i++) {
                    STBTTVertex v = vertices.get(i);
                    switch (v.type()) {
                        case STBTruetype.STBTT_vmove:
                            lastX = startX = v.x();
                            lastY = startY = v.y();
                            break;
                        case STBTruetype.STBTT_vline:
                            data.curves.add(new Curve(lastX, lastY, (lastX + v.x()) / 2f, (lastY + v.y()) / 2f, v.x(), v.y()));
                            lastX = v.x();
                            lastY = v.y();
                            break;
                        case STBTruetype.STBTT_vcurve:
                            data.curves.add(new Curve(lastX, lastY, v.cx(), v.cy(), v.x(), v.y()));
                            lastX = v.x();
                            lastY = v.y();
                            break;
                        case STBTruetype.STBTT_vcubic:
                            float midX = (lastX + 3 * v.cx() + 3 * v.cx1() + v.x()) / 8f;
                            float midY = (lastY + 3 * v.cy() + 3 * v.cy1() + v.y()) / 8f;
                            data.curves.add(new Curve(lastX, lastY, (lastX + 3 * v.cx()) / 4f, (lastY + 3 * v.cy()) / 4f, midX, midY));
                            data.curves.add(new Curve(midX, midY, (3 * v.cx1() + v.x()) / 4f, (3 * v.cy1() + v.y()) / 4f, v.x(), v.y()));
                            lastX = v.x();
                            lastY = v.y();
                            break;
                    }
                }
                STBTruetype.stbtt_FreeShape(fontInfo, vertices);
            }
        }
        return data;
    }

    public SdfGlyphData getSdfGlyphData(int codepoint, float pixelHeight, int padding, int onEdgeValue, float pixelDistScale) {
        float scale = getScaleForPixelHeight(pixelHeight);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x0 = stack.mallocInt(1);
            IntBuffer y0 = stack.mallocInt(1);
            IntBuffer x1 = stack.mallocInt(1);
            IntBuffer y1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetCodepointBitmapBox(fontInfo, codepoint, scale, scale, x0, y0, x1, y1);

            int w = x1.get(0) - x0.get(0);
            int h = y1.get(0) - y0.get(0);
            if (w <= 0 || h <= 0) {
                return null;
            }

            ByteBuffer bitmap = MemoryUtil.memAlloc(w * h);
            try {
                STBTruetype.stbtt_MakeCodepointBitmap(fontInfo, bitmap, w, h, w, scale, scale, codepoint);

                byte[] alpha = new byte[w * h];
                bitmap.get(alpha);

                int outW = w + padding * 2;
                int outH = h + padding * 2;
                byte[] sdf = buildSdf(alpha, w, h, padding, onEdgeValue, pixelDistScale);
                return new SdfGlyphData(sdf, outW, outH, x0.get(0) - padding, y0.get(0) - padding);
            } finally {
                MemoryUtil.memFree(bitmap);
            }
        }
    }

    private static byte[] buildSdf(byte[] alpha, int width, int height, int padding, int onEdgeValue, float pixelDistScale) {
        int outW = width + padding * 2;
        int outH = height + padding * 2;
        byte[] out = new byte[outW * outH];
        float maxDistance = Math.max(1.0f, 255.0f / Math.max(0.001f, pixelDistScale));

        for (int y = 0; y < outH; y++) {
            int srcY = y - padding;
            for (int x = 0; x < outW; x++) {
                int srcX = x - padding;
                float alphaAtSample = sampleAlpha(alpha, width, height, srcX, srcY);
                boolean inside = alphaAtSample >= 0.5f;
                float nearest = maxDistance;
                int minX = Math.max(0, srcX - (int) Math.ceil(maxDistance));
                int maxX = Math.min(width - 1, srcX + (int) Math.ceil(maxDistance));
                int minY = Math.max(0, srcY - (int) Math.ceil(maxDistance));
                int maxY = Math.min(height - 1, srcY + (int) Math.ceil(maxDistance));

                for (int yy = minY; yy <= maxY; yy++) {
                    for (int xx = minX; xx <= maxX; xx++) {
                        float neighborAlpha = sampleAlpha(alpha, width, height, xx, yy);
                        if ((neighborAlpha >= 0.5f) == inside) {
                            continue;
                        }
                        float dx = (xx + 0.5f) - (srcX + 0.5f);
                        float dy = (yy + 0.5f) - (srcY + 0.5f);
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);
                        if (dist < nearest) {
                            nearest = dist;
                        }
                    }
                }

                if (nearest > maxDistance) {
                    nearest = maxDistance;
                }

                float edgeBias = (alphaAtSample - 0.5f) * 2.0f;
                float signed = inside ? nearest : -nearest;
                if (alphaAtSample > 0.0f && alphaAtSample < 1.0f) {
                    signed = edgeBias;
                }
                int value = Math.round(onEdgeValue + signed * pixelDistScale);
                if (value < 0) {
                    value = 0;
                } else if (value > 255) {
                    value = 255;
                }
                out[y * outW + x] = (byte) value;
            }
        }
        return out;
    }

    private static float sampleAlpha(byte[] alpha, int width, int height, int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0.0f;
        }
        return (alpha[y * width + x] & 0xFF) / 255.0f;
    }
}
