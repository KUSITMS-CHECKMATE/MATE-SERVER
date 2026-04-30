package server.MATE.global.common;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.ErrorCode;
import server.MATE.toss.exception.TossErrorCode;

@Controller
public class ErrorCodeViewController {

    @GetMapping("/docs/error-codes")
    public String errorCodesPage(Model model) {
        List<ErrorCode> errorCodes = List.of(
                BaseErrorCode.values(),
                TossErrorCode.values()
        ).stream()
                .flatMap(Arrays::stream)
                .map(ErrorCode.class::cast)
                .sorted(Comparator.comparing(ErrorCode::getCode))
                .toList();

        Map<String, List<ErrorCode>> groupedErrorCodes = errorCodes.stream()
                .collect(Collectors.groupingBy(
                        errorCode -> extractDomain(errorCode.getCode()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        model.addAttribute("groupedErrorCodes", groupedErrorCodes);
        model.addAttribute("totalCount", errorCodes.size());
        return "docs/error-codes";
    }

    private String extractDomain(String code) {
        int delimiterIndex = code.indexOf('_');
        if (delimiterIndex <= 0) {
            return "UNCLASSIFIED";
        }
        return code.substring(0, delimiterIndex);
    }
}
