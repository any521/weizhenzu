package com.weizhenzu.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总数
     */
    private Long total;

    /**
     * 当前页
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总页数
     */
    private Long pages;

    public PageResult() {
        this.records = Collections.emptyList();
        this.total = 0L;
        this.current = 1L;
        this.size = 10L;
        this.pages = 0L;
    }

    public PageResult(List<T> records, Long total, Long current, Long size) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total == null ? 0L : total;
        this.current = current == null ? 1L : current;
        this.size = size == null ? 10L : size;
        this.pages = this.size == 0 ? 0L : (this.total + this.size - 1) / this.size;
    }

    public static <T> PageResult<T> of(List<T> records, Long total, Long current, Long size) {
        return new PageResult<>(records, total, current, size);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>();
    }
}
