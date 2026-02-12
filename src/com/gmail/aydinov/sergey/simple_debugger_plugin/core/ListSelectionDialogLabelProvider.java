package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Objects;
import org.eclipse.jface.viewers.LabelProvider;

/**
 * Provides textual representation for elements in selection dialogs.
 * Returns the element's toString() value or an empty string if the element is null.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class ListSelectionDialogLabelProvider extends LabelProvider {

    /**
     * Returns the text for the given element.
     *
     * @param element the element to render as text
     * @return the element's string representation, or empty string if element is null
     */
    @Override
    public String getText(Object element) {
        if (Objects.isNull(element)) {
            return "";
        }
        return element.toString();
    }
}
