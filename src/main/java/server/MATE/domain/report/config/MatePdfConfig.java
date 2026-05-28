package server.MATE.domain.report.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(MatePdfProperties.class)
public class MatePdfConfig {

    @Bean
    @Qualifier("matePdfWebClient")
    public WebClient matePdfWebClient(WebClient.Builder builder, MatePdfProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.timeout());

        return builder
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
