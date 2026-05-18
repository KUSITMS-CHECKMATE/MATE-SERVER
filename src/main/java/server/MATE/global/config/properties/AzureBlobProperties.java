package server.MATE.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "azure.storage")
public class AzureBlobProperties {
    private String connectionString;
    private String containerName;
    private int uploadSasExpiryMinutes = 10;
    private int downloadSasExpiryMinutes = 30;
}
