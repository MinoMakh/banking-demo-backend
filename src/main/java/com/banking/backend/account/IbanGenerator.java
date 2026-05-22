package com.banking.backend.account;

import java.math.BigInteger;

final class IbanGenerator {

    private static final String COUNTRY = "IL";
    private static final String BANK_CODE = "010";
    private static final String BRANCH_CODE = "001";

    private IbanGenerator() {}

    static String forAccountNo(String accountNo) {
        String digits = accountNo.replaceAll("\\D", "");
        String paddedAccount = String.format("%13s", digits).replace(' ', '0');
        String bban = BANK_CODE + BRANCH_CODE + paddedAccount;
        String rearranged = bban + lettersToDigits(COUNTRY) + "00";
        int check = 98 - new BigInteger(rearranged).mod(BigInteger.valueOf(97)).intValue();
        return COUNTRY + String.format("%02d", check) + bban;
    }

    private static String lettersToDigits(String s) {
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) {
            out.append(Character.isLetter(c) ? (Character.toUpperCase(c) - 'A' + 10) : c);
        }
        return out.toString();
    }
}
