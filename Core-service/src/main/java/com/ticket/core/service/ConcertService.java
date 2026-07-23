package com.ticket.core.service;

import com.ticket.core.dto.request.ConcertRequest;
import com.ticket.core.dto.response.ConcertResponse;

import java.util.List;

public interface ConcertService {
    ConcertResponse createConcert(ConcertRequest request);
    ConcertResponse getConcertById(Long id);
    List<ConcertResponse> getAllConcerts();
    ConcertResponse updateConcert(Long id, ConcertRequest request);
    void deleteConcert(Long id);
}
