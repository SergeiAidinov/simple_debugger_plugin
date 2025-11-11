package com.example.app.lifecycle.parts;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import jakarta.annotation.PostConstruct;

public class SampleView {

    private Composite parentComposite; // Предполагаемый контейнер
    private Label myLabelInView; 

    @PostConstruct
    public void createControls(Composite parent) {
        // Инициализируем контейнер
        this.parentComposite = parent;
        
        // Создаем myLabelInView
        this.myLabelInView = new Label(parent, SWT.NONE);
        this.myLabelInView.setText("Hello, e4 View!");
    }

    @Focus
    public void setFocus() {
        // Устанавливаем фокус на родительский контейнер или существующий элемент.
        if (parentComposite != null && !parentComposite.isDisposed()) {
            parentComposite.setFocus(); 
        } 
        // Если вам нужен фокус именно на Label, используйте Вариант 1.
    }

}
