package com.ticket.core.converter;

import com.ticket.core.dto.request.ConcertRequest;
import com.ticket.core.dto.response.ConcertResponse;
import com.ticket.core.entity.Concert;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertConverter {

    private final ModelMapper modelMapper;

    public Concert toEntity(ConcertRequest request) {
        if (request == null) {
            return null;
        }
        return modelMapper.map(request, Concert.class);
    }

    public ConcertResponse toResponse(Concert entity) {
        if (entity == null) {
            return null;
        }
        return modelMapper.map(entity, ConcertResponse.class);
    }
}
