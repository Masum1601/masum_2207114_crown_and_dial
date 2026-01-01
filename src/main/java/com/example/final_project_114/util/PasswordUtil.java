package com.example.final_project_114.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    private static final int BCRYPT_ROUNDS = 12;

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            return plainPassword.equals(hashedPassword);
        }
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit;
    }

    public static String getPasswordStrengthMessage(String password) {
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters long";
        }
        if (password.length() < 8) {
            return "Password should be at least 8 characters for better security";
        }
        if (!isStrongPassword(password)) {
            return "Password should contain uppercase, lowercase, and numbers";
        }
        return "Password strength: Strong";
    }
}
