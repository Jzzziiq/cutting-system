package com.cutting.cuttingsystem.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BoardTextureStorageService {
    String store(MultipartFile file) throws IOException;
}
