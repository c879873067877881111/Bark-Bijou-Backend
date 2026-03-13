package com.smallnine.apiserver.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * 儲存上傳檔案到指定子目錄
     *
     * @param file   上傳的檔案
     * @param subDir 子目錄名稱（例如 "member_images"、"articles"）
     * @return 可透過 HTTP 存取的 URL 路徑
     */
    String store(MultipartFile file, String subDir);
}
