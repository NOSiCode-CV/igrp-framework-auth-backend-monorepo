package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Client-side mirror of the Spring Data {@code Page} JSON envelope returned by
 * the paginated Access Management endpoints (audit log + the three reports).
 *
 * <p>Only the stable, documented envelope fields are mapped; the rich
 * {@code pageable} / {@code sort} sub-objects Spring also emits are ignored
 * ({@link JsonIgnoreProperties}). Deserialize with a {@code TypeReference} so
 * the element type {@code T} is preserved, e.g.
 * {@code new TypeReference<PageResponse<AuditReportRowDTO>>(){}}.
 *
 * @param <T> the row type carried in {@link #getContent()}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponse<T> {

    @JsonProperty("content")
    private List<T> content;

    @JsonProperty("totalElements")
    private long totalElements;

    @JsonProperty("totalPages")
    private int totalPages;

    @JsonProperty("size")
    private int size;

    @JsonProperty("number")
    private int number;

    @JsonProperty("numberOfElements")
    private int numberOfElements;

    @JsonProperty("first")
    private boolean first;

    @JsonProperty("last")
    private boolean last;

    @JsonProperty("empty")
    private boolean empty;

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public int getNumberOfElements() { return numberOfElements; }
    public void setNumberOfElements(int numberOfElements) { this.numberOfElements = numberOfElements; }

    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }

    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
}
