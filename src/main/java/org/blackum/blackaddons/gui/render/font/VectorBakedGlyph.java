package org.blackum.blackaddons.gui.render.font;

import org.blackum.blackaddons.client.render.BlackaddonsRenderPipelines;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
//? if < 1.21.11 {
/*import net.minecraft.client.renderer.RenderType;*/
//?} else
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Style;

public class VectorBakedGlyph implements BakedGlyph {
    private final int codepoint;
    private final VectorFontManager.GlyphData glyphData;

    public VectorBakedGlyph(int codepoint, VectorFontManager.GlyphData glyphData) {
        this.codepoint = codepoint;
        this.glyphData = glyphData;
    }

    @Override
    public GlyphInfo info() {
        return new GlyphInfo() {
            @Override
            public float getAdvance() {
                float size = ConfigManager.data.vectorTextScale;
                float scale = VectorFontManager.getInstance().getScaleForPixelHeight(size);
                return (glyphData.advance * scale);
            }
        };
    }

    //? if < 1.21.11 {
    /*@Override
    public TextRenderable createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
        return new VectorTextRenderable(codepoint, glyphData, x, y, color, shadowColor, style, boldOffset, shadowOffset);
    }*/
    //?}

    //? if >= 1.21.11 {
    @Override
    public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
        return new VectorTextRenderable(codepoint, glyphData, x, y, color, shadowColor, style, boldOffset, shadowOffset);
    }
    //?}

    //? if < 1.21.11 {
    /*public static class VectorTextRenderable implements TextRenderable {*/
    //?}
    //? if >= 1.21.11 {
    public static class VectorTextRenderable implements TextRenderable.Styled {
    //?}
        private final int codepoint;
        private final VectorFontManager.GlyphData glyphData;
        private final float x, y;
        private final int color, shadowColor;
        private final Style style;
        private final float boldOffset, shadowOffset;

        public VectorTextRenderable(int codepoint, VectorFontManager.GlyphData glyphData, float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
            this.codepoint = codepoint;
            this.glyphData = glyphData;
            this.x = x;
            this.y = y;
            int argb = color;
            if (style != null && style.getColor() != null) {
                int styleRgb = style.getColor().getValue();
                int alpha = (argb & 0xFF000000);
                argb = alpha | (styleRgb & 0x00FFFFFF);
            }
            this.color = (argb & 0xFF000000) == 0 ? (argb | 0xFF000000) : argb;
            this.shadowColor = (shadowColor & 0xFF000000) == 0 ? (shadowColor | 0xFF000000) : shadowColor;
            this.style = style;
            this.boldOffset = boldOffset;
            this.shadowOffset = shadowOffset;
        }

        //? if >= 1.21.11 {
        @Override
        public Style style() {
            return this.style;
        }
        //?}

        private float getScale() {
            float size = ConfigManager.data.vectorTextScale;
            return VectorFontManager.getInstance().getScaleForPixelHeight(size);
        }

        private float getBaseline() {
            return (VectorFontManager.getInstance().getAscent() * getScale()) - 2.5f;
        }

        @Override
        public void render(Matrix4f matrix4f, VertexConsumer vertexConsumer, int light, boolean isGui) {
            if (glyphData.curves.isEmpty()) {
                return;
            }
            float scale = getScale();
            float baseline = getBaseline();
            float x0 = x + glyphData.x0 * scale;
            float x1 = x + glyphData.x1 * scale;
            float y0 = y + baseline - glyphData.y1 * scale;
            float y1 = y + baseline - glyphData.y0 * scale;

            float bo = style != null && style.isBold() ? boldOffset : 0;
            
            Vector4f v1 = new Vector4f(x0 + bo, y0, 0, 1).mul(matrix4f);
            Vector4f v2 = new Vector4f(x0 + bo, y1, 0, 1).mul(matrix4f);
            Vector4f v3 = new Vector4f(x1 + bo, y1, 0, 1).mul(matrix4f);
            Vector4f v4 = new Vector4f(x1 + bo, y0, 0, 1).mul(matrix4f);

            vertexConsumer.addVertex(v1.x(), v1.y(), v1.z()).setColor(color).setUv(0, 0).setLight(light);
            vertexConsumer.addVertex(v2.x(), v2.y(), v2.z()).setColor(color).setUv(0, 1).setLight(light);
            vertexConsumer.addVertex(v3.x(), v3.y(), v3.z()).setColor(color).setUv(1, 1).setLight(light);
            vertexConsumer.addVertex(v4.x(), v4.y(), v4.z()).setColor(color).setUv(1, 0).setLight(light);
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            return VectorFontRenderer.getInstance().getLayer(codepoint, glyphData);
        }

        @Override
        public GpuTextureView textureView() {
            DynamicTexture texture = VectorFontRenderer.getInstance().getTexture(codepoint, glyphData);
            return texture.getTextureView();
        }

        @Override
        public RenderPipeline guiPipeline() {
            return BlackaddonsRenderPipelines.VECTOR_TEXT;
        }

        @Override
        public float left() {
            return x + glyphData.x0 * getScale();
        }

        @Override
        public float top() {
            return y + getBaseline() - glyphData.y1 * getScale();
        }

        @Override
        public float right() {
            return x + glyphData.x1 * getScale();
        }

        @Override
        public float bottom() {
            return y + getBaseline() - glyphData.y0 * getScale();
        }
    }
}
