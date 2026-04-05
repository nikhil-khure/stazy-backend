package com.stazy.backend.common.util;

import com.stazy.backend.common.exception.BadRequestException;
import java.util.regex.Pattern;

public final class PasswordRules {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordRules() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long.");
        }
        if (!UPPERCASE.matcher(password).find()) {
            throw new BadRequestException("Password must contain at least 1 uppercase letter.");
        }
        if (!DIGIT.matcher(password).find()) {
            throw new BadRequestException("Password must contain at least 1 number.");
        }
        if (!SPECIAL.matcher(password).find()) {
            throw new BadRequestException("Password must contain at least 1 special symbol.");
        }
    }
}
