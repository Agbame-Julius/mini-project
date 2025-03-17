package com.project;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

import com.project.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class S3Controller {

    @Autowired
    private S3Service s3FileService;

    @Value("${aws.bucketName}")
    private String bucketName;

    @Value("${api.endpoint}")
    private String apiEndpoint;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("bucketName", bucketName);
        model.addAttribute("apiEndpoint", apiEndpoint);
        return "index";
    }

    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return new ResponseEntity<>("Please select a file to upload", HttpStatus.BAD_REQUEST);
            }

            String key = s3FileService.uploadFile(file);
            return new ResponseEntity<>("File uploaded successfully", HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/files")
    @ResponseBody
    public List<S3Service.FileInfo> listFiles(@RequestParam(value = "search", required = false) String searchTerm) {
        return s3FileService.listFiles(searchTerm);
    }

    @GetMapping("/api/download")
    public void downloadFile(@RequestParam("key") String key,
                             @RequestParam("filename") String filename,
                             HttpServletResponse response) {
        try {
            if (!s3FileService.fileExists(key)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            S3Object s3Object = s3FileService.getFile(key);
            S3ObjectInputStream inputStream = s3Object.getObjectContent();

            // Set content type and attachment header
            response.setContentType(s3Object.getObjectMetadata().getContentType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                    URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()) + "\"");

            // Copy the stream to response output stream
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();

            inputStream.close();
        } catch (IOException e) {
            try {
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error downloading file: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @PostMapping("/api/delete")
    @ResponseBody
    public ResponseEntity<String> deleteFile(@RequestParam("key") String key) {
        try {
            if (!s3FileService.fileExists(key)) {
                return new ResponseEntity<>("File not found", HttpStatus.NOT_FOUND);
            }

            s3FileService.deleteFile(key);
            return new ResponseEntity<>("File deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Failed to delete file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}