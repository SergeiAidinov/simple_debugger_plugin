package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.List;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;

public class MapPageDTO<K, V> {

    private final String mapName;
    private final String mapType;
    private final int totalEntries;
    private final int currentPage;
    private final int totalPages;
    private final int fromIndex;
    private final int toIndex;
    private final List<PairDTO<K, V>> entries;

    private Tag anchorTag; // <- новое поле

    private MapPageDTO(Builder<K, V> builder) {
        this.mapName = builder.mapName;
        this.mapType = builder.mapType;
        this.totalEntries = builder.totalEntries;
        this.currentPage = builder.currentPage;
        this.totalPages = builder.totalPages;
        this.fromIndex = builder.fromIndex;
        this.toIndex = builder.toIndex;
        this.entries = builder.entries;
        this.anchorTag = builder.anchorTag; // присваиваем тег из билдер
    }

    // =================== Геттеры ===================
    public String getMapName() { return mapName; }
    public String getMapType() { return mapType; }
    public int getTotalEntries() { return totalEntries; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
    public int getFromIndex() { return fromIndex; }
    public int getToIndex() { return toIndex; }
    public List<PairDTO<K, V>> getEntries() { return entries; }

    public Tag getAnchorTag() { return anchorTag; }        // <- геттер для тега
    public void setAnchorTag(Tag anchorTag) {             // <- сеттер для тега
        this.anchorTag = anchorTag;
    }

    // =========================================================
    // Builder
    // =========================================================
    public static <K, V> Builder<K, V> builder() { return new Builder<>(); }

    public static class Builder<K, V> {
        private String mapName;
        private String mapType;
        private int totalEntries;
        private int currentPage;
        private int totalPages;
        private int fromIndex;
        private int toIndex;
        private List<PairDTO<K, V>> entries;
        private Tag anchorTag; // <- тег в билдере

        public Builder<K, V> mapName(String mapName) { this.mapName = mapName; return this; }
        public Builder<K, V> mapType(String mapType) { this.mapType = mapType; return this; }
        public Builder<K, V> totalEntries(int totalEntries) { this.totalEntries = totalEntries; return this; }
        public Builder<K, V> currentPage(int currentPage) { this.currentPage = currentPage; return this; }
        public Builder<K, V> totalPages(int totalPages) { this.totalPages = totalPages; return this; }
        public Builder<K, V> fromIndex(int fromIndex) { this.fromIndex = fromIndex; return this; }
        public Builder<K, V> toIndex(int toIndex) { this.toIndex = toIndex; return this; }
        public Builder<K, V> entries(List<PairDTO<K, V>> entries) { this.entries = entries; return this; }

        public Builder<K, V> anchorTag(Tag tag) {    // <- метод для установки тега
            this.anchorTag = tag;
            return this;
        }

        public MapPageDTO<K, V> build() {
            return new MapPageDTO<>(this);
        }
    }

    // =========================================================
    // Удобные методы
    // =========================================================
    public boolean hasPreviousPage() { return currentPage > 0; }
    public boolean hasNextPage() { return currentPage < totalPages - 1; }
}