package com.project;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service {

    @Autowired
    private AmazonS3 amazonS3;


    private String bucketName = "my-bucket-for-project";

    /**
     * Upload a file to S3
     * @param file The file to upload
     * @return The S3 key of the uploaded file
     * @throws IOException If there's an error reading the file
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // Create metadata for the file
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        // Generate a unique key for the file
        String key = generateKey(file.getOriginalFilename());

        // Upload the file to S3
        amazonS3.putObject(new PutObjectRequest(
                bucketName,
                key,
                file.getInputStream(),
                metadata
        ).withCannedAcl(CannedAccessControlList.Private));

        return key;
    }

    /**
     * List files in the S3 bucket with optional search
     * @param searchTerm Optional search term to filter results
     * @return List of file information
     */
    public List<FileInfo> listFiles(String searchTerm) {
        try {
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withMaxKeys(1000);

            ListObjectsV2Result result = amazonS3.listObjectsV2(request);
            List<S3ObjectSummary> objects = result.getObjectSummaries();

            List<FileInfo> fileInfoList = objects.stream()
                    .map(obj -> new FileInfo(
                            obj.getKey(),
                            extractFileName(obj.getKey()),
                            obj.getSize(),
                            obj.getLastModified().getTime()
                    ))
                    .collect(Collectors.toList());

            // Filter by search term if provided
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String term = searchTerm.toLowerCase().trim();
                fileInfoList = fileInfoList.stream()
                        .filter(file -> file.getName().toLowerCase().contains(term))
                        .collect(Collectors.toList());
            }

            return fileInfoList;
        } catch (AmazonS3Exception e) {
            // Log the exception
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get a file from S3
     * @param key The S3 key of the file
     * @return The S3Object containing the file data
     */
    public S3Object getFile(String key) {
        return amazonS3.getObject(bucketName, key);
    }

    /**
     * Delete a file from S3
     * @param key The S3 key of the file to delete
     */
    public void deleteFile(String key) {
        amazonS3.deleteObject(bucketName, key);
    }

    /**
     * Check if a file exists in S3
     * @param key The S3 key to check
     * @return True if the file exists, false otherwise
     */
    public boolean fileExists(String key) {
        return amazonS3.doesObjectExist(bucketName, key);
    }

    /**
     * Generate a unique key for the file
     * @param filename Original filename
     * @return Unique S3 key
     */
    private String generateKey(String filename) {
        return System.currentTimeMillis() + "_" + filename;
    }

    /**
     * Extract the original filename from the S3 key
     * @param key S3 key
     * @return Original filename
     */
    private String extractFileName(String key) {
        int underscoreIndex = key.indexOf('_');
        return underscoreIndex >= 0 ? key.substring(underscoreIndex + 1) : key;
    }

    /**
     * FileInfo class to represent file information
     */
    public static class FileInfo {
        private String key;
        private String name;
        private long size;
        private long lastModified;

        public FileInfo(String key, String name, long size, long lastModified) {
            this.key = key;
            this.name = name;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    }
}