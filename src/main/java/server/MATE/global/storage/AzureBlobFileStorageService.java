package server.MATE.global.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import server.MATE.global.config.properties.AzureBlobProperties;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AzureBlobFileStorageService implements FileStorageService {

    private final AzureBlobProperties properties;
    private BlobContainerClient containerClient;

    @PostConstruct
    public void init() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(properties.getConnectionString())
                .buildClient();
        containerClient = serviceClient.getBlobContainerClient(properties.getContainerName());
        containerClient.createIfNotExists();
    }

    @Override
    public String generatePresignedUrl(String key) {
        BlobClient blobClient = containerClient.getBlobClient(key);
        BlobSasPermission permission = new BlobSasPermission()
                .setCreatePermission(true)
                .setWritePermission(true)
                .setAddPermission(true);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusMinutes(properties.getUploadSasExpiryMinutes()), permission)
                .setContentType(resolveContentType(key));
        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
    }

    @Override
    public String generateDownloadUrl(String key) {
        BlobClient blobClient = containerClient.getBlobClient(key);
        BlobSasPermission permission = new BlobSasPermission()
                .setReadPermission(true);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusMinutes(properties.getDownloadSasExpiryMinutes()), permission);
        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
    }

    @Override
    public void deleteFiles(List<String> keys) {
        keys.forEach(key -> containerClient.getBlobClient(key).deleteIfExists());
    }

    private String resolveContentType(String key) {
        String ext = key.substring(key.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "pdf" -> "application/pdf";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }
}
