package com.sobczyk.testThing.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sobczyk.testThing.config.BucketConfig;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class FileAction {

    private final AmazonFileStore amazonFileStore;
    private final AmazonS3 s3;

    @Autowired
    public FileAction(AmazonFileStore amazonFileStore, AmazonS3 s3) {
        this.amazonFileStore = amazonFileStore;
        this.s3 = s3;
    }

    public void uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalStateException("File is empty.");
        }
        if (!Arrays.asList(ContentType.IMAGE_JPEG.getMimeType(), ContentType.IMAGE_PNG.getMimeType(), ContentType.IMAGE_GIF.getMimeType())
                .contains(file.getContentType())) {
            throw new IllegalStateException("File must be in jpeg, png or gif format.");
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", file.getContentType());
        metadata.put("Content-Length", String.valueOf(file.getSize()));

        String path = String.format("%s/%s", BucketConfig.MY_BUCKET.getName(), BucketConfig.FOLDER_NAME.getName());
        String filename = String.format("%s", file.getOriginalFilename());
        try {
            this.amazonFileStore.upload(path, filename, Optional.of(metadata), file.getInputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<byte[]> downloadImages() {
        List<byte[]> files = new ArrayList<>();
        ObjectListing objects = this.s3.listObjects(BucketConfig.MY_BUCKET.getName(), "testThing");
        while (true) {
            List<S3ObjectSummary> objectSummaries = objects.getObjectSummaries();
            if (objectSummaries.size() < 1) {
                break;
            }
            for (S3ObjectSummary item : objectSummaries) {
                if (!item.getKey().endsWith("/")) {
                    String path = String.format("%s/%s",
                            BucketConfig.MY_BUCKET.getName(),
                            BucketConfig.FOLDER_NAME.getName());
                    files.add(this.amazonFileStore.download(path, item.getKey().replace(BucketConfig.FOLDER_NAME.getName() + "/", "")));
                }
            }
            objects = this.s3.listNextBatchOfObjects(objects);
        }
        return files;
    }
}