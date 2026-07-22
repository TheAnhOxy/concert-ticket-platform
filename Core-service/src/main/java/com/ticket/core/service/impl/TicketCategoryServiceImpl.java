package com.ticket.core.service.impl;

import com.ticket.core.converter.TicketCategoryConverter;
import com.ticket.core.dto.request.TicketCategoryRequest;
import com.ticket.core.dto.response.TicketCategoryResponse;
import com.ticket.core.entity.Concert;
import com.ticket.core.entity.TicketCategory;
import com.ticket.core.repository.ConcertRepository;
import com.ticket.core.repository.TicketCategoryRepository;
import com.ticket.core.service.TicketCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketCategoryServiceImpl implements TicketCategoryService {

    private final TicketCategoryRepository ticketCategoryRepository;
    private final ConcertRepository concertRepository;
    private final TicketCategoryConverter ticketCategoryConverter;
    private final StringRedisTemplate redisTemplate;
    private final com.ticket.core.service.SeatService seatService;

    private static final String INVENTORY_CACHE_KEY_PREFIX = "inventory:ticket:";
    private static final String PRICE_CACHE_KEY_PREFIX = "price:ticket:";

    @Override
    public Integer getAvailableQuantity(Long id) {
        String cacheKey = INVENTORY_CACHE_KEY_PREFIX + id;
        String cachedQty = redisTemplate.opsForValue().get(cacheKey);
        if (cachedQty != null) {
            try {
                return Integer.parseInt(cachedQty);
            } catch (NumberFormatException e) {
                log.warn("Lỗi parse available quantity từ Redis cho key {}: {}", cacheKey, cachedQty);
            }
        }

        Integer qty = ticketCategoryRepository.getAvailableQuantity(id);
        int finalQty = qty != null ? qty : 0;

        redisTemplate.opsForValue().set(cacheKey, String.valueOf(finalQty));
        return finalQty;
    }

    @Override
    @Transactional
    public void deductQuantity(Long id, int quantity) {
        int updatedRows = ticketCategoryRepository.deductQuantity(id, quantity);
        if (updatedRows == 0) {
            log.error("Không đủ vé trong kho hoặc không tìm thấy TicketCategory ID: {}", id);
            throw new RuntimeException("Số lượng vé không đủ để thực hiện giao dịch!");
        }
        log.info("Đã trừ {} vé cho TicketCategory ID: {}", quantity, id);
    }

    @Override
    @Transactional
    public void refundQuantity(Long id, int quantity) {
        ticketCategoryRepository.refundQuantity(id, quantity);
        log.info("Đã hoàn {} vé cho TicketCategory ID: {}", quantity, id);
    }

    @Override
    public BigDecimal getPrice(Long id) {
        String cacheKey = PRICE_CACHE_KEY_PREFIX + id;
        String cachedPrice = redisTemplate.opsForValue().get(cacheKey);
        if (cachedPrice != null) {
            return new BigDecimal(cachedPrice);
        }

        BigDecimal price = ticketCategoryRepository.findById(id)
                .map(TicketCategory::getPrice)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng vé với ID: " + id));

        redisTemplate.opsForValue().set(cacheKey, price.toString());
        return price;
    }

    @Override
    @Transactional
    public TicketCategoryResponse createTicketCategory(TicketCategoryRequest request) {
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy concert với ID: " + request.getConcertId()));

        TicketCategory category = ticketCategoryConverter.toEntity(request);
        category.setConcert(concert);
        category.setAvailableQuantity(request.getTotalQuantity()); // ban đầu available = total

        TicketCategory saved = ticketCategoryRepository.save(category);

        // Sinh ghế tự động hoặc tạo danh sách ghế tự chọn cho hạng vé vừa tạo
        seatService.generateSeatsForCategory(saved.getId(), saved.getTotalQuantity(), saved.getName(), request.getSeatNumbers());

        // Khởi tạo cache
        redisTemplate.opsForValue().set(INVENTORY_CACHE_KEY_PREFIX + saved.getId(), String.valueOf(saved.getAvailableQuantity()));
        redisTemplate.opsForValue().set(PRICE_CACHE_KEY_PREFIX + saved.getId(), saved.getPrice().toString());

        return ticketCategoryConverter.toResponse(saved);
    }

    @Override
    public TicketCategoryResponse getTicketCategoryById(Long id) {
        TicketCategory category = ticketCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng vé với ID: " + id));
        return ticketCategoryConverter.toResponse(category);
    }

    @Override
    public List<TicketCategoryResponse> getAllTicketCategories() {
        return ticketCategoryRepository.findAll().stream()
                .map(ticketCategoryConverter::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TicketCategoryResponse updateTicketCategory(Long id, TicketCategoryRequest request) {
        TicketCategory existing = ticketCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng vé với ID: " + id));

        if (request.getConcertId() != null && !request.getConcertId().equals(existing.getConcert().getId())) {
            Concert concert = concertRepository.findById(request.getConcertId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy concert với ID: " + request.getConcertId()));
            existing.setConcert(concert);
        }

        existing.setName(request.getName());
        existing.setPrice(request.getPrice());

        int diff = request.getTotalQuantity() - existing.getTotalQuantity();
        existing.setTotalQuantity(request.getTotalQuantity());
        existing.setAvailableQuantity(Math.max(0, existing.getAvailableQuantity() + diff));
        existing.setStatus(request.getStatus());

        TicketCategory saved = ticketCategoryRepository.save(existing);

        // Cập nhật lại cache
        redisTemplate.opsForValue().set(INVENTORY_CACHE_KEY_PREFIX + saved.getId(), String.valueOf(saved.getAvailableQuantity()));
        redisTemplate.opsForValue().set(PRICE_CACHE_KEY_PREFIX + saved.getId(), saved.getPrice().toString());

        return ticketCategoryConverter.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTicketCategory(Long id) {
        if (!ticketCategoryRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy hạng vé với ID: " + id);
        }
        ticketCategoryRepository.deleteById(id);

        // Xóa cache
        redisTemplate.delete(INVENTORY_CACHE_KEY_PREFIX + id);
        redisTemplate.delete(PRICE_CACHE_KEY_PREFIX + id);
    }
}