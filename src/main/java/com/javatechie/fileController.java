package com.javatechie;

import com.javatechie.service.StorageService;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class fileController {

    @Autowired
    private StorageService service;

    public static void main(String[] args) {
        SpringApplication.run(fileController.class, args);
    }

    // Upload image
    @PostMapping("/image/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) throws IOException {
        String response = service.uploadImage(file);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Download image by filename
    @GetMapping("/image/{fileName}")
    public ResponseEntity<?> downloadImage(@PathVariable String fileName) {
        byte[] imageData = service.downloadImage(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_PNG)
                .body(imageData);
    }

    // Parse uploaded file
    @PostMapping("/file/parse")
    public ResponseEntity<?> parseFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("extractType") String extractType) throws IOException, InvalidFormatException {
        String parsedData = service.parseFile(file, extractType);
        return ResponseEntity.status(HttpStatus.OK).body(parsedData);
    }

    // Other endpoints for images
    @GetMapping("/image/all")
    public ResponseEntity<List<String>> getAllImages() {
        List<String> imageList = service.getAllImages();
        return ResponseEntity.status(HttpStatus.OK).body(imageList);
    }

    @PutMapping("/image/{fileName}")
    public ResponseEntity<?> updateImage(@PathVariable String fileName, @RequestParam("image") MultipartFile file) throws IOException {
        String response = service.updateImage(fileName, file);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/image/{fileName}")
    public ResponseEntity<?> deleteImage(@PathVariable String fileName) {
        String response = service.deleteImage(fileName);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
