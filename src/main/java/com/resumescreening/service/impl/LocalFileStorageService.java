package com.resumescreening.service.impl;

import com.resumescreening.config.FileStorageConfig;
import com.resumescreening.exception.InvalidFileException;
import com.resumescreening.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local disk file storage — used when STORAGE_TYPE=local (default).
 * Files are stored under the configured upload directory.
 * Note: files are NOT persistent across container restarts on Render free tier.
 * Use CloudinaryFileStorageService for persistent storage.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final FileStorageConfig fileStorageConfig;

    public LocalFileStorageService(FileStorageConfig fileStorageConfig) {
        this.fileStorageConfig = fileStorageConfig;
    }

    @Override
    public String store(MultipartFile file, String filename) {
        Path uploadPath = Paths.get(fileStorageConfig.getUploadDir()).toAbsolutePath().normalize();
        Path targetPath = uploadPath.resolve(filename);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored locally at: {}", targetPath);
            return targetPath.toString();
        } catch (IOException e) {
            log.error("Failed to store file locally: {}", e.getMessage());
            throw new RuntimeException("Failed to store file. Please try again.");
        }
    }

    @Override
    public InputStream retrieve(String storageRef) {
        Path filePath = Paths.get(storageRef);
        if (!Files.exists(filePath)) {
            throw new com.resumescreening.exception.ResourceNotFoundException(
                    "Resume file not found on disk. It may have been lost after a server restart. Please re-upload.");
        }
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        }
    }

    @Override
    public void delete(String storageRef) {
        try {
            Files.deleteIfExists(Paths.get(storageRef));
            log.info("File deleted: {}", storageRef);
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", storageRef, e.getMessage());
        }
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}
