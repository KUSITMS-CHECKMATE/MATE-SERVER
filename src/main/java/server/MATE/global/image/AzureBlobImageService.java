package server.MATE.global.image;

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
public class AzureBlobImageService implements ImageService {

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
    public String generatePresignedUrl(String imageKey) {
        BlobClient blobClient = containerClient.getBlobClient(imageKey);
        BlobSasPermission permission = new BlobSasPermission()
                .setCreatePermission(true)
                .setWritePermission(true)
                .setAddPermission(true);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusMinutes(10), permission);
        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
    }

    @Override
    public void deleteFiles(List<String> keys) {
        keys.forEach(key -> containerClient.getBlobClient(key).deleteIfExists());
    }
}
