package server.MATE.global.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import server.MATE.global.config.properties.AzureBlobProperties;
import server.MATE.global.storage.condition.AzureStorageConfiguredCondition;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
    public String generateDownloadUrl(String key, String downloadFilename) {
        BlobClient blobClient = getContainerClient(key).getBlobClient(key);
        if (isPublic(key)) {
            return blobClient.getBlobUrl();
        }
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusMinutes(properties.getDownloadSasExpiryMinutes()), permission)
                .setContentDisposition("attachment; filename=\"" + downloadFilename + "\"");
        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
    }

    @Override
    public void deleteFiles(List<String> keys) {
        keys.forEach(key -> getContainerClient(key).getBlobClient(key).deleteIfExists());
    }

    @Override
    public void upload(String key, byte[] content, String contentType) {
        BlobClient blobClient = getContainerClient(key).getBlobClient(key);
        BlobParallelUploadOptions options = new BlobParallelUploadOptions(BinaryData.fromBytes(content))
                .setHeaders(new BlobHttpHeaders().setContentType(contentType));
        blobClient.uploadWithResponse(options, null, null);
    }

    @Override
    public byte[] download(String key) {
        BlobClient blobClient = getContainerClient(key).getBlobClient(key);
        return blobClient.downloadContent().toBytes();
    }

    @Override
    public List<String> findOversizedFiles(long maxSizeBytes) {
        List<String> oversizedKeys = new ArrayList<>();
        collectOversizedFiles(publicContainerClient, maxSizeBytes, oversizedKeys);
        collectOversizedFiles(privateContainerClient, maxSizeBytes, oversizedKeys);
        return oversizedKeys;
    }

    private static final Pattern PROTECTED_REPORT_KEY_PATTERN =
            Pattern.compile("^reports/(pdf/\\d+\\.pdf|excel/\\d+\\.xlsx)$");

    private void collectOversizedFiles(BlobContainerClient containerClient, long maxSizeBytes, List<String> target) {
        for (BlobItem blobItem : containerClient.listBlobs()) {
            String key = blobItem.getName();
            if (isProtectedReportKey(key)) {
                continue;
            }
            Long contentLength = blobItem.getProperties().getContentLength();
            if (contentLength != null && contentLength > maxSizeBytes) {
                target.add(key);
            }
        }
    }

    private boolean isProtectedReportKey(String key) {
        return PROTECTED_REPORT_KEY_PATTERN.matcher(key).matches();
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
