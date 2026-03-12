package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.CollectionEntryDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.CollectionInspectorTab;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;

public class CollectionInspectorWindow {

    private final Shell shell;
    private final CollectionInspectorTab inspectorTab;

    private final Button backButton;
    private final Button forwardButton;
    private final Text pageText;
    private final Label totalPagesLabel;

    private int currentPage = 1;
    private int totalPages = 1;
    private List<CollectionEntryDTO> allElements;

    public CollectionInspectorWindow(Display display, List<CollectionEntryDTO> elements) {
        this.allElements = elements;

        shell = new Shell(display, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("Collection Inspector");
        shell.setSize(600, 768);
        shell.setImage(DebugWindowsManager.instance().icons.get("debugger").getFirst());
        shell.setLayout(new GridLayout(1, false));

        // Верхняя панель навигации
        Composite topPanel = new Composite(shell, SWT.NONE);
        topPanel.setLayout(new GridLayout(4, false));
        topPanel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        backButton = new Button(topPanel, SWT.PUSH);
        backButton.setText("◀ Back");
        backButton.setEnabled(false);
        backButton.addListener(SWT.Selection, e -> goBack());

        pageText = new Text(topPanel, SWT.BORDER | SWT.CENTER);
        pageText.setTextLimit(5); // ограничим длину ввода
        pageText.setLayoutData(new GridData(50, SWT.DEFAULT));
        pageText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.character == SWT.CR) { // Enter
                    jumpToPageFromText();
                }
            }
        });
        pageText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                jumpToPageFromText();
            }
        });

        totalPagesLabel = new Label(topPanel, SWT.NONE);
        totalPagesLabel.setText("/ 1");

        forwardButton = new Button(topPanel, SWT.PUSH);
        forwardButton.setText("Forward ▶");
        forwardButton.setEnabled(false);
        forwardButton.addListener(SWT.Selection, e -> goForward());

        // Вкладка с коллекцией
        inspectorTab = new CollectionInspectorTab(shell);
        inspectorTab.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Инициализация страницы
        totalPages = (int) Math.ceil((double) allElements.size() / DebugUtils.PAGE_SIZE);
        updatePage();
        
        shell.open();
    }

    /** Навигация вперед */
    private void goForward() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePage();
        }
    }

    /** Навигация назад */
    private void goBack() {
        if (currentPage > 1) {
            currentPage--;
            updatePage();
        }
    }

    /** Ввод страницы вручную */
    private void jumpToPageFromText() {
        try {
            int page = Integer.parseInt(pageText.getText().trim());
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;
            currentPage = page;
            updatePage();
        } catch (NumberFormatException ignored) {
            pageText.setText(String.valueOf(currentPage));
        }
    }

    /** Обновление текущей страницы */
    private void updatePage() {
        int start = (currentPage - 1) * DebugUtils.PAGE_SIZE;
        int end = Math.min(start + DebugUtils.PAGE_SIZE, allElements.size());
        List<CollectionEntryDTO> pageElements = allElements.subList(start, end);
        inspectorTab.showCollection(pageElements);

        pageText.setText(String.valueOf(currentPage));
        totalPagesLabel.setText("/ " + totalPages);
        backButton.setEnabled(currentPage > 1);
        forwardButton.setEnabled(currentPage < totalPages);
    }

    public void open() {
        shell.open();
    }

    public boolean isOpen() {
        return !shell.isDisposed();
    }

    public void close() {
        if (!shell.isDisposed()) shell.close();
    }
}