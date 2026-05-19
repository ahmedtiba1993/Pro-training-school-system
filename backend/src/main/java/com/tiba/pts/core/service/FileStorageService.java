package com.tiba.pts.core.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

  private final Path baseStorageLocation;

  public FileStorageService(@Value("${app.storage.upload-dir:uploads/}") String uploadDir) {
    this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.baseStorageLocation);
    } catch (Exception ex) {
      log.error("Error creating the root upload directory.", ex);
      throw new RuntimeException("COULD_NOT_CREATE_UPLOAD_DIRECTORY", ex);
    }
  }

  /**
   * STORE A FILE WITH A RANDOM NAME (UUID)
   *
   * @param file The Multipart file from the Controller
   * @param subDirectory The subfolder (e.g., "users/avatars")
   * @return The relative path to save in the database
   */
  public String storeFile(MultipartFile file, String subDirectory) {
    // Calls the main function, passing null for the custom name
    return storeFile(file, subDirectory, null);
  }

  /**
   * STORE A FILE WITH A CUSTOM NAME (Overload)
   *
   * @param file The Multipart file from the Controller
   * @param subDirectory The subfolder (e.g., "subjects")
   * @param customFilename The desired name without extension (e.g., "MATH-101")
   * @return The relative path to save in the database (e.g., "subjects/MATH-101.pdf")
   */
  public String storeFile(MultipartFile file, String subDirectory, String customFilename) {
    if (file == null || file.isEmpty()) {
      throw new BusinessValidationException("FILE_CANNOT_BE_EMPTY");
    }

    String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

    // Security: prevent "Directory Traversal" attacks
    if (originalFilename.contains("..")) {
      throw new BusinessValidationException("FILENAME_CONTAINS_INVALID_PATH_SEQUENCE");
    }

    // Extract the original file extension (.pdf, .png, etc.)
    String fileExtension = "";
    if (originalFilename.contains(".")) {
      fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    // Determine the final name
    String finalFilename;
    if (customFilename != null && !customFilename.trim().isEmpty()) {
      // Clean the custom name to avoid special OS characters
      String sanitizedName = customFilename.trim().replaceAll("[^a-zA-Z0-9\\-_]", "");
      finalFilename = sanitizedName + fileExtension;
    } else {
      // Generate a unique name by default if no name is provided
      finalFilename = UUID.randomUUID().toString() + fileExtension;
    }

    try {
      // Dynamically create the subfolder if it doesn't exist yet
      Path targetLocation = this.baseStorageLocation.resolve(subDirectory);
      Files.createDirectories(targetLocation);

      // Physical copy of the file (REPLACE_EXISTING will overwrite the old file if it has the same
      // name)
      Path targetFile = targetLocation.resolve(finalFilename);
      Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

      return subDirectory + "/" + finalFilename;

    } catch (IOException ex) {
      log.error("Could not save file: {}", finalFilename, ex);
      throw new RuntimeException("FILE_STORAGE_ERROR", ex);
    }
  }

  /**
   * RETRIEVE A FILE (For download or display in frontend)
   *
   * @param filePath The path stored in the database (e.g., "subjects/123-abc.pdf")
   * @return The resource ready to be returned via ResponseEntity
   */
  public Resource loadFileAsResource(String filePath) {
    try {
      Path file = this.baseStorageLocation.resolve(filePath).normalize();
      Resource resource = new UrlResource(file.toUri());
      if (resource.exists() && resource.isReadable()) {
        return resource;
      } else {
        throw new ResourceNotFoundException("FILE_NOT_FOUND");
      }
    } catch (MalformedURLException ex) {
      throw new ResourceNotFoundException("FILE_NOT_FOUND");
    }
  }

  /**
   * PHYSICALLY DELETE A FILE
   *
   * @param filePath The path stored in the database
   */
  public void deleteFile(String filePath) {
    if (filePath == null || filePath.trim().isEmpty()) {
      return;
    }
    try {
      Path file = this.baseStorageLocation.resolve(filePath).normalize();
      Files.deleteIfExists(file);
      log.info("File successfully deleted: {}", filePath);
    } catch (IOException ex) {
      log.warn("Could not delete old file: {}", filePath, ex);
    }
  }
}
