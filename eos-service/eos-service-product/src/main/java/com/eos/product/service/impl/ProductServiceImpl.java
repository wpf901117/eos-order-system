package com.eos.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eos.common.constant.CommonConstant;
import com.eos.common.exception.BizException;
import com.eos.common.result.ResultCode;
import com.eos.common.util.SnowflakeUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import com.eos.product.dto.ProductCreateDTO;
import com.eos.product.dto.ProductQueryDTO;
import com.eos.product.dto.ProductUpdateDTO;
import com.eos.product.dto.StockDeductDTO;
import com.eos.product.entity.Product;
import com.eos.product.mapper.ProductMapper;
import com.eos.product.service.ProductService;
import com.eos.product.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品服务实现类
 *
 * <p>展示多个高级技术点：</p>
 * <ul>
 *   <li><strong>缓存</strong>：@Cacheable + Redis 缓存热点商品数据</li>
 *   <li><strong>缓存一致性</strong>：@CacheEvict 在库存变更时清除缓存</li>
 *   <li><strong>乐观锁</strong>：MySQL WHERE 条件防止超卖</li>
 *   <li><strong>分布式锁</strong>：Redisson 保护高并发库存扣减</li>
 *   <li><strong>分页</strong>：MyBatis Plus 分页插件</li>
 *   <li><strong>条件查询</strong>：LambdaQueryWrapper 动态构建查询条件</li>
 * </ul>
 *
 * @author EOS Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    @Cacheable(value = "product", key = "#productId")
    public ProductVO getProductById(Long productId) {
        log.info("[商品查询] 缓存未命中，从数据库查询 productId={}", productId);
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return convertToVO(product);
    }

    @Override
    public List<ProductVO> listProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        wrapper.eq(Product::getDeleted, 0);
        wrapper.orderByDesc(Product::getSales, Product::getCreateTime);
        List<Product> products = productMapper.selectList(wrapper);
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<ProductVO> pageProducts(ProductQueryDTO query, long pageNo, long pageSize) {
        Page<Product> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getDeleted, 0);
        if (query.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Product::getStatus, query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
            wrapper.like(Product::getName, query.getKeyword());
        }
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = productMapper.selectPage(page, wrapper);
        Page<ProductVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public ProductVO createProduct(ProductCreateDTO dto) {
        Product product = new Product();
        product.setId(SnowflakeUtil.getInstance().nextId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setImageUrl(dto.getImageUrl());
        product.setCategoryId(dto.getCategoryId());
        product.setStatus(1);
        product.setSales(0);
        productMapper.insert(product);

        // 初始化Redis库存缓存
        String stockKey = CommonConstant.CACHE_STOCK + product.getId();
        redisTemplate.opsForValue().set(stockKey, product.getStock(), 10, TimeUnit.MINUTES);

        log.info("[商品创建] 商品创建成功，productId={}，name={}", product.getId(), product.getName());
        return convertToVO(product);
    }

    @Override
    @CacheEvict(value = "product", key = "#productId")
    public ProductVO updateProduct(Long productId, ProductUpdateDTO dto) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setImageUrl(dto.getImageUrl());
        product.setCategoryId(dto.getCategoryId());
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }
        productMapper.updateById(product);

        // 同步更新Redis库存缓存
        String stockKey = CommonConstant.CACHE_STOCK + productId;
        redisTemplate.opsForValue().set(stockKey, product.getStock(), 10, TimeUnit.MINUTES);

        log.info("[商品更新] 商品更新成功，productId={}", productId);
        return convertToVO(product);
    }

    @Override
    @CacheEvict(value = "product", key = "#productId")
    public void deleteProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        productMapper.deleteById(productId);

        // 清除库存缓存
        String stockKey = CommonConstant.CACHE_STOCK + productId;
        redisTemplate.delete(stockKey);

        log.info("[商品删除] 商品已删除，productId={}", productId);
    }

    @Override
    @CacheEvict(value = "product", key = "#productId")
    public void updateStatus(Long productId, Integer status) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        product.setStatus(status);
        productMapper.updateById(product);
        log.info("[商品状态更新] productId={}，status={}", productId, status);
    }

    @Override
    @CacheEvict(value = "product", key = "#dto.productId")
    public boolean deductStock(StockDeductDTO dto) {
        String lockKey = CommonConstant.LOCK_PREFIX + "stock:" + dto.getProductId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("[库存扣减] 获取分布式锁失败，productId={}", dto.getProductId());
                throw new BizException(ResultCode.DISTRIBUTED_LOCK_ERROR);
            }

            Product product = productMapper.selectById(dto.getProductId());
            if (product == null || product.getStatus() != 1) {
                throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
            }
            if (product.getStock() < dto.getQuantity()) {
                log.warn("[库存扣减] 库存不足，productId={}，当前库存={}，需求={}",
                        dto.getProductId(), product.getStock(), dto.getQuantity());
                return false;
            }

            int affected = productMapper.deductStock(dto.getProductId(), dto.getQuantity());
            if (affected == 0) {
                log.warn("[库存扣减] 乐观锁扣减失败，productId={}", dto.getProductId());
                return false;
            }

            String stockKey = CommonConstant.CACHE_STOCK + dto.getProductId();
            redisTemplate.opsForValue().decrement(stockKey, dto.getQuantity());

            log.info("[库存扣减] 成功，productId={}，扣减数量={}", dto.getProductId(), dto.getQuantity());
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.DISTRIBUTED_LOCK_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @CacheEvict(value = "product", key = "#dto.productId")
    public boolean rollbackStock(StockDeductDTO dto) {
        String lockKey = CommonConstant.LOCK_PREFIX + "stock:" + dto.getProductId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("[库存回滚] 获取分布式锁失败，productId={}", dto.getProductId());
                throw new BizException(ResultCode.DISTRIBUTED_LOCK_ERROR);
            }

            int affected = productMapper.rollbackStock(dto.getProductId(), dto.getQuantity());
            if (affected == 0) {
                log.warn("[库存回滚] 数据库回滚失败，productId={}", dto.getProductId());
                return false;
            }

            String stockKey = CommonConstant.CACHE_STOCK + dto.getProductId();
            redisTemplate.opsForValue().increment(stockKey, dto.getQuantity());

            log.info("[库存回滚] 成功，productId={}，回滚数量={}", dto.getProductId(), dto.getQuantity());
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.DISTRIBUTED_LOCK_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Integer getStock(Long productId) {
        String stockKey = CommonConstant.CACHE_STOCK + productId;
        Object cached = redisTemplate.opsForValue().get(stockKey);
        if (cached != null) {
            return Integer.parseInt(cached.toString());
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return 0;
        }
        redisTemplate.opsForValue().set(stockKey, product.getStock(), 10, TimeUnit.MINUTES);
        return product.getStock();
    }

    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setDescription(product.getDescription());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setImageUrl(product.getImageUrl());
        vo.setCategoryId(product.getCategoryId());
        vo.setStatus(product.getStatus());
        vo.setSales(product.getSales());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }
}
