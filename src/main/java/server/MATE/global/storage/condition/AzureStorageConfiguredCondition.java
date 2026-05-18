package server.MATE.global.storage.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class AzureStorageConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String connectionString = context.getEnvironment().getProperty("azure.storage.connection-string");
        String containerName = context.getEnvironment().getProperty("azure.storage.container-name");

        return StringUtils.hasText(connectionString) && StringUtils.hasText(containerName);
    }
}
