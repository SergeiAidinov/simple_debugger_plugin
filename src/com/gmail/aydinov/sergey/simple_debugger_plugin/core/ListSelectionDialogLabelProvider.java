package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import org.eclipse.jface.viewers.LabelProvider;

public class ListSelectionDialogLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
        if (element == null) return "";
        return element.toString();
    }
}
