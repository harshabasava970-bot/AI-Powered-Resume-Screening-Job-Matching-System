package com.resumescreening.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Abstraction for file storage.
 * Two implementations exist:
 *  - LocalFileStorageService  — stores on local disk (development)
 *  - CloudinaryFileStorageService — stores on Cloudinary CDN (production)
 *
 * The active implementation is selected by the STORAGE_TYPE environment variable:
 *   STORAGE_TYPE=local      → LocalFileStorageService  (default)
 *   STORAGE_TYPE=cloudinary → CloudinaryFileStorageService
 */
public interface FileStorageService {

    /**
     * Store an uploaded file and return a storage reference (path or URL).
     *
     * @param file     the uploaded multipart file
     * @param filename the desired stored filename (UUID-based)
     * @return storage reference — local path or Cloudinary public URL
     */
    String store(MultipartFile file, String filename);

    /**
     * Open an input stream to a previously stored file.
     *
     * @param storageRef the reference returned by store()
     * @return an InputStream to read the file content
     */
    InputStream retrieve(String storageRef);

    /**
     * Delete a stored file.
     *
     * @param storageRef the reference returned by store()
     */
    void delete(String storageRef);

    /**
     * Returns true if this implementation persists files beyond container restarts.
     */
    boolean isPersistent();
}
