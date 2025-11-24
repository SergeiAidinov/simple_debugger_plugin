package com.gmail.aydinov.sergey.simple_debugger_plugin.core;


import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.ide.IDE;

public class AutoBreakpointHighlighter {

    private Annotation currentAnnotation;
    private int currentLine = -1;

    public void highlightBreakpoint(Location location) {
        if (location == null) return;

        // JDI lineNumber начинается с 1, Eclipse с 0
        int lineNumber = location.lineNumber() - 1;
        AtomicReference<String> className = new AtomicReference<String>();
		try {
			className.set(location.sourceName());
		} catch (AbsentInformationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // пример: "target_debug.TargetImpl"

        Display.getDefault().asyncExec(() -> {
            try {
                IFile file = findIFileForLocation(location);
               IPath qq = file.getFullPath();
               System.out.println("IPath: " + qq);
                File ww = qq.toFile();
                
                if (file == null) {
                    System.out.println("Не удалось найти IFile для класса: " + className);
                    return;
                }

                IWorkbenchPage page = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow()
                        .getActivePage();
                if (page == null) return;
                URI uri = URI.create(qq.toString());
                
                file = getIFileFromFile(new File(uri));
                IEditorPart editorPart = IDE.openEditor(page, file, true);
                if (!(editorPart instanceof ITextEditor)) return;

                ITextEditor editor = (ITextEditor) editorPart;
                IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
                IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
                if (document == null || annotationModel == null) return;

                int offset = document.getLineOffset(lineNumber);
                int length = document.getLineLength(lineNumber);

                // удалить старую аннотацию
                if (currentAnnotation != null) {
                    annotationModel.removeAnnotation(currentAnnotation);
                }

                // создать новую аннотацию
                currentAnnotation = new Annotation(
                        "com.gmail.aydinov.sergey.simple_debugger_plugin.annotation.lineHighlight",
                        false,
                        "Stopped here"
                );
                annotationModel.addAnnotation(currentAnnotation, new Position(offset, length));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    public IFile getIFileFromFile(File file) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        String absolutePath = file.getAbsolutePath();

        // Получаем проект и путь внутри workspace
        for (IFile workspaceFile : root.findFilesForLocationURI(file.toURI())) {
            if (workspaceFile.exists()) {
                return workspaceFile; // нашли соответствующий IFile
            }
        }

        // Если не нашли — можно пробовать создать путь вручную
        // Например, из workspace-relative path
//        IPath workspaceRelativePath = new Path("/target_debug/src/target_debug/TargetImpl.java");
//        IFile iFile = root.getFile(workspaceRelativePath);
//        if (iFile.exists()) {
//            return iFile;
//        }
        IPath iPath = IPath.fromFile(file);
        IFile iFile = root.getFile(iPath);
        return iFile; // не нашли
    }
    
    private IFile findIFileForLocation(Location location) {
        try {
            String sourcePath = location.sourcePath(); 
            // например "target_debug/TargetImpl.java"

            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject project = root.getProject("target_debug");

            if (!project.exists()) {
                System.out.println("Проект target_debug не найден");
                return null;
            }

            // Путь к файлу в проекте
            String fullPath = "src/" + sourcePath;

            IFile file = project.getFile(fullPath);

            if (!file.exists()) {
                System.out.println("Файл не найден: " + fullPath);
                return null;
            }

            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
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



