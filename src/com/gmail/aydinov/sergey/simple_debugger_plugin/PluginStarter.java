package com.gmail.aydinov.sergey.simple_debugger_plugin;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jface.text.source.Annotation;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow;

public class PluginStarter implements IStartup {
	
	private Annotation currentAnnotation = null;

	@Override
	public void earlyStartup() {
		System.out.println("[AppLifeCycle] earlyStartup called.");
		SimpleDebuggerWorkFlow.Factory.create("localhost", 8000, workflow -> {
			System.out.println("[AppLifeCycle] Workflow ready! Starting debug...");
			try {
				workflow.debug();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
//		org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> {
//            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
//            if (window == null) return;
//
//            IWorkbenchPage page = window.getActivePage();
//            if (page == null) return;
//
//            // Подписка на события редактора
//            page.addPartListener(new IPartListener2() {
//                @Override
//                public void partActivated(IWorkbenchPartReference ref) {
//                    IEditorPart editor = (IEditorPart) ref.getPart(false);
//                    if (editor instanceof ITextEditor) {
//                        // Здесь вызываем highlightLine
//                        highlightLine((ITextEditor) editor, 0); // например, первую строку
//                    }
//                }
//
//                private void highlightLine(ITextEditor editor, int lineNumber) {
//                    if (editor == null) return;
//
//                    IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
//                    IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
//                    if (annotationModel == null || document == null) return;
//
//                    try {
//                        // Получаем смещение и длину строки
//                        int offset = document.getLineOffset(lineNumber);
//                        int length = document.getLineLength(lineNumber);
//
//                        org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> {
//                            // Снимаем предыдущую аннотацию, если есть
//                            if (currentAnnotation != null) {
//                                annotationModel.removeAnnotation(currentAnnotation);
//                                currentAnnotation = null;
//                            }
//
//                            // Создаём новую аннотацию с уникальным ID
//                            currentAnnotation = new Annotation(
//                                "com.gmail.aydinov.sergey.simple_debugger_plugin.annotation.lineHighlight", // ID типа аннотации
//                                false, // не маркер
//                                "Stopped here" // текст для hover
//                            );
//
//                            // Добавляем аннотацию в модель
//                            annotationModel.addAnnotation(currentAnnotation, new Position(offset, length));
//                        });
//
//                    } catch (BadLocationException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//				@Override public void partBroughtToTop(IWorkbenchPartReference ref) {}
//                @Override public void partClosed(IWorkbenchPartReference ref) {}
//                @Override public void partDeactivated(IWorkbenchPartReference ref) {}
//                @Override public void partHidden(IWorkbenchPartReference ref) {}
//                @Override public void partInputChanged(IWorkbenchPartReference ref) {}
//                @Override public void partOpened(IWorkbenchPartReference ref) {}
//                @Override public void partVisible(IWorkbenchPartReference ref) {}
//            });
//        });
	}
}
