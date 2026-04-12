package server.MATE.global.common;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import server.MATE.global.common.exception.ErrorCode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ErrorCodeViewController {

    @GetMapping("/docs/error-codes")
    public String errorCodesPage(Model model) {
        Map<String, List<ErrorCode>> groupedErrorCodes = Arrays.stream(ErrorCode.values())
                .sorted(Comparator.comparing(ErrorCode::getCode))
                .collect(Collectors.groupingBy(
                        errorCode -> extractDomain(errorCode.getCode()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        model.addAttribute("groupedErrorCodes", groupedErrorCodes);
        model.addAttribute("totalCount", ErrorCode.values().length);
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
