package server.MATE.global.storage.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class AzureStorageConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String connectionString = context.getEnvironment().getProperty("azure.storage.connection-string");
        String publicContainerName = context.getEnvironment().getProperty("azure.storage.public-container-name");
        String privateContainerName = context.getEnvironment().getProperty("azure.storage.private-container-name");

        return StringUtils.hasText(connectionString)
                && StringUtils.hasText(publicContainerName)
                && StringUtils.hasText(privateContainerName);
    }
}
