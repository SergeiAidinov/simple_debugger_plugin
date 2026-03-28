package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Collection;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;

public class PairDTO<A, B> {
    private final A first;
    private final B second;

    private PairDTO(A first, B second) {
		super();
		this.first = first;
		this.second = second;
	}

	public A getFirst() {
		return first;
	}

	public B getSecond() {
		return second;
	}

	public static <A,B> PairDTO<A,B> of(A first, B second) {
        return new PairDTO<>(first, second);
    }
}
