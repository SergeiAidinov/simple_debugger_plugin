package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

public class TripletDTO<A, B, C> {

    private final A first;
    private final B second;
    private final C third;

    private TripletDTO(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    public C getThird() {
        return third;
    }

    public static <A, B, C> TripletDTO<A, B, C> of(A first, B second, C third) {
        return new TripletDTO<>(first, second, third);
    }
}