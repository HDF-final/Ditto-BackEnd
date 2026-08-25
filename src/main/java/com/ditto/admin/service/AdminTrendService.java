package com.ditto.admin.service;

import org.springframework.stereotype.Service;

import com.ditto.admin.domain.TrendArtifactType;
import com.ditto.admin.dto.response.TrendArtifactResponse;
import com.ditto.admin.repository.TrendArtifactRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminTrendService {

    private final TrendArtifactRepository trendArtifactRepository;

    public TrendArtifactResponse getTop4() {
        return trendArtifactRepository.findLatest(TrendArtifactType.TOP4);
    }

    public TrendArtifactResponse getCandidates() {
        return trendArtifactRepository.findLatest(TrendArtifactType.CANDIDATES);
    }

    public TrendArtifactResponse getYoutube() {
        return trendArtifactRepository.findLatest(TrendArtifactType.YOUTUBE);
    }
}
