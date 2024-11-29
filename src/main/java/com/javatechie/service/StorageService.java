package com.javatechie.service;

import com.javatechie.entity.ImageData;
import com.javatechie.respository.StorageRepository;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.javatechie.FileParserUtil;
import com.javatechie.util.ImageUtils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
                } else if (file.getOriginalFilename().endsWith(".pptx")) {
                    return FileParserUtil.parsePptFile(file, extractType);
                }
                throw new IllegalArgumentException("Unsupported x-tika-ooxml file subtype");

            case "application/pdf": // PDF
                return FileParserUtil.parsePdfFile(file, extractType);

            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": // Excel
                return FileParserUtil.parseExcelFile(file, extractType);

            case "application/vnd.openxmlformats-officedocument.presentationml.presentation": // PowerPoint
                return FileParserUtil.parsePptFile(file, extractType);

            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }
    //convert wordtopdf api
    public File convertWordToPdf(MultipartFile wordFile) throws IOException {
        // Open Word document
        XWPFDocument document = new XWPFDocument(wordFile.getInputStream());

        // Temporary file for PDF output
        File pdfFile = File.createTempFile("converted", ".pdf");

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            PdfWriter pdfWriter = new PdfWriter(fos);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(pdfWriter);
            Document pdfDocument = new Document(pdfDoc);

            // Set a font to avoid issues with text rendering
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Add each paragraph from the Word document
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    pdfDocument.add(new Paragraph(text).setFont(font));
                }
            }

            pdfDocument.close();
        }

        return pdfFile;
    }



    public File convertPdfToWord(MultipartFile file) throws IOException {
        // Create a temporary Word file
        File outputFile = File.createTempFile("converted", ".docx");

        // Load PDF document
        try (InputStream pdfInputStream = file.getInputStream();
             PDDocument pdfDocument = PDDocument.load(pdfInputStream);
             XWPFDocument wordDocument = new XWPFDocument()) {

            // Use PDFBox to extract text
            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(pdfDocument);

            // Create a Word document
            String[] lines = pdfText.split("\n");
            for (String line : lines) {
                XWPFParagraph paragraph = wordDocument.createParagraph();
                paragraph.createRun().setText(line.trim());
            }

            // Write the Word document to a file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                wordDocument.write(fos);
            }
        }

        return outputFile;
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
