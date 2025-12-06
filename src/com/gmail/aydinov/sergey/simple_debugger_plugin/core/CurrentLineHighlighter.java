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

public class CurrentLineHighlighter {

	private static final String ANNOTATION_TYPE = "com.gmail.aydinov.sergey.simple_debugger_plugin.currentLine";

	public void highlight(ITextEditor editor, int lineNumber) {
		IAnnotationModel model = getAnnotationModel(editor);
		if (Objects.isNull(model))
			return;
		//clearPreviousHighlight(model);
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		int offset = 0, length = 0;
		Position position = null;
		try {
			offset = document.getLineOffset(lineNumber);
			length = document.getLineLength(lineNumber);
			position = new Position(offset, length);
		} catch (Exception e) {
		}
		Annotation annotation = new Annotation(ANNOTATION_TYPE, false, "Execution stopped here");
		model.addAnnotation(annotation, position);
		editor.selectAndReveal(offset, length);
	}

	private IAnnotationModel getAnnotationModel(ITextEditor editor) {
		IDocumentProvider provider = editor.getDocumentProvider();
		return provider.getAnnotationModel(editor.getEditorInput());
	}

	public void clearPreviousHighlight(ITextEditor editor) {
		IAnnotationModel model = getAnnotationModel(editor);
		if (Objects.isNull(model))
			return;
		List<Annotation> toRemove = new ArrayList<>();
		Iterator<?> iterator = model.getAnnotationIterator();
		while (iterator.hasNext()) {
			Annotation a = (Annotation) iterator.next();
			if (ANNOTATION_TYPE.equals(a.getType())) {
				toRemove.add(a);
			}
		}

		for (Annotation annotation : toRemove) {
			model.removeAnnotation(annotation);
		}
	}
}