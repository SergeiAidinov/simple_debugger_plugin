package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Highlights the current line in an Eclipse text editor.
 * Used to indicate where execution is currently stopped.
 */
public class CurrentLineHighlighter {

    private static final String ANNOTATION_TYPE = "com.gmail.aydinov.sergey.simple_debugger_plugin.currentLine";

    /**
     * Highlights a specific line in the given text editor.
     *
     * @param editor the text editor to highlight in
     * @param lineNumber the zero-based line number to highlight
     */
    public void highlight(ITextEditor editor, int lineNumber) {
        IAnnotationModel model = getAnnotationModel(editor);
        if (Objects.isNull(model)) return;

        // Get the document and compute the position of the line
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        int offset = 0, length = 0;
        Position position = null;
        try {
            offset = document.getLineOffset(lineNumber);
            length = document.getLineLength(lineNumber);
            position = new Position(offset, length);
        } catch (Exception ignored) {
        }

        // Add annotation
        Annotation annotation = new Annotation(ANNOTATION_TYPE, false, "Execution stopped here");
        model.addAnnotation(annotation, position);

        // Select the line in the editor
        editor.selectAndReveal(offset, length);
    }

    /**
     * Clears all previous highlights of the current line type in the given editor.
     *
     * @param editor the text editor to clear highlights from
     */
    public void clearPreviousHighlight(ITextEditor editor) {
        IAnnotationModel model = getAnnotationModel(editor);
        if (Objects.isNull(model)) return;
        List<Annotation> toRemove = new ArrayList<>();
        Iterator<?> iterator = model.getAnnotationIterator();
        while (iterator.hasNext()) {
            Annotation annotation = (Annotation) iterator.next();
            if (ANNOTATION_TYPE.equals(annotation.getType())) {
                toRemove.add(annotation);
            }
        }

        for (Annotation annotation : toRemove) {
            model.removeAnnotation(annotation);
        }
    }

    /**
     * Returns the annotation model associated with the given editor.
     *
     * @param editor the text editor
     * @return the annotation model, or null if unavailable
     */
    private IAnnotationModel getAnnotationModel(ITextEditor editor) {
        IDocumentProvider provider = editor.getDocumentProvider();
        return provider.getAnnotationModel(editor.getEditorInput());
    }
}
