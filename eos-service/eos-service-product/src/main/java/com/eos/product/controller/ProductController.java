package com.eos.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.eos.common.constant.CommonConstant;
import com.eos.common.exception.BizException;
import com.eos.common.result.PageResult;
import com.eos.common.result.Result;
import com.eos.common.result.ResultCode;
import com.eos.product.dto.ProductCreateDTO;
import com.eos.product.dto.ProductQueryDTO;
import com.eos.product.dto.ProductUpdateDTO;
import com.eos.product.dto.StockDeductDTO;
import com.eos.product.service.ProductService;
import com.eos.product.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 *
 * <p>公开接口：商品浏览</p>
 * <p>管理员接口：商品增删改、上下架、分页查询</p>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    // ==================== 公开接口 ====================

    /**
     * 查询商品详情（公开）
     */
    @GetMapping("/{productId}")
    public Result<ProductVO> getProductById(@PathVariable Long productId) {
        return Result.ok(productService.getProductById(productId));
    }

    /**
     * 查询商品列表（公开，仅上架商品）
     */
    @GetMapping("/list")
    public Result<List<ProductVO>> listProducts() {
        return Result.ok(productService.listProducts());
    }

    /**
     * 查询库存（公开）
     */
    @GetMapping("/{productId}/stock")
    public Result<Integer> getStock(@PathVariable Long productId) {
        return Result.ok(productService.getStock(productId));
    }

    // ==================== 管理员接口 ====================

    /**
     * 分页查询商品（管理员）
     */
    @GetMapping("/admin/page")
    public Result<PageResult<ProductVO>> pageProducts(
            @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        checkAdmin(role);
        ProductQueryDTO query = new ProductQueryDTO();
        query.setKeyword(keyword);
        query.setCategoryId(categoryId);
        query.setStatus(status);
        IPage<ProductVO> page = productService.pageProducts(query, pageNo, pageSize);
        return Result.ok(PageResult.of(page, page.getRecords()));
    }

    /**
     * 创建商品（管理员）
     */
    @PostMapping("/admin")
    public Result<ProductVO> createProduct(
            @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role,
            @RequestBody @Validated ProductCreateDTO dto) {
        checkAdmin(role);
        return Result.ok(productService.createProduct(dto));
    }

    /**
     * 更新商品（管理员）
     */
    @PutMapping("/admin/{productId}")
    public Result<ProductVO> updateProduct(
            @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role,
            @PathVariable Long productId,
            @RequestBody @Validated ProductUpdateDTO dto) {
        checkAdmin(role);
        return Result.ok(productService.updateProduct(productId, dto));
    }

    /**
     * 删除商品（管理员）
     */
    @DeleteMapping("/admin/{productId}")
    public Result<Void> deleteProduct(
            @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role,
            @PathVariable Long productId) {
        checkAdmin(role);
        productService.deleteProduct(productId);
        return Result.ok();
    }

    /**
     * 修改商品状态（管理员）
     */
    @PutMapping("/admin/{productId}/status")
    public Result<Void> updateStatus(
            @RequestHeader(value = CommonConstant.HEADER_ROLE, required = false) String role,
            @PathVariable Long productId,
            @RequestParam Integer status) {
        checkAdmin(role);
        if (status != 0 && status != 1) {
            throw new BizException(ResultCode.BAD_REQUEST, "状态只能为0或1");
        }
        productService.updateStatus(productId, status);
        return Result.ok();
    }

    /**
     * 扣减库存（内部调用）
     */
    @PostMapping("/deductStock")
    public Result<Boolean> deductStock(@RequestBody @Validated StockDeductDTO dto) {
        boolean success = productService.deductStock(dto);
        return Result.ok(success);
    }

    /**
     * 回滚库存（内部调用）
     */
    @PostMapping("/rollbackStock")
    public Result<Boolean> rollbackStock(@RequestBody @Validated StockDeductDTO dto) {
        boolean success = productService.rollbackStock(dto);
        return Result.ok(success);
    }

    /**
     * 管理员权限校验
     */
    private void checkAdmin(String role) {
        if (!CommonConstant.ROLE_ADMIN.equals(role)) {
            throw new BizException(ResultCode.PERMISSION_DENIED);
        }
    }
}
