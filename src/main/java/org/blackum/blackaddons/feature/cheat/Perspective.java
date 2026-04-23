package org.blackum.blackaddons.feature.cheat;

import java.util.HashSet;
import java.util.Set;

import org.blackum.blackaddons.common.config.ConfigManager;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class Perspective {
    private static final Perspective INSTANCE = new Perspective();
    private static final int MOUSE_BIND_OFFSET = 1000;

    private final Set<Integer> pressedMouseButtons = new HashSet<>();

    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;

    private boolean active;
    private boolean keybindPressedLastTick;
    private boolean holdKeyActive;
    private CameraType previousCameraType;

    private Perspective() {
    }

    public static Perspective getInstance() {
        return INSTANCE;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> getInstance().onTick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> getInstance().deactivate());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> getInstance().deactivate());
    }

    public void setMouseButtonState(int button, boolean pressed) {
        if (pressed) {
            pressedMouseButtons.add(button);
        } else {
            pressedMouseButtons.remove(button);
        }
    }

    public void toggle() {
        if (active) {
            deactivate();
        } else {
            activate();
        }
    }

    public void activate() {
        if (active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Freecam.getInstance().deactivate();

        active = true;
        ConfigManager.data.perspectiveEnabled = true;

        previousCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();
        lastYaw = yaw;
        lastPitch = pitch;
    }

    public void deactivate() {
        active = false;
        holdKeyActive = false;
        keybindPressedLastTick = false;
        ConfigManager.data.perspectiveEnabled = false;

        Minecraft mc = Minecraft.getInstance();
        if (previousCameraType != null) {
            mc.options.setCameraType(previousCameraType);
            previousCameraType = null;
        }
    }

    private void onTick() {
        handleKeybind();
        lastYaw = yaw;
        lastPitch = pitch;
    }

    private void handleKeybind() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            keybindPressedLastTick = false;
            holdKeyActive = false;
            return;
        }

        int keyCode = ConfigManager.data.perspectiveKeyCode;
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode < 0) {
            keybindPressedLastTick = false;
            if (holdKeyActive) {
                holdKeyActive = false;
            }
            return;
        }

        boolean pressed = isBindPressed(mc, keyCode);
        if (ConfigManager.data.perspectiveHoldMode) {
            if (pressed && !keybindPressedLastTick && !active) {
                activate();
                holdKeyActive = active;
            } else if (!pressed && holdKeyActive) {
                deactivate();
            }
        } else if (pressed && !keybindPressedLastTick) {
            toggle();
        }

        keybindPressedLastTick = pressed;
    }

    private boolean isBindPressed(Minecraft mc, int keyCode) {
        if (keyCode >= MOUSE_BIND_OFFSET) {
            return pressedMouseButtons.contains(keyCode - MOUSE_BIND_OFFSET);
        }
        return InputConstants.isKeyDown(mc.getWindow(), keyCode);
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        yaw += (float) deltaX * 0.15f * ConfigManager.data.perspectiveSensitivity;
        pitch += (float) deltaY * 0.15f * ConfigManager.data.perspectiveSensitivity;
        pitch = Mth.clamp(pitch, -90.0f, 90.0f);
    }

    public boolean isActive() {
        return active;
    }

    public float getYaw(float tickDelta) {
        return Mth.rotLerp(tickDelta, lastYaw, yaw);
    }

    public float getPitch(float tickDelta) {
        return Mth.lerp(tickDelta, lastPitch, pitch);
    }

    public float getDistance() {
        return ConfigManager.data.perspectiveDistance;
    }

    public void onMouseScroll(double amount) {
        if (!active || !ConfigManager.data.perspectiveScrollEnabled) return;
        ConfigManager.data.perspectiveDistance -= (float) amount * 0.25f;
        ConfigManager.data.perspectiveDistance = Mth.clamp(ConfigManager.data.perspectiveDistance, 1.0f, 20.0f);
        ConfigManager.save();
    }
}
