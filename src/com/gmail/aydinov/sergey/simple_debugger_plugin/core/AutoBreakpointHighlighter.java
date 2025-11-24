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

public class AutoBreakpointHighlighter implements IDebugEventSetListener {

    @Override
    public void handleDebugEvents(DebugEvent[] events) {
        for (DebugEvent event : events) {
            if (event.getKind() == DebugEvent.SUSPEND) {
                Object source = event.getSource();
                if (source instanceof IThread) {
                    highlightCurrentLine((IThread) source);
                }
            }
        }
    }

    private void highlightCurrentLine(IThread thread) {
        if (!(thread instanceof IJavaThread)) return;
        IJavaThread javaThread = (IJavaThread) thread;

        try {
            if (javaThread.getTopStackFrame() == null) return;

            int lineNumber = javaThread.getTopStackFrame().getLineNumber();
            String fileName = javaThread.getTopStackFrame().getName();

            PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                try {
                    IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage()
                            .getActiveEditor();

                    if (editorPart instanceof ITextEditor textEditor) {
                        IAnnotationModel annotationModel = textEditor.getDocumentProvider()
                                .getAnnotationModel(textEditor.getEditorInput());

                        Annotation annotation = new Annotation("org.eclipse.jdt.debug.highlight", false, "Current line");
                        Position position = new Position(getLineOffset(textEditor, lineNumber), getLineLength(textEditor, lineNumber));
                        annotationModel.addAnnotation(annotation, position);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (DebugException e) {
            e.printStackTrace();
        }
    }

    private int getLineOffset(ITextEditor editor, int lineNumber) throws Exception {
        return editor.getDocumentProvider().getDocument(editor.getEditorInput()).getLineOffset(lineNumber - 1);
    }

    private int getLineLength(ITextEditor editor, int lineNumber) throws Exception {
        return editor.getDocumentProvider().getDocument(editor.getEditorInput()).getLineLength(lineNumber - 1);
    }
}


