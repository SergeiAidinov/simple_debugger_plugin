package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class CurrentLineHighlighter {

	private static final String ANNOTATION_TYPE =
		    "com.gmail.aydinov.sergey.simple_debugger_plugin.currentLine";

    public void highlight(ITextEditor editor, int lineNumber) {
        try {
            IAnnotationModel model = getAnnotationModel(editor);
            if (model == null) return;

            clearPreviousHighlight(model);

            IDocument document = editor.getDocumentProvider()
                                       .getDocument(editor.getEditorInput());

            int offset = document.getLineOffset(lineNumber);
            int length = document.getLineLength(lineNumber);

            Position position = new Position(offset, length);

            Annotation annotation = new Annotation(
                    ANNOTATION_TYPE,
                    false,
                    "Execution stopped here"
            );

            model.addAnnotation(annotation, position);
            editor.selectAndReveal(offset, length);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private IAnnotationModel getAnnotationModel(ITextEditor editor) {
        IDocumentProvider provider = editor.getDocumentProvider();
        return provider.getAnnotationModel(editor.getEditorInput());
    }

    private void clearPreviousHighlight(IAnnotationModel model) {
        List<Annotation> toRemove = new ArrayList<>();

        Iterator<?> it = model.getAnnotationIterator();
        while (it.hasNext()) {
            Annotation a = (Annotation) it.next();
            if (ANNOTATION_TYPE.equals(a.getType())) {
                toRemove.add(a);
            }
        }

        for (Annotation ann : toRemove) {
            model.removeAnnotation(ann);
        }
    }
}
