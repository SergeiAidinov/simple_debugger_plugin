package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import java.util.List;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;

public class MapPageDTO<K, V> extends AbstractInspectionCollectionPage<PairDTO<K, V>> {

    private final int fromIndex;
    private final int toIndex;

    private MapPageDTO(Builder<K, V> builder) {
        super(
            builder.anchorTag,
            builder.elementName,  // единое поле
            builder.elementType,  // единое поле, например "Map<KeyType,ValueType>"
            builder.totalEntries,
            builder.currentPage,
            builder.totalPages,
            builder.entries
        );
        this.fromIndex = builder.fromIndex;
        this.toIndex = builder.toIndex;
    }

    public int getFromIndex() { return fromIndex; }
    public int getToIndex() { return toIndex; }

    public static <K,V> Builder<K,V> builder() { return new Builder<>(); }

    public static class Builder<K,V> {
        private String elementName;
        private String elementType;
        private int totalEntries;
        private int currentPage;
        private int totalPages;
        private int fromIndex;
        private int toIndex;
        private List<PairDTO<K,V>> entries;
        private Tag anchorTag;

        public Builder<K,V> elementName(String name) { this.elementName = name; return this; }
        public Builder<K,V> elementType(String type) { this.elementType = type; return this; }
        public Builder<K,V> totalEntries(int total) { this.totalEntries = total; return this; }
        public Builder<K,V> currentPage(int page) { this.currentPage = page; return this; }
        public Builder<K,V> totalPages(int pages) { this.totalPages = pages; return this; }
        public Builder<K,V> fromIndex(int from) { this.fromIndex = from; return this; }
        public Builder<K,V> toIndex(int to) { this.toIndex = to; return this; }
        public Builder<K,V> entries(List<PairDTO<K,V>> list) { this.entries = list; return this; }
        public Builder<K,V> anchorTag(Tag tag) { this.anchorTag = tag; return this; }

        public MapPageDTO<K,V> build() {
            return new MapPageDTO<>(this);
        }
    }
}