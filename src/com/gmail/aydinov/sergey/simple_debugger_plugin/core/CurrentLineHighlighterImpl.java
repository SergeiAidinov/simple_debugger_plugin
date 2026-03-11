package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.CurrentLineHighlighter;
import com.sun.jdi.Location;

/**
 * Highlights the current line in an Eclipse text editor.
 * Used to indicate where execution is currently stopped.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class CurrentLineHighlighterImpl implements CurrentLineHighlighter {

    private static final String ANNOTATION_TYPE = "com.gmail.aydinov.sergey.simple_debugger_plugin.currentLine";

  //  private final TargetApplicationRepresentation targetApplicationRepresentation;

    public CurrentLineHighlighterImpl() {
    }

    /**
     * Highlights the given line for the given location asynchronously.
     *
     * @param location the JDI location to highlight
     */
    public void highlight(Location location) {
        Display display = Display.getDefault();
        if (Objects.isNull(display) || display.isDisposed() || location == null) return;

        display.asyncExec(() -> {
            try {
                ITextEditor editor = openEditorForLocation(location);
                if (Objects.nonNull(editor)) {
                    int lineNumber = location.lineNumber() - 1;

                    // Remove previous highlight
                    clearPreviousHighlight(editor);

                    // Highlight new line
                    highlight(editor, lineNumber);
                }
            } catch (Throwable exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void highlight(ITextEditor editor, int lineNumber) {
        IAnnotationModel model = getAnnotationModel(editor);
        if (Objects.isNull(model)) return;

        try {
            IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            int offset = document.getLineOffset(lineNumber);
            int length = document.getLineLength(lineNumber);
            Position position = new Position(offset, length);

            Annotation annotation = new Annotation(ANNOTATION_TYPE, false, "Execution stopped here");
            model.addAnnotation(annotation, position);

            editor.selectAndReveal(offset, length);
        } catch (Exception ignored) {}
    }

    @Override
    public void clearHighlight(ITextEditor editor) {
        clearPreviousHighlight(editor);
    }

    private void clearPreviousHighlight(ITextEditor editor) {
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

    private IAnnotationModel getAnnotationModel(ITextEditor editor) {
        IDocumentProvider provider = editor.getDocumentProvider();
        return provider.getAnnotationModel(editor.getEditorInput());
    }

    private ITextEditor openEditorForLocation(Location location) throws Exception {
        if (location == null) return null;

        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow == null) return null;

        IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
        if (workbenchPage == null) return null;

        IFile file = TargetApplicationRepresentation.getInstance().findIFileForLocation(location);
        if (file == null) throw new IllegalStateException("Cannot map location to IFile: " + location);

        IEditorPart editorPart = IDE.openEditor(workbenchPage, file, true);
        if (editorPart instanceof ITextEditor textEditor) {
            return textEditor;
        }

        throw new IllegalStateException("Opened editor is not a text editor");
    }
}