package dev.p3ntest.zombies.service;

import dev.p3ntest.zombies.entity.CustomAsset;
import dev.p3ntest.zombies.entity.User;
import dev.p3ntest.zombies.repository.CustomAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final CustomAssetRepository assetRepository;
    
    @Value("${assets.service.url}")
    private String assetsServiceUrl;
    
    @Value("${assets.service.token}")
    private String assetsServiceToken;

    private final WebClient webClient = WebClient.builder().build();

    public String getAssetsEndpoint() {
        return assetsServiceUrl;
    }

    public List<Map<String, Object>> viewAssetLibrary(String search) {
        List<CustomAsset> assets = search != null && !search.isEmpty()
            ? assetRepository.searchByNameOrDescription(search)
            : assetRepository.findAll();
        
        return assets.stream()
            .map(asset -> Map.<String, Object>of(
                "id", asset.getId(),
                "uploadId", asset.getUploadId(),
                "name", asset.getName(),
                "tags", asset.getTags()
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public String uploadAssetFromUrl(String externalUrl, String name, User user) {
        if (externalUrl == null || externalUrl.isEmpty()) {
            return "No file provided";
        }

        Map<String, String> response = webClient.post()
            .uri(assetsServiceUrl + "/from-url")
            .header("Authorization", "Bearer " + assetsServiceToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("url", externalUrl))
            .retrieve()
            .bodyToMono(Map.class)
            .map(resp -> (Map<String, String>) resp)
            .block();

        String uploadId = response.get("id");

        CustomAsset asset = new CustomAsset();
        asset.setId(UUID.randomUUID().toString());
        asset.setUploadId(uploadId);
        asset.setName(name);
        asset.setUploadedBy(user);
        assetRepository.save(asset);

        return uploadId;
    }

    @Transactional
    public void handleAssetUpload(User user, MultipartFile file, String assetName) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());

            Map<String, String> response = webClient.post()
                .uri(assetsServiceUrl + "/upload")
                .header("Authorization", "Bearer " + assetsServiceToken)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> (Map<String, String>) resp)
                .block();

            String uploadId = response.get("id");

            CustomAsset asset = new CustomAsset();
            asset.setId(UUID.randomUUID().toString());
            asset.setUploadId(uploadId);
            asset.setName(assetName != null ? assetName : file.getOriginalFilename());
            asset.setUploadedBy(user);
            assetRepository.save(asset);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload asset", e);
        }
    }
}


