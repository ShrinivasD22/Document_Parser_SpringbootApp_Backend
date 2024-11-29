package com.javatechie;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.annotation.Resource;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.io.source.ByteArrayOutputStream;
import com.javatechie.service.StorageService;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.springframework.http.HttpHeaders;

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
    
    @PostMapping("/convert")
    public ResponseEntity<ByteArrayResource> convertFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversionType") String conversionType) {
        try {
            // Call the service method to handle the conversion
            File convertedFile;
            String outputFileName;

            if ("wordToPdf".equalsIgnoreCase(conversionType)) {
                convertedFile = service.convertWordToPdf(file);
                outputFileName = file.getOriginalFilename().replace(".docx", ".pdf");
            } else if ("pdfToWord".equalsIgnoreCase(conversionType)) {
                convertedFile = service.convertPdfToWord(file);
                outputFileName = file.getOriginalFilename().replace(".pdf", ".docx");
            } else {
                return ResponseEntity.badRequest()
                        .body(null); // Unsupported conversion type
            }

            // Convert the file into a ByteArrayResource
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(convertedFile.toPath()));

            // Set headers for downloading
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // Handle errors gracefully
        }
    }

    
    //extract text from image ocr Tesseract library
    @PostMapping("/extract-textfromimage")
   
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) {
        // Temporary file creation
        File tempFile;
        try {
            tempFile = File.createTempFile("uploaded-", file.getOriginalFilename());
            file.transferTo(tempFile);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing the uploaded file: " + e.getMessage());
        }

        // Tesseract OCR processing
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
 // Set the path to your tessdata directory
            tesseract.setLanguage("eng"); // Specify the language

            String extractedText = tesseract.doOCR(tempFile);

            // Delete the temporary file
            tempFile.delete();

            return ResponseEntity.ok(extractedText);

        } catch (TesseractException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("OCR error: " + e.getMessage());
        }
    }
}
