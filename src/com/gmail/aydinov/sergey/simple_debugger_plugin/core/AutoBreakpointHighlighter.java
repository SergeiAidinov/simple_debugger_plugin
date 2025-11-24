package com.gmail.aydinov.sergey.simple_debugger_plugin.core;


import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.swt.widgets.Display;

public class AutoBreakpointHighlighter {

    private Annotation currentAnnotation;
    private int currentLine = -1;

    public void highlightLine(ITextEditor editor, int lineNumber) {
        if (editor == null) return;

        IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (annotationModel == null || document == null) return;

        try {
            int offset = document.getLineOffset(lineNumber);
            int length = document.getLineLength(lineNumber);

            Display.getDefault().asyncExec(() -> {
                // Снять старую аннотацию
                if (currentAnnotation != null) {
                    annotationModel.removeAnnotation(currentAnnotation);
                }

                // Добавить новую
                currentAnnotation = new Annotation("org.eclipse.debug.ui.breakpoint", false, "Stopped here");
                annotationModel.addAnnotation(currentAnnotation, new Position(offset, length));
                currentLine = lineNumber;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearHighlight(ITextEditor editor) {
        if (editor == null || currentAnnotation == null) return;

        IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        if (annotationModel != null) {
            Display.getDefault().asyncExec(() -> {
                annotationModel.removeAnnotation(currentAnnotation);
                currentAnnotation = null;
                currentLine = -1;
            });
        }
    }
}



