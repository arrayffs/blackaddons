package org.blackum.blackaddons.gui.widget.editor;

import java.util.List;

import net.minecraft.network.chat.Style;

public interface SyntaxHighlighter {
    List<Style> highlight(String line);
}
