package com.javatechie;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Iterator;
import java.util.List;

public class FileParserUtil {

    // Method to parse Word files (docx format) and extract headings or bold text
	public static String parseWordFile(MultipartFile file, String extractType) throws IOException {
	    try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
	        StringBuilder result = new StringBuilder();

	        for (XWPFParagraph paragraph : document.getParagraphs()) {
	            if ("bold".equalsIgnoreCase(extractType) || "headings".equalsIgnoreCase(extractType)) {
	                // Loop through each run in the paragraph
	                for (XWPFRun run : paragraph.getRuns()) {
	                    if ("bold".equalsIgnoreCase(extractType) && run.isBold()) {
	                        // Append bold text
	                        result.append(run.getText(0)).append("\n");
	                    } else if ("headings".equalsIgnoreCase(extractType) && paragraph.getStyle() != null) {
	                        // Append heading text if style is not null (assumed to be a heading)
	                        result.append(paragraph.getText()).append("\n");
	                    }
	                }
	            } else {
	                // Append all text if extractType is neither "bold" nor "headings"
	                result.append(paragraph.getText()).append("\n");
	            }
	        }

	        return result.toString();
	    }
	}


    // Method to parse PDF files and extract all text
    public static String parsePdfFile(MultipartFile file, String extractType) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            // Initialize PDFTextStripper for basic text extraction
            PDFTextStripper stripper = new PDFTextStripper();

            // StringBuilder to accumulate the result
            StringBuilder result = new StringBuilder();

            // Extract text based on the required extraction type (bold, headings, or all content)
            if ("bold".equalsIgnoreCase(extractType) || "headings".equalsIgnoreCase(extractType)) {
                // Use custom PDFTextStripper for bold or headings extraction
                PDFTextStripper customStripper = new PDFTextStripper() {
                    @Override
                    protected void processTextPosition(TextPosition text) {
                        if ("bold".equalsIgnoreCase(extractType) && text.getFont().getName().contains("Bold")) {
                            result.append(text.getUnicode()); // Append bold text
                        } else if ("headings".equalsIgnoreCase(extractType) && text.getFontSize() > 12) {
                            result.append(text.getUnicode()); // Append heading text (larger font size)
                        }
                    }
                };
                customStripper.getText(document); // Extract based on bold or headings condition
            } else {
                // Default: Extract all text from the PDF
                result.append(stripper.getText(document));
            }

            return result.toString();
        }
    }


    // Method to parse Excel files (xlsx format) and extract cell values with normal,bold and headings
    public static String parseExcelFile(MultipartFile file, String extractType) throws IOException, InvalidFormatException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {  // Use XSSFWorkbook for .xlsx files
            StringBuilder result = new StringBuilder();

            // Define a threshold for font size (for example, we consider anything above 12 as a possible heading)
            int headingFontSizeThreshold = 12;

            // Iterate through all sheets in the workbook
            for (Sheet sheet : workbook) {
                // Iterate through the first few rows (assuming headings are in the first 3 rows)
                for (int rowIndex = 0; rowIndex < Math.min(5, sheet.getPhysicalNumberOfRows()); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);

                    // Iterate through all cells in the row
                    for (Cell cell : row) {
                        // Get the cell's style to check for font size and boldness
                        CellStyle cellStyle = cell.getCellStyle();
                        Font font = workbook.getFontAt(cellStyle.getFontIndex());
                        int fontSize = font.getFontHeightInPoints();

                        // Check if extractType is "bold"
                        if ("bold".equalsIgnoreCase(extractType) && font.getBold()) {
                            result.append(cell.toString()).append("\t"); // Append only bold text
                        } 
                        // Check if extractType is "heading" (assumes headings are either bold or large font size)
                        else if ("heading".equalsIgnoreCase(extractType) && (font.getBold() || fontSize >= headingFontSizeThreshold)) {
                            result.append(cell.toString()).append("\t"); // Append only heading text
                        }
                        // If extractType is neither "bold" nor "heading", append all text
                        else if (!"bold".equalsIgnoreCase(extractType) && !"heading".equalsIgnoreCase(extractType)) {
                            result.append(cell.toString()).append("\t");
                        }
                    }
                    result.append("\n");
                }
            }
            return result.toString();
        }
    }

    //for return bold content only
//    public static String parseExcelFile(MultipartFile file, String extractType) throws IOException {
//        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
//            StringBuilder result = new StringBuilder();
//
//            for (Sheet sheet : workbook) {
//                for (Row row : sheet) {
//                    for (Cell cell : row) {
//                        CellStyle style = cell.getCellStyle();
//                        if ("bold".equalsIgnoreCase(extractType) && style.getFontIndexAsInt() != 0) {
//                            Font font = workbook.getFontAt(style.getFontIndexAsInt());
//                            if (font.getBold()) {
//                                result.append(cell.toString()).append(" ");
//                            }
//                        }
//                    }
//                }
//            }
//
//            return result.toString().trim();
//        }
//    }

}
