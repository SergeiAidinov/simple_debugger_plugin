package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.FieldInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInspectionDTO;

/**
 * Менеджер popup для инспекции одного элемента.
 * Всегда отображается только один popup.
 * При наведении на другой элемент старый закрывается.
 */
public class InstanceInspectionPopupManager {

    private final Composite root;

    private Shell currentPopup;
    private UUID currentElementId;

    public InstanceInspectionPopupManager(Composite root) {
        this.root = root;
    }
    
    

    public Shell getCurrentPopup() {
		return currentPopup;
	}



	public void setCurrentPopup(Shell currentPopup) {
		this.currentPopup = currentPopup;
	}



	/**
     * Показывает popup для inspectable элемента.
     * Если уже открыт popup для другого элемента — закрывает его.
     * @param dto данные инстанса
     * @param elementId уникальный UUID элемента
     * @param location точка для отображения popup
     */
    public void showPopup(UserInstanceInspectionDTO dto, UUID elementId, Point location) {
        Display display = root.getDisplay();

        display.asyncExec(() -> {
            if (root.isDisposed())
                return;

            // если popup уже открыт для этого же элемента — ничего не делаем
            if (currentPopup != null && !currentPopup.isDisposed()
                    && Objects.equals(currentElementId, elementId)) {
                return;
            }

            // если открыт для другого элемента — закрываем
            if (currentPopup != null && !currentPopup.isDisposed()) {
                currentPopup.dispose();
            }

            // если элемент не inspectable (dto == null) — просто закрыли и выходим
            if (dto == null) {
                currentElementId = null;
                return;
            }

            Shell popup = new Shell(root.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
            popup.setLayout(new GridLayout(1, false));

            // формируем текст
            StringBuilder info = new StringBuilder();
            info.append("Instance: ").append(dto.getInstanceName()).append("\n\n");

            if (dto.getInstanceElements() != null) {
                for (FieldInspectionDTO field : dto.getInstanceElements()) {

                    info.append("Field: ").append(field.getFieldName()).append("\n");
                    info.append("Type: ").append(field.getType()).append("\n");

                    if (field.getValue() != null)
                        info.append("Value: ").append(field.getValue()).append("\n");

                    if (field.getMethods() != null && !field.getMethods().isEmpty()) {
                        info.append("Methods:\n");
                        for (String m : field.getMethods())
                            info.append("   ").append(m).append("\n");
                    }

                    info.append("\n");
                }
            }

            Label label = new Label(popup, SWT.WRAP);
            label.setText(info.toString());
            label.setLayoutData(new GridData(350, SWT.DEFAULT));

            popup.pack();

            // позиционируем popup
            if (location != null) {
                popup.setLocation(
                        root.getDisplay().map(root, null, location.x + 10, location.y + 10)
                );
            } else {
                // если location == null, ставим в центр root
                Point size = root.getSize();
                popup.setLocation(root.toDisplay(size.x / 2, size.y / 2));
            }

            popup.open();

            // сохраняем текущее окно и элемент
            currentPopup = popup;
            currentElementId = elementId;

            // при закрытии popup — очищаем ссылки
            popup.addListener(SWT.Dispose, e -> {
                currentPopup = null;
                currentElementId = null;
            });

            // автозакрытие через 3 сек (по желанию)
//            display.timerExec(3000, () -> {
//                if (currentPopup != null && !currentPopup.isDisposed()) {
//                    currentPopup.dispose();
//                }
//            });
        });
    }

    /**
     * Закрывает popup, если он открыт.
     */
    public void closePopup() {
        Display display = root.getDisplay();

        display.asyncExec(() -> {
            if (currentPopup != null && !currentPopup.isDisposed()) {
                currentPopup.dispose();
            }
            currentPopup = null;
            currentElementId = null;
        });
    }
}