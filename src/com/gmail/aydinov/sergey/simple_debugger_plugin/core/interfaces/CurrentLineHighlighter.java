package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import org.eclipse.ui.texteditor.ITextEditor;

public interface CurrentLineHighlighter {
    void highlight(ITextEditor editor, int lineNumber);
    void clearHighlight(ITextEditor editor);
}
