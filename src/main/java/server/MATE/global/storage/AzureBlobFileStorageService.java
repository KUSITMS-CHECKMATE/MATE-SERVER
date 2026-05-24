package server.MATE.global.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import server.MATE.global.config.properties.AzureBlobProperties;
import server.MATE.global.storage.condition.AzureStorageConfiguredCondition;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Conditional(AzureStorageConfiguredCondition.class)
@RequiredArgsConstructor
public class AzureBlobFileStorageService implements FileStorageService {

    private final AzureBlobProperties properties;
    private BlobContainerClient publicContainerClient;
    private BlobContainerClient privateContainerClient;

    @PostConstruct
    public void init() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(properties.getConnectionString())
                .buildClient();

        publicContainerClient = serviceClient.getBlobContainerClient(properties.getPublicContainerName());
        publicContainerClient.createIfNotExists();

        privateContainerClient = serviceClient.getBlobContainerClient(properties.getPrivateContainerName());
        privateContainerClient.createIfNotExists();
    }

    @Override
    public String generatePresignedUrl(String key) {
        BlobClient blobClient = getContainerClient(key).getBlobClient(key);
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
        BlobClient blobClient = getContainerClient(key).getBlobClient(key);
        if (isPublic(key)) {
            return blobClient.getBlobUrl();
        }
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusMinutes(properties.getDownloadSasExpiryMinutes()), permission);
        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
    }

    @Override
    public void deleteFiles(List<String> keys) {
        keys.forEach(key -> getContainerClient(key).getBlobClient(key).deleteIfExists());
    }

    private BlobContainerClient getContainerClient(String key) {
        return isPublic(key) ? publicContainerClient : privateContainerClient;
    }

    private boolean isPublic(String key) {
        return key.startsWith("media/");
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
