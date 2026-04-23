package org.blackum.blackaddons.client.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.blackum.blackaddons.common.util.mc.McCompat;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
//? if < 1.21.11 {
/*import net.minecraft.resources.ResourceLocation;*/
//?} else
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class BlackaddonsRenderPipelines {

    private static final List<RenderPipeline> PIPELINES = new ArrayList<>();

    public static final RenderPipeline CUSTOM_TEXT = add(RenderPipeline.builder()
            //? if < 1.21.11 {
            /*.withLocation(ResourceLocation.fromNamespaceAndPath("blackaddons", "custom_text"))*/
            /*.withVertexShader(ResourceLocation.fromNamespaceAndPath("blackaddons", "core/custom_text"))*/
            /*.withFragmentShader(ResourceLocation.fromNamespaceAndPath("blackaddons", "core/custom_text"))*/
            //?}
            //? if >= 1.21.11 {
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "custom_text"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            //?}
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build());

    public static final RenderPipeline CUSTOM_TEXT_DEPTH = add(RenderPipeline.builder()
            //? if < 1.21.11 {
            /*.withLocation(ResourceLocation.fromNamespaceAndPath("blackaddons", "custom_text_depth"))*/
            /*.withVertexShader(ResourceLocation.fromNamespaceAndPath("blackaddons", "core/custom_text"))*/
            /*.withFragmentShader(ResourceLocation.fromNamespaceAndPath("blackaddons", "core/custom_text"))*/
            //?}
            //? if >= 1.21.11 {
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "custom_text_depth"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/custom_text"))
            //?}
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthBias(-1.0F, -10.0F)
            .build());

    public static final RenderPipeline VECTOR_TEXT = add(RenderPipeline.builder()
            //? if < 1.21.11 {
            /*.withLocation(ResourceLocation.fromNamespaceAndPath("blackaddons", "vector_text"))*/
            /*.withVertexShader(ResourceLocation.fromNamespaceAndPath("blackaddons", "core/vector_text"))*/
            /*.withFragmentShader(ResourceLocation.fromNamespaceAndPath("blackaddons", "core/vector_text"))*/
            //?}
            //? if >= 1.21.11 {
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "vector_text"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/vector_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/vector_text"))
            //?}
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build());

    public static final RenderPipeline ROUNDED_FILL = add(RenderPipeline.builder()
            //? if < 1.21.11 {
            /*.withLocation(ResourceLocation.fromNamespaceAndPath("blackaddons", "rounded_fill"))*/
            /*.withVertexShader(ResourceLocation.fromNamespaceAndPath("blackaddons", "core/rounded_fill"))*/
            /*.withFragmentShader(ResourceLocation.fromNamespaceAndPath("blackaddons", "core/rounded_fill"))*/
            //?}
            //? if >= 1.21.11 {
            .withLocation(Identifier.fromNamespaceAndPath("blackaddons", "rounded_fill"))
            .withVertexShader(Identifier.fromNamespaceAndPath("blackaddons", "core/rounded_fill"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("blackaddons", "core/rounded_fill"))
            //?}
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build());

    private static RenderPipeline add(RenderPipeline pipeline) {
        PIPELINES.add(pipeline);
        return pipeline;
    }

    public static void precompile() {
        GpuDevice device = RenderSystem.getDevice();
        ResourceManager resources = Minecraft.getInstance().getResourceManager();

        for (RenderPipeline pipeline : PIPELINES) {
            device.precompilePipeline(pipeline, (location, shaderType) -> {
                String extension = shaderType == ShaderType.VERTEX ? ".vsh" : ".fsh";
                String shaderPath = "shaders/" + location.getPath() + extension;
                Resource resource = McCompat.findResource(resources, location.getNamespace(), shaderPath)
                        .orElseThrow(() -> new RuntimeException("Could not find shader: " + location.getNamespace() + ":" + shaderPath));

                try (var inputStream = resource.open()) {
                    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load shader: " + location.getNamespace() + ":" + shaderPath, e);
                }
            });
        }
    }
}
