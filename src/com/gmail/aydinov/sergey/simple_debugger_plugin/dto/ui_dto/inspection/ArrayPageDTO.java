package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public class ArrayPageDTO extends AbstractInspectionCollectionPage {

    private final String collectionName;
    private final String collectionType;
    private final int fromIndex;
    private final int toIndex;

    private Tag anchorTag;

    private ArrayPageDTO(Builder builder) {
        super(builder.anchorTag, builder.collectionName, builder.elementType,
              builder.totalElements, builder.currentPage, builder.totalPages, builder.entries);

        this.collectionName = builder.collectionName;
        this.collectionType = builder.collectionType;
        this.fromIndex = builder.fromIndex;
        this.toIndex = builder.toIndex;
        this.anchorTag = builder.anchorTag;
    }

    // =================== Геттеры ===================
    public String getCollectionName() { return collectionName; }
    public String getCollectionType() { return collectionType; }
    public int getFromIndex() { return fromIndex; }
    public int getToIndex() { return toIndex; }
    public Tag getAnchorTag() { return anchorTag; }
    public void setAnchorTag(Tag anchorTag) { this.anchorTag = anchorTag; }

    public static Builder builder() { return new Builder(); }

    // =========================================================
    // Builder
    // =========================================================
    public static class Builder {

        private String collectionName;
        private String collectionType;
        private String elementType;
        private int totalElements;
        private int currentPage;
        private int totalPages;
        private int fromIndex;
        private int toIndex;
        private List<PairDTO<Integer, InnerElementRepresentationDTO>> entries;
        private Tag anchorTag;

        public Builder collectionName(String value) { this.collectionName = value; return this; }
        public Builder collectionType(String value) { this.collectionType = value; return this; }
        public Builder elementType(String value) { this.elementType = value; return this; }
        public Builder totalElements(int value) { this.totalElements = value; return this; }
        public Builder currentPage(int value) { this.currentPage = value; return this; }
        public Builder totalPages(int value) { this.totalPages = value; return this; }
        public Builder fromIndex(int value) { this.fromIndex = value; return this; }
        public Builder toIndex(int value) { this.toIndex = value; return this; }
        public Builder entries(List<PairDTO<Integer, InnerElementRepresentationDTO>> list) { this.entries = list; return this; }
        public Builder anchorTag(Tag tag) { this.anchorTag = tag; return this; }

        public ArrayPageDTO build() {
            return new ArrayPageDTO(this);
        }
    }
}