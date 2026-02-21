package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

public class PairDTO<K, V> {
    private final K key;
    private final V value;

    public PairDTO(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }

    public static <K,V> PairDTO<K,V> of(K key, V value) {
        return new PairDTO<>(key, value);
    }
}
