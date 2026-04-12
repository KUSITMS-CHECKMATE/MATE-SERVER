package server.MATE.global.config;

import java.net.http.HttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import server.MATE.global.config.properties.TossApiProperties;

@Configuration
@EnableConfigurationProperties(TossApiProperties.class)
public class TossApiConfig {

    @Bean
    @ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
    RestClient tossRestClient(RestClient.Builder builder, SslBundles sslBundles, TossApiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout().connect())
                .sslContext(sslBundles.getBundle("toss").createSslContext())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout().read());

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
