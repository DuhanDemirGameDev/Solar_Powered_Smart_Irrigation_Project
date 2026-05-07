package com.example.smartirrigationmobile.model;

import java.util.Collections;
import java.util.List;

public class PageResponse<T> {

    private List<T> content;
    private int number;
    private int size;
    private int totalPages;
    private long totalElements;
    private boolean last;
    private boolean first;
    private boolean empty;

    public PageResponse() {
    }

    public List<T> getContent() {
        return content == null ? Collections.emptyList() : content;
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public boolean isLast() {
        return last;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isEmpty() {
        return empty;
    }
}
