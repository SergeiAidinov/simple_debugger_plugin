package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;

public class ArrayPageDTO extends AbstractInspectionCollectionPage<PairDTO<Integer, InnerElementRepresentationDTO>> {

    private final int fromIndex;
    private final int toIndex;

    private ArrayPageDTO(Builder builder) {
        super(
            builder.anchorTag,
            builder.elementName,       // единое поле
            builder.elementType,       // единое поле
            builder.totalElements,
            builder.currentPage,
            builder.totalPages,
            builder.entries            // тип соответствует дженерику T
        );
        this.fromIndex = builder.fromIndex;
        this.toIndex = builder.toIndex;
    }

    // ================= Геттеры =================
    public int getFromIndex() { return fromIndex; }
    public int getToIndex() { return toIndex; }

    public static Builder builder() { return new Builder(); }

    // ================= Builder =================
    public static class Builder {
        private String elementName;
        private String elementType;
        private int totalElements;
        private int currentPage;
        private int totalPages;
        private int fromIndex;
        private int toIndex;
        private List<PairDTO<Integer, InnerElementRepresentationDTO>> entries;
        private Tag anchorTag;

        public Builder elementName(String name) { this.elementName = name; return this; }
        public Builder elementType(String type) { this.elementType = type; return this; }
        public Builder totalElements(int total) { this.totalElements = total; return this; }
        public Builder currentPage(int page) { this.currentPage = page; return this; }
        public Builder totalPages(int pages) { this.totalPages = pages; return this; }
        public Builder fromIndex(int from) { this.fromIndex = from; return this; }
        public Builder toIndex(int to) { this.toIndex = to; return this; }
        public Builder entries(List<PairDTO<Integer, InnerElementRepresentationDTO>> list) { this.entries = list; return this; }
        public Builder anchorTag(Tag tag) { this.anchorTag = tag; return this; }

        public ArrayPageDTO build() {
            return new ArrayPageDTO(this);
        }
    }
}