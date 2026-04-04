package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;

public class UserObjectPageDTO extends AbstractInspectionDTO {

    private final String classType;
    private List<InnerElementRepresentationDTO> entries;

    private UserObjectPageDTO(Builder builder) {
        super(
            builder.anchorTag,
            builder.elementName,
            builder.elementType,
            builder.breadcrumbs
        );
        this.classType = builder.classType;
        this.entries = builder.entries;
    }

    public String getClassType() { return classType; }
    public List<InnerElementRepresentationDTO> getEntries() { return entries; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String elementName;
        private String elementType;
        private String classType;
        private List<InnerElementRepresentationDTO> entries;
        private Tag anchorTag;
        private List<BreadcrumbItemDTO> breadcrumbs = List.of(); // по умолчанию пустой список

        public Builder elementName(String name) { this.elementName = name; return this; }
        public Builder elementType(String type) { this.elementType = type; return this; }
        public Builder classType(String classType) { this.classType = classType; return this; }
        public Builder entries(List<InnerElementRepresentationDTO> fields) { this.entries = fields; return this; }
        public Builder anchorTag(Tag tag) { this.anchorTag = tag; return this; }
        public Builder breadcrumbs(List<BreadcrumbItemDTO> breadcrumbs) { this.breadcrumbs = breadcrumbs; return this; }

        public UserObjectPageDTO build() {
            return new UserObjectPageDTO(this);
        }
    }
}