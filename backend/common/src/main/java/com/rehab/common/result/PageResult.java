package com.rehab.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应对象。
 *
 * <p>前端约定结构 {@code {records, page, size, total}}，page 从 1 开始。</p>
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> records;

    /** 当前页码（从 1 开始） */
    private Long page;

    /** 每页条数 */
    private Long size;

    /** 总记录数 */
    private Long total;

    public PageResult() {
    }

    public PageResult(List<T> records, Long page, Long size, Long total) {
        this.records = records;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    /**
     * 由 MyBatis-Plus 分页对象组装。
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 由 MyBatis-Plus 分页对象 + 转换后的 VO 列表组装。
     */
    public static <E, T> PageResult<T> of(IPage<E> page, List<T> voList) {
        return new PageResult<>(voList, page.getCurrent(), page.getSize(), page.getTotal());
    }

    public static <T> PageResult<T> empty(Long page, Long size) {
        return new PageResult<>(Collections.emptyList(), page, size, 0L);
    }
}
