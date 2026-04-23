package org.blackum.blackaddons.gui.screen.feature;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;

public class ModOrganizer {

    public static class ModGroup {
        public final String groupName;
        public final String parentId;
        public final List<ModInfo> mods = new ArrayList<>();
        public boolean allSelected = false;

        public ModGroup(String groupName, String parentId) {
            this.groupName = groupName;
            this.parentId = parentId;
        }
    }

    public static class ModInfo {
        public final ModContainer mod;
        public final String id;
        public final String name;
        public final boolean isLibrary;
        public String parentId;
        public final Set<String> dependencies = new HashSet<>();
        public final Set<String> dependents = new HashSet<>();

        public ModInfo(ModContainer mod) {
            this.mod = mod;
            this.id = mod.getMetadata().getId();
            this.name = mod.getMetadata().getName();

            this.isLibrary = checkLibrary(mod);
            this.parentId = findParent(mod);

            for (ModDependency dep : mod.getMetadata().getDependencies()) {
                if (dep.getKind() == ModDependency.Kind.DEPENDS ||
                        dep.getKind() == ModDependency.Kind.RECOMMENDS) {
                    dependencies.add(dep.getModId());
                }
            }
        }

        private boolean checkLibrary(ModContainer mod) {
            ModMetadata metadata = mod.getMetadata();
            String id = metadata.getId();
            String name = metadata.getName();

            CustomValue modMenuValue = metadata.getCustomValue("modmenu");
            if (modMenuValue != null && modMenuValue.getType() == CustomValue.CvType.OBJECT) {
                CustomValue.CvObject modMenuObject = modMenuValue.getAsObject();
                CustomValue badgesCv = modMenuObject.get("badges");
                if (badgesCv != null && badgesCv.getType() == CustomValue.CvType.ARRAY) {
                    for (CustomValue badge : badgesCv.getAsArray()) {
                        if (badge.getType() == CustomValue.CvType.STRING && "library".equals(badge.getAsString())) {
                            return true;
                        }
                    }
                }
            }
            if (metadata.containsCustomValue("fabric-api:module-lifecycle")) {
                return true;
            }
            if (id.startsWith("fabric-") && id.contains("-api")) {
                return true;
            }

            if (metadata.containsCustomValue("fabric-loom:generated")) {
                return true;
            }
            if ("java".equals(id)) {
                return true;
            }

            if (id.startsWith("mm_") || id.contains("shedaniel") || id.contains("jarvis")
                    || id.contains("cloth-config")) {
                return true;
            }
            String lowerId = id.toLowerCase(Locale.ROOT);
            String lowerName = name.toLowerCase(Locale.ROOT);
            String type = metadata.getType();

            return "builtin".equals(type) ||
                    lowerId.contains("library") ||
                    lowerId.contains("api") ||
                    lowerId.contains("lib") ||
                    lowerId.contains("config") ||
                    lowerName.contains("library") ||
                    lowerName.contains("api") ||
                    lowerName.contains("config") ||
                    lowerId.contains("kotlin") ||
                    lowerName.contains("kotlin");
        }

        private String findParent(ModContainer mod) {
            ModMetadata metadata = mod.getMetadata();
            String id = metadata.getId();

            CustomValue modMenuValue = metadata.getCustomValue("modmenu");
            if (modMenuValue != null && modMenuValue.getType() == CustomValue.CvType.OBJECT) {
                CustomValue.CvObject modMenuObject = modMenuValue.getAsObject();
                CustomValue parentCv = modMenuObject.get("parent");
                if (parentCv != null) {
                    if (parentCv.getType() == CustomValue.CvType.STRING) {
                        return parentCv.getAsString();
                    } else if (parentCv.getType() == CustomValue.CvType.OBJECT) {
                        CustomValue.CvObject parentObj = parentCv.getAsObject();
                        CustomValue idCv = parentObj.get("id");
                        if (idCv != null && idCv.getType() == CustomValue.CvType.STRING) {
                            return idCv.getAsString();
                        }
                    }
                }
            }

            boolean isGenerated = metadata.containsCustomValue("fabric-loom:generated") &&
                    metadata.getCustomValue("fabric-loom:generated").getType() == CustomValue.CvType.BOOLEAN &&
                    metadata.getCustomValue("fabric-loom:generated").getAsBoolean();

            if (isGenerated && mod.getContainingMod().isPresent()) {
                return mod.getContainingMod().get().getMetadata().getId();
            }

            if (id.startsWith("fabric") && metadata.containsCustomValue("fabric-api:module-lifecycle")) {
                if (FabricLoader.getInstance().isModLoaded("fabric-api")) {
                    return "fabric-api";
                } else if (FabricLoader.getInstance().isModLoaded("fabric")) {
                    return "fabric";
                }
            }

            return null;
        }
    }

    public static class OrganizedMods {
        public ModGroup minecraftGroup = null;
        public final List<ModGroup> userGroups = new ArrayList<>();
        public final List<ModGroup> libraryGroups = new ArrayList<>();
        public final Map<String, ModInfo> allMods = new HashMap<>();
        public final Map<String, ModGroup> groupMap = new HashMap<>();
    }

    public static OrganizedMods organizeMods() {
        OrganizedMods result = new OrganizedMods();

        List<ModInfo> allModInfos = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            ModInfo info = new ModInfo(mod);
            allModInfos.add(info);
            result.allMods.put(info.id, info);
        }

        for (ModInfo info : allModInfos) {
            for (String depId : info.dependencies) {
                ModInfo dep = result.allMods.get(depId);
                if (dep != null) {
                    dep.dependents.add(info.id);
                }
            }
        }

        for (ModInfo info : allModInfos) {
            String parentId = info.parentId;

            if (parentId != null && !result.allMods.containsKey(parentId)) {
                parentId = null;
            }

            String groupKey = (parentId == null) ? info.id : parentId;
            ModInfo parentInfo = result.allMods.get(groupKey);

            ModGroup group;
            if (result.groupMap.containsKey(groupKey)) {
                group = result.groupMap.get(groupKey);
            } else {
                String groupName = (parentInfo != null) ? parentInfo.name : info.name;
                group = new ModGroup(groupName, groupKey);
                result.groupMap.put(groupKey, group);

                if ("minecraft".equals(groupKey)) {
                    result.minecraftGroup = group;
                } else {
                    boolean isLibrary = (parentInfo != null) ? parentInfo.isLibrary : info.isLibrary;

                    if (groupKey.startsWith("fabric") || "fabricloader".equals(groupKey) || "java".equals(groupKey)) {
                        isLibrary = true;
                    }

                    if (isLibrary) {
                        result.libraryGroups.add(group);
                    } else {
                        result.userGroups.add(group);
                    }
                }
            }

            group.mods.add(info);
        }

        result.userGroups.sort(Comparator.comparing(g -> g.groupName.toLowerCase(Locale.ROOT)));
        result.libraryGroups.sort(Comparator.comparing(g -> g.groupName.toLowerCase(Locale.ROOT)));

        for (ModGroup group : result.userGroups)
            sortGroupMods(group);
        for (ModGroup group : result.libraryGroups)
            sortGroupMods(group);
        if (result.minecraftGroup != null)
            sortGroupMods(result.minecraftGroup);

        return result;
    }

    private static void sortGroupMods(ModGroup group) {
        group.mods.sort((m1, m2) -> {
            if (m1.id.equals(group.parentId))
                return -1;
            if (m2.id.equals(group.parentId))
                return 1;
            return m1.name.compareToIgnoreCase(m2.name);
        });
    }
}
