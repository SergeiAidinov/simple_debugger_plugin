package com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.holder.DataProviderHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.SimpleDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebuggerEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;

public class SimpleDataProviderHolderImpl implements DataProviderHolder {

    private final SimpleDataProvider simpleDataProvider;
    private final int inspectionSeanceId;

    public SimpleDataProviderHolderImpl(
            SimpleDataProvider simpleDataProvider,
            int inspectionSeanceId) {

        this.simpleDataProvider = simpleDataProvider;
        this.inspectionSeanceId = inspectionSeanceId;
    }

    public long getDataProviderHolderId() {
        return inspectionSeanceId;
    }

    public void handleElementRequest(
            InnerElementRepresentationDTO innerElementRepresentationDTO) {

        simpleDataProvider.handleElementRequest(innerElementRepresentationDTO);
    }

    @Override
    public void handleEvent(
            AbstractSimpleDebuggerEvent abstractSimpleDebuggerEvent) {

        AbstractUIEvent event =
                (AbstractUIEvent) abstractSimpleDebuggerEvent;

        UIEvent<InnerElementRepresentationDTO> uiEvent =
                (UIEvent<InnerElementRepresentationDTO>) event;

        simpleDataProvider.handleElementRequest(uiEvent.getPayload());
    }

    @Override
    public void startDataProvider() {
    }

    @Override
    public void terminateCurrentRequest() {
    }

    @Override
    public DataProvider getDataProvider() {
        return simpleDataProvider;       // ВОТ ЭТО
    }
}
