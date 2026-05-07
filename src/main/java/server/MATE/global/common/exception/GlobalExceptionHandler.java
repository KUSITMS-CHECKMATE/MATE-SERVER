package server.MATE.global.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import server.MATE.global.common.response.ErrorResponse;
import server.MATE.global.discord.DiscordWebhookNotifier;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final DiscordWebhookNotifier discordWebhookNotifier;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            discordWebhookNotifier.notifyError(errorCode, errorCode.getHttpStatus(), e, request);
        }
        log.warn("BaseException: {}", e.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity
                .status(BaseErrorCode.COMMON_001.getHttpStatus())
                .body(ErrorResponse.of(BaseErrorCode.COMMON_001));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(BaseErrorCode.COMMON_003));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String field = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField())
                .findFirst()
                .orElse(null);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse(BaseErrorCode.COMMON_002.getMessage());
        log.warn("ValidationException: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(BaseErrorCode.COMMON_002, message, field));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        String field = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath().toString())
                .findFirst()
                .orElse(null);
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse(BaseErrorCode.COMMON_002.getMessage());
        log.warn("ConstraintViolationException: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(BaseErrorCode.COMMON_002, message, field));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String field = e.getParameterValidationResults().stream()
                .map(result -> result.getMethodParameter().getParameterName())
                .findFirst()
                .orElse(null);
        String message = e.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse(BaseErrorCode.COMMON_002.getMessage());
        log.warn("HandlerMethodValidationException: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(BaseErrorCode.COMMON_002, message, field));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity
                .status(BaseErrorCode.COMMON_006.getHttpStatus())
                .body(ErrorResponse.of(BaseErrorCode.COMMON_006));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("UnhandledException: {}", e.getMessage(), e);
        discordWebhookNotifier.notifyError(BaseErrorCode.COMMON_999, HttpStatus.INTERNAL_SERVER_ERROR, e, request);
        return ResponseEntity
                .status(BaseErrorCode.COMMON_999.getHttpStatus())
                .body(ErrorResponse.of(BaseErrorCode.COMMON_999));
    }
}
