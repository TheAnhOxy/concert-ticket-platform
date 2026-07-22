package com.ticket.core.service.impl;

import com.ticket.core.converter.ConcertConverter;
import com.ticket.core.dto.request.ConcertRequest;
import com.ticket.core.dto.response.ConcertResponse;
import com.ticket.core.entity.Concert;
import com.ticket.core.repository.ConcertRepository;
import com.ticket.core.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConcertServiceImpl implements ConcertService {

    private final ConcertRepository concertRepository;
    private final ConcertConverter concertConverter;

    @Override
    @Transactional
    public ConcertResponse createConcert(ConcertRequest request) {
        Concert concert = concertConverter.toEntity(request);
        Concert saved = concertRepository.save(concert);
        return concertConverter.toResponse(saved);
    }

    @Override
    public ConcertResponse getConcertById(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy concert với ID: " + id));
        return concertConverter.toResponse(concert);
    }

    @Override
    public List<ConcertResponse> getAllConcerts() {
        return concertRepository.findAll().stream()
                .map(concertConverter::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConcertResponse updateConcert(Long id, ConcertRequest request) {
        Concert existing = concertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy concert với ID: " + id));
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setVenue(request.getVenue());
        existing.setStartTime(request.getStartTime());
        existing.setStatus(request.getStatus());
        Concert saved = concertRepository.save(existing);
        return concertConverter.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteConcert(Long id) {
        if (!concertRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy concert với ID: " + id);
        }
        concertRepository.deleteById(id);
    }
}
