package server.MATE.toss.exception.parser;

import org.springframework.http.HttpStatusCode;

public interface TossErrorResponseParser {

    boolean supports(HttpStatusCode statusCode, String path, String responseBody);

    TossErrorContext parse(HttpStatusCode statusCode, String path, String responseBody);
}
