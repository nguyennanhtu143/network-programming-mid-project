package dev.p3ntest.zombies.controller;

import dev.p3ntest.zombies.entity.User;
import dev.p3ntest.zombies.service.AssetService;
import dev.p3ntest.zombies.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final UserService userService;

    @GetMapping("/endpoint")
    public ResponseEntity<String> getAssetsEndpoint() {
        return ResponseEntity.ok(assetService.getAssetsEndpoint());
    }

    @GetMapping("/library")
    public ResponseEntity<List<Map<String, Object>>> viewAssetLibrary(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(assetService.viewAssetLibrary(search));
    }

    @PostMapping("/upload-from-url")
    public ResponseEntity<String> uploadAssetFromUrl(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserFromJwt(jwt);
        String externalUrl = request.get("externalUrl");
        String name = request.get("name");
        return ResponseEntity.ok(assetService.uploadAssetFromUrl(externalUrl, name, user));
    }

    @PostMapping("/create")
    public ResponseEntity<String> createAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("assetName") String assetName,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserFromJwt(jwt);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        assetService.handleAssetUpload(user, file, assetName);
        return ResponseEntity.ok("ok");
    }
}


