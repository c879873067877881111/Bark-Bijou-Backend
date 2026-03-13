package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.upload.base-dir:./uploads}")
    private String baseDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(baseDir));
            log.info("上傳根目錄已就緒: {}", Paths.get(baseDir).toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("無法建立上傳目錄: " + baseDir, e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("檔案不得為空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String filename = UUID.randomUUID() + ext;

        Path dir = Paths.get(baseDir, subDir);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            log.debug("檔案已儲存: {}", target.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("檔案儲存失敗: " + filename, e);
        }

        return "/uploads/" + subDir + "/" + filename;
    }
}
