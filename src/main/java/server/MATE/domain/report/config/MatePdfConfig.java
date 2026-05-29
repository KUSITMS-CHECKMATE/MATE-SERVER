package server.MATE.domain.report.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
<<<<<<< HEAD
<<<<<<< HEAD
=======
import org.springframework.web.reactive.function.client.ExchangeStrategies;
>>>>>>> origin/dev
=======
import org.springframework.web.reactive.function.client.ExchangeStrategies;
>>>>>>> origin/feat/ci
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(MatePdfProperties.class)
public class MatePdfConfig {
<<<<<<< HEAD
=======

>>>>>>> origin/feat/ci
    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10MB

    @Bean
    @Qualifier("matePdfWebClient")
    public WebClient matePdfWebClient(WebClient.Builder builder, MatePdfProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.timeout());
<<<<<<< HEAD
=======

>>>>>>> origin/feat/ci
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();

        return builder
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}
