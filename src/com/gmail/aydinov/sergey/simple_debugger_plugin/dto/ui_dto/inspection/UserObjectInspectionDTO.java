package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.NavigationHistoryStep;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.BreadCrumbDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public class UserObjectInspectionDTO extends AbstractInspectionDTO {

    private final String className;
    private final String stringValue;

    private final List<InnerElementRepresentationDTO> instanceFields;
    private final List<InnerElementRepresentationDTO> staticFields;

    private final List<InnerElementRepresentationDTO> instanceMethods;
    private final List<InnerElementRepresentationDTO> staticMethods;

    public UserObjectInspectionDTO(Tag tag,
                                   String elementName,
                                   String elementType, 
                                   List<PairDTO<Integer, BreadCrumbDTO>> breadcrumbs,
                                   String className,
                                   String stringValue,
                                   List<InnerElementRepresentationDTO> instanceFields,
                                   List<InnerElementRepresentationDTO> staticFields,
                                   List<InnerElementRepresentationDTO> instanceMethods,
                                   List<InnerElementRepresentationDTO> staticMethods,
                                   Long objectId) {
        super(tag, elementName, elementType, breadcrumbs, objectId);
        this.className = className;
        this.stringValue = stringValue;
        this.instanceFields = List.copyOf(instanceFields);
        this.staticFields = List.copyOf(staticFields);
        this.instanceMethods = List.copyOf(instanceMethods);
        this.staticMethods = List.copyOf(staticMethods);
    }

    public String getClassName() { return className; }
    public String getStringValue() { return stringValue; }
    public List<InnerElementRepresentationDTO> getInstanceFields() { return instanceFields; }
    public List<InnerElementRepresentationDTO> getStaticFields() { return staticFields; }
    public List<InnerElementRepresentationDTO> getInstanceMethods() { return instanceMethods; }
    public List<InnerElementRepresentationDTO> getStaticMethods() { return staticMethods; }

    // =================== Builder ===================
    public static class Builder {

        private Tag tag;
        private String elementName;
        private String elementType;
        private List<PairDTO<Integer, BreadCrumbDTO>> breadcrumbs = List.of(); // по умолчанию пустой
        private String className;
        private String stringValue;
        private List<InnerElementRepresentationDTO> instanceFields = List.of();
        private List<InnerElementRepresentationDTO> staticFields = List.of();
        private List<InnerElementRepresentationDTO> instanceMethods = List.of();
        private List<InnerElementRepresentationDTO> staticMethods = List.of();
        private Long objectId;

        public Builder tag(Tag tag) { this.tag = tag; return this; }
        public Builder elementName(String elementName) { this.elementName = elementName; return this; }
        public Builder elementType(String elementType) { this.elementType = elementType; return this; }
        public Builder breadcrumbs(List<PairDTO<Integer, BreadCrumbDTO>> breadcrumbs) { this.breadcrumbs = breadcrumbs; return this; }
        public Builder className(String className) { this.className = className; return this; }
        public Builder stringValue(String stringValue) { this.stringValue = stringValue; return this; }
        public Builder instanceFields(List<InnerElementRepresentationDTO> instanceFields) { this.instanceFields = instanceFields; return this; }
        public Builder staticFields(List<InnerElementRepresentationDTO> staticFields) { this.staticFields = staticFields; return this; }
        public Builder instanceMethods(List<InnerElementRepresentationDTO> instanceMethods) { this.instanceMethods = instanceMethods; return this; }
        public Builder staticMethods(List<InnerElementRepresentationDTO> staticMethods) { this.staticMethods = staticMethods; return this; }

        public UserObjectInspectionDTO build() {
            return new UserObjectInspectionDTO(
                tag, elementName, elementType, breadcrumbs,
                className, stringValue, instanceFields, staticFields,
                instanceMethods, staticMethods, objectId
            );
        }
    }

    public static Builder builder() { return new Builder(); }
}