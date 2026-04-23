package org.blackum.blackaddons;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class Blackaddons implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("blackaddons");
    public static Runnable guiOpener;
    public static Runnable testMenuOpener;
    public static Runnable mainGuiOpener;
    public static Consumer<Screen> screenOpener;
    public static Consumer<String> notificationTrigger;

    @Override
    public void onInitialize() {
        LOGGER.info("Initialization completed");
    }

    private int executeStatus(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(
                Component.literal("§0Black§7Addons is §arunning!"));
        return 1;
    }

    private int executeOpenMainGui(CommandContext<CommandSourceStack> context) {
        if (mainGuiOpener != null) {
            mainGuiOpener.run();
        }
        return 1;
    }

    private int executeOpenGui(CommandContext<CommandSourceStack> context) {
        if (guiOpener != null) {
            guiOpener.run();
        }
        return 1;
    }

    private int executeOpenTestMenu(CommandContext<CommandSourceStack> context) {
        if (testMenuOpener != null) {
            testMenuOpener.run();
        }
        return 1;
    }

    private int executeNotify(CommandContext<CommandSourceStack> context) {
        if (notificationTrigger != null) {
            String message = StringArgumentType.getString(context, "message");
            notificationTrigger.accept(message);
        }
        return 1;
    }
}
