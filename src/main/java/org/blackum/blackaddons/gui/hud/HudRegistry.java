package org.blackum.blackaddons.gui.hud;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public final class HudRegistry {
    private static final Map<String, HudElement> ELEMENTS = new LinkedHashMap<>();
    private static boolean installed = false;

    private HudRegistry() {
    }

    public static void register(HudElement element) {
        ELEMENTS.put(element.id(), element);
    }

    public static HudElement get(String id) {
        return ELEMENTS.get(id);
    }

    public static Collection<HudElement> all() {
        return Collections.unmodifiableCollection(ELEMENTS.values());
    }

    public static void install() {
        if (installed) return;
        installed = true;
        HudRenderCallback.EVENT.register((graphics, tracker) -> {
            for (HudElement e : ELEMENTS.values()) {
                if (!e.enabled()) continue;
                e.render(graphics, tracker);
            }
        });
    }
}
