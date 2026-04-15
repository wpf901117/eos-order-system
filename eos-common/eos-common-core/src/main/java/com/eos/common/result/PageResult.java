package com.eos.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页响应封装
 *
 * <p>分页查询是后端开发最高频的场景之一，统一分页结构可以：</p>
 * <ul>
 *   <li>减少前端分页逻辑的差异</li>
 *   <li>统一分页参数命名（pageSize/pageNo vs size/page）</li>
 *   <li>提供标准的总数、总页数、当前页数据</li>
 * </ul>
 *
 * @param <T> 列表数据类型
 * @author EOS Team
 * @since 1.0.0
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码，从1开始 */
    private Long pageNo;

    /** 每页大小 */
    private Long pageSize;

    /** 总记录数 */
    private Long total;

    /** 总页数 */
    private Long totalPages;

    /** 当前页数据列表 */
    private List<T> list;

    /**
     * 私有构造方法
     */
    private PageResult() {
    }

    /**
     * 基于MyBatis Plus的IPage构造分页结果
     *
     * @param page MyBatis Plus分页对象
     * @param <T>  数据类型
     * @return 分页响应对象
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setPageNo(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setTotalPages(page.getPages());
        result.setList(page.getRecords());
        return result;
    }

    /**
     * 基于MyBatis Plus的IPage和数据列表构造分页结果（用于类型转换场景）
     *
     * @param page   MyBatis Plus分页对象
     * @param records 转换后的数据列表
     * @param <T>   数据类型
     * @return 分页响应对象
     */
    public static <T> PageResult<T> of(IPage<?> page, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setPageNo(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setTotalPages(page.getPages());
        result.setList(records);
        return result;
    }

    /**
     * 手动构造分页结果
     *
     * @param pageNo    页码
     * @param pageSize  页大小
     * @param total     总数
     * @param totalPages 总页数
     * @param list      数据列表
     * @param <T>       数据类型
     * @return 分页响应对象
     */
    public static <T> PageResult<T> of(long pageNo, long pageSize, long total, long totalPages, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setTotalPages(totalPages);
        result.setList(list);
        return result;
    }

    /**
     * 返回空分页结果
     *
     * @param pageNo   页码
     * @param pageSize 页大小
     * @param <T>      数据类型
     * @return 空分页响应对象
     */
    public static <T> PageResult<T> empty(long pageNo, long pageSize) {
        return of(pageNo, pageSize, 0L, 0L, Collections.emptyList());
    }
}
