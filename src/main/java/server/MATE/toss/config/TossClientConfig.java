package server.MATE.toss.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties({TossProperties.class, TossCryptoProperties.class})
public class TossClientConfig {

    @Bean
    @Qualifier("tossWebClient")
    @ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
    public WebClient tossWebClient(
            WebClient.Builder builder,
            SslBundles sslBundles,
            TossProperties properties
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(properties.timeout().connect().toMillis()))
                .responseTimeout(properties.timeout().read());

        if (properties.ssl().enabled()) {
            JdkSslContext sslContext = new JdkSslContext(
                    sslBundles.getBundle(properties.ssl().bundle()).createSslContext(),
                    true,
                    ClientAuth.NONE
            );
            httpClient = httpClient.secure(sslSpec -> sslSpec.sslContext(sslContext));
        }

        return builder
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
