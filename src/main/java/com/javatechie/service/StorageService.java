package com.javatechie.service;

import com.javatechie.entity.ImageData;
import com.javatechie.respository.StorageRepository;
import com.javatechie.FileParserUtil;
import com.javatechie.util.ImageUtils;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StorageService {

    @Autowired
    private StorageRepository repository;

    private final Tika tika = new Tika();

    // Upload image
    public String uploadImage(MultipartFile file) throws IOException {
        ImageData imageData = repository.save(ImageData.builder()
                .name(file.getOriginalFilename())
                .type(file.getContentType())
                .imageData(ImageUtils.compressImage(file.getBytes())).build());
        if (imageData != null) {
            return "File uploaded successfully: " + file.getOriginalFilename();
        }
        return null;
    }

    // Download image by filename
    public byte[] downloadImage(String fileName) {
        Optional<ImageData> dbImageData = repository.findByName(fileName);
        return dbImageData.map(data -> ImageUtils.decompressImage(data.getImageData())).orElse(null);
    }

    // Get image by ID
    public byte[] getImageById(Long id) {
        Optional<ImageData> dbImageData = repository.findById(id);
        return dbImageData.map(data -> ImageUtils.decompressImage(data.getImageData())).orElse(null);
    }

    // Get all image names
    public List<String> getAllImages() {
        return repository.findAll().stream().map(ImageData::getName).collect(Collectors.toList());
    }

    // Update image by filename
    public String updateImage(String fileName, MultipartFile file) throws IOException {
        Optional<ImageData> dbImageData = repository.findByName(fileName);
        if (dbImageData.isPresent()) {
            ImageData updatedImageData = dbImageData.get();
            updatedImageData.setImageData(ImageUtils.compressImage(file.getBytes()));
            updatedImageData.setType(file.getContentType());
            repository.save(updatedImageData);
            return "Image updated successfully: " + fileName;
        }
        return "Image not found: " + fileName;
    }

    // Delete image by filename
    @Transactional
    public String deleteImage(String fileName) {
        Optional<ImageData> dbImageData = repository.findByName(fileName);
        if (dbImageData.isPresent()) {
            repository.deleteByName(fileName);
            return "Image deleted successfully: " + fileName;
        }
        return "Image not found: " + fileName;
    }

    
    
    
    
 // Add this method in your StorageService
    private String detectFileType(MultipartFile file) throws IOException {
        Tika tika = new Tika();
        return tika.detect(file.getInputStream());
    }

    // Use it like this:
    
    public String parseFile(MultipartFile file, String extractType) throws IOException, InvalidFormatException {
        String fileType = detectFileType(file);

        switch (fileType) {
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document": // Word
            case "application/x-tika-ooxml": // Generic Office files
                if (file.getOriginalFilename().endsWith(".docx")) {
                    return FileParserUtil.parseWordFile(file, extractType);
                } else if (file.getOriginalFilename().endsWith(".xlsx")) {
                    return FileParserUtil.parseExcelFile(file, extractType);
                }
                throw new IllegalArgumentException("Unsupported x-tika-ooxml file subtype");

            case "application/pdf": // PDF
                return FileParserUtil.parsePdfFile(file, extractType);

            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": // Excel
                return FileParserUtil.parseExcelFile(file, extractType);

            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }

    // Parse uploaded file (Word, PDF, or Excel)
//    public String parseFile(MultipartFile file, String extractType) throws IOException, InvalidFormatException {
//        String fileType = tika.detect(file.getInputStream());
//
//        if (fileType.contains("word")) {
//            return FileParserUtil.parseWordFile(file, extractType);
//        } else if (fileType.contains("pdf")) {
//            return FileParserUtil.parsePdfFile(file, extractType);
//        } else if (fileType.contains("excel")) {
//            return FileParserUtil.parseExcelFile(file, extractType);
//        } else {
//            throw new IllegalArgumentException("Unsupported file type: " + fileType);
//        }
//    }
}
