package com.blogcms.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> content;      // the items on this page
    private int page;             // current page number (0-based)
    private int size;             // items per page
    private long totalElements;   // total items across all pages
    private int totalPages;       // total number of pages
    private boolean last;         // true if this is the final page
}