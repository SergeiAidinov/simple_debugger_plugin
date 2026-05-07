package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.NavigationHistoryStep;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.BreadCrumbDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public class UserObjectPageDTO extends AbstractInspectionDTO {

    private final String classType;
    private List<InnerElementRepresentationDTO> entries;

    private UserObjectPageDTO(Builder builder) {
        super(
            builder.anchorTag,
            builder.elementName,
            builder.elementType,
            builder.breadcrumbs,
            builder.objectId
        );
        this.classType = builder.classType;
        this.entries = builder.entries;
    }

    public String getClassType() { return classType; }
    public List<InnerElementRepresentationDTO> getEntries() { return entries; }
    

    @Override
	public String toString() {
		return "UserObjectPageDTO [classType=" + classType + ", entries=" + entries + "]";
	}
    
    

	@Override
	public int hashCode() {
		return Objects.hash(classType, entries);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserObjectPageDTO other = (UserObjectPageDTO) obj;
		return Objects.equals(classType, other.classType) && Objects.equals(entries, other.entries);
	}

	public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long objectId;
		private String elementName;
        private String elementType;
        private String classType;
        private List<InnerElementRepresentationDTO> entries;
        private Tag anchorTag;
        private List<PairDTO<Integer, BreadCrumbDTO>> breadcrumbs = List.of(); // по умолчанию пустой список

        public Builder elementName(String name) { this.elementName = name; return this; }
        public Builder elementType(String type) { this.elementType = type; return this; }
        public Builder classType(String classType) { this.classType = classType; return this; }
        public Builder entries(List<InnerElementRepresentationDTO> fields) { this.entries = fields; return this; }
        public Builder anchorTag(Tag tag) { this.anchorTag = tag; return this; }
        public Builder breadcrumbs(List<PairDTO<Integer, BreadCrumbDTO>> breadcrumbs) { this.breadcrumbs = breadcrumbs; return this; }
        public Builder objectId(Long objectId) {this.objectId = objectId; return this;}

        public UserObjectPageDTO build() {
            return new UserObjectPageDTO(this);
        }
    }
}