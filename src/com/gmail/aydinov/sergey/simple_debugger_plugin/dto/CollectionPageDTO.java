package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public class CollectionPageDTO {

    private final String collectionName;
    private final String collectionType;
    private final String elementType;
    private final int totalElements;
    private final int currentPage;
    private final int totalPages;
    private final int fromIndex;
    private final int toIndex;

    private final List<PairDTO<Integer, InnerElementRepresentationDTO>> entries;

    private CollectionPageDTO(Builder builder) {
        this.collectionName = builder.collectionName;
        this.collectionType = builder.collectionType;
        this.elementType = builder.elementType;
        this.totalElements = builder.totalElements;
        this.currentPage = builder.currentPage;
        this.totalPages = builder.totalPages;
        this.fromIndex = builder.fromIndex;
        this.toIndex = builder.toIndex;
        this.entries = builder.entries;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getCollectionType() {
        return collectionType;
    }

    public String getElementType() {
        return elementType;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getToIndex() {
        return toIndex;
    }

    public List<PairDTO<Integer, InnerElementRepresentationDTO>> getEntries() {
        return entries;
    }


    public boolean hasNextPage() {
        return currentPage < totalPages;
    }

    public boolean hasPreviousPage() {
        return currentPage > 0;
    }
    
    public static Builder builder() {
        return new Builder();
    }

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

        public Builder collectionName(String value) {
            this.collectionName = value;
            return this;
        }

        public Builder collectionType(String value) {
            this.collectionType = value;
            return this;
        }

        public Builder elementType(String value) {
            this.elementType = value;
            return this;
        }

        public Builder totalElements(int value) {
            this.totalElements = value;
            return this;
        }

        public Builder currentPage(int value) {
            this.currentPage = value;
            return this;
        }

        public Builder totalPages(int value) {
            this.totalPages = value;
            return this;
        }

        public Builder fromIndex(int value) {
            this.fromIndex = value;
            return this;
        }

        public Builder toIndex(int value) {
            this.toIndex = value;
            return this;
        }

        public Builder entries(List<PairDTO<Integer, InnerElementRepresentationDTO>> list) {
            this.entries = list;
            return this;
        }

        public CollectionPageDTO build() {
            return new CollectionPageDTO(this);
        }
    }
}