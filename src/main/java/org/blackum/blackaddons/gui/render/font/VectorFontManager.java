package org.blackum.blackaddons.gui.render.font;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

public class VectorFontManager {
    private static VectorFontManager instance;
    private final STBTTFontinfo fontInfo;
    private final ByteBuffer fontBuffer;

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

    private VectorFontManager(ByteBuffer fontBuffer) {
        this.fontBuffer = fontBuffer;
        this.fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
            throw new RuntimeException("Failed to initialize font");
        }
    }

    public static void init(ByteBuffer fontBuffer) {
        instance = new VectorFontManager(fontBuffer);
    }

    public static VectorFontManager getInstance() {
        return instance;
    }

    public boolean hasGlyph(int codepoint) {
        return STBTruetype.stbtt_FindGlyphIndex(fontInfo, codepoint) != 0;
    }

    public float getScaleForPixelHeight(float pixels) {
        return STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, pixels);
    }

    public int getAscent() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
            return ascent.get(0);
        }
    }
    public GlyphData getGlyphData(int codepoint) {
        int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(fontInfo, codepoint);
        if (glyphIndex == 0) return null; // i'm not gonna do other symbols

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
}
