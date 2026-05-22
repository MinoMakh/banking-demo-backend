package com.banking.backend.transfer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FxService {

    private static final int SCALE = 4;
    private static final RoundingMode ROUND = RoundingMode.HALF_UP;

    private final BigDecimal usdToIls;

    public FxService(@Value("${app.fx.usd-to-ils}") BigDecimal usdToIls) {
        this.usdToIls = usdToIls;
    }

    public BigDecimal convert(BigDecimal amount, String fromCcy, String toCcy) {
        if (fromCcy.equalsIgnoreCase(toCcy)) {
            return amount;
        }
        if ("USD".equalsIgnoreCase(fromCcy) && "ILS".equalsIgnoreCase(toCcy)) {
            return amount.multiply(usdToIls).setScale(SCALE, ROUND);
        }
        if ("ILS".equalsIgnoreCase(fromCcy) && "USD".equalsIgnoreCase(toCcy)) {
            return amount.divide(usdToIls, SCALE, ROUND);
        }
        throw new IllegalArgumentException("Unsupported currency pair: " + fromCcy + " -> " + toCcy);
    }
}
