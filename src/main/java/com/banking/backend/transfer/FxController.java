package com.banking.backend.transfer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/fx")
public class FxController {

    private final FxService fxService;

    public FxController(FxService fxService) {
        this.fxService = fxService;
    }

    public record FxQuote(String from, String to, BigDecimal amount, BigDecimal converted) {}

    @GetMapping("/quote")
    public ResponseEntity<FxQuote> quote(@RequestParam String from,
                                          @RequestParam String to,
                                          @RequestParam BigDecimal amount) {
        BigDecimal converted = fxService.convert(amount, from, to);
        return ResponseEntity.ok(new FxQuote(from.toUpperCase(), to.toUpperCase(), amount, converted));
    }
}
