package server.MATE.global.image;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;
import server.MATE.global.config.properties.AzureBlobProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class AzureBlobImageService implements ImageService {

    private final AzureBlobProperties properties;
    private BlobContainerClient containerClient;

    @PostConstruct
    public void init() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(properties.getConnectionString())
                .buildClient();
        containerClient = serviceClient.getBlobContainerClient(properties.getContainerName());
        if (!containerClient.exists()) {
            containerClient.create();
        }
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files) {
        List<String> keys = new ArrayList<>();
        for (MultipartFile file : files) {
            keys.add(upload(file));
        }
        return keys;
    }

    private String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String key = UUID.randomUUID() + extension;
        BlobClient blobClient = containerClient.getBlobClient(key);
        try {
            blobClient.upload(file.getInputStream(), file.getSize(), true);
        } catch (IOException e) {
            throw new BaseException(ErrorCode.FILE_UPLOAD_FAIL);
        }
        return key;
    }
}
