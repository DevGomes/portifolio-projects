package com.brenogomes.pm.client;

import com.brenogomes.pm.model.dto.MemberExternalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberClient {

    private final RestTemplate restTemplate;

    @Value("${member.api.url}")
    private String memberApiUrl;

    public List<MemberExternalResponse> findAll() {
        log.info("Buscando todos os membros na API externa: {}", memberApiUrl);
        MemberExternalResponse[] response = restTemplate.getForObject(
                memberApiUrl + "/members",
                MemberExternalResponse[].class
        );
        return response != null ? Arrays.asList(response) : List.of();
    }

    public Optional<MemberExternalResponse> findById(Long id) {
        log.info("Buscando membro id {} na API externa: {}", id, memberApiUrl);
        try {
            MemberExternalResponse response = restTemplate.getForObject(
                    memberApiUrl + "/members/{id}",
                    MemberExternalResponse.class,
                    id
            );
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Membro id {} não encontrado na API externa", id);
            return Optional.empty();
        }
    }

    public List<MemberExternalResponse> findAllByIds(List<Long> ids) {
        return ids.stream()
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}