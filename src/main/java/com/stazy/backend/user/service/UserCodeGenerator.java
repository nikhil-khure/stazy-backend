package com.stazy.backend.user.service;

import com.stazy.backend.common.enums.RoleName;
import org.springframework.stereotype.Component;

@Component
public class UserCodeGenerator {

    public String generate(RoleName roleName, String displayName, long sequence) {
        String prefix = switch (roleName) {
            case STUDENT -> "STU";
            case OWNER -> "OWN";
            case ADMIN -> "ADM";
            case SUPER_ADMIN -> "SUP";
        };
        String letters = displayName == null ? "USR" : displayName.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (letters.length() < 3) {
            letters = (letters + "USR").substring(0, 3);
        } else {
            letters = letters.substring(0, 3);
        }
        return "%s-%s-%06d".formatted(prefix, letters, sequence);
    }
}
