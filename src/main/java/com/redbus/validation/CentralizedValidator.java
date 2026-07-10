package com.redbus.validation;

import com.redbus.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

/**
 * CENTRALIZED VALIDATOR
 * All validation logic lives here. Never scatter validation in controllers or services.
 * If a rule changes in the future, edit only this class.
 */
@Component
public class CentralizedValidator {

    // ── Name ──────────────────────────────────────────────────────────────────

    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("check your name once");
        }
        // Only [A-Za-z] and single spaces allowed
        if (!name.matches("[A-Za-z]+(\\s[A-Za-z]+)*")) {
            throw new ValidationException("check your name once");
        }
        // No 3 or more consecutive identical characters (case-insensitive)
        if (hasConsecutiveChars(name.toLowerCase(), 3)) {
            throw new ValidationException("check your name once");
        }
    }

    // ── Mobile Number ─────────────────────────────────────────────────────────

    public void validateMobileNumber(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            throw new ValidationException("Mobile Number Must 10 digits only");
        }
        if (!mobile.matches("\\d+")) {
            throw new ValidationException("Invalid Mobile number entered");
        }
        if (mobile.length() != 10) {
            throw new ValidationException("Mobile Number Must 10 digits only");
        }
        char firstDigit = mobile.charAt(0);
        if (firstDigit != '6' && firstDigit != '7' && firstDigit != '8' && firstDigit != '9') {
            throw new ValidationException("Invalid Mobile number entered");
        }
    }

    // ── Date of Birth ─────────────────────────────────────────────────────────

    public LocalDate validateAndParseDob(String dob) {
        if (dob == null || dob.isBlank()) {
            throw new ValidationException("Invalid DOB");
        }
        if (!dob.matches("\\d{8}")) {
            throw new ValidationException("Invalid DOB");
        }

        String day   = dob.substring(0, 2);
        String month = dob.substring(2, 4);
        String year  = dob.substring(4, 8);

        int yyyy = Integer.parseInt(year);
        if (yyyy < 1901) {
            throw new ValidationException("Invalid DOB");
        }

        LocalDate parsed;
        try {
            parsed = LocalDate.parse(day + "/" + month + "/" + year,
                                     DateTimeFormatter.ofPattern("dd/MM/uuuu")
                                                      .withResolverStyle(ResolverStyle.STRICT));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid DOB");
        }

        if (Period.between(parsed, LocalDate.now()).getYears() < 18) {
            throw new ValidationException("Invalid DOB");
        }

        return parsed;
    }

    // ── Email ─────────────────────────────────────────────────────────────────

    public void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Invalid Email");
        }
        // No spaces
        if (email.contains(" ")) {
            throw new ValidationException("Invalid Email");
        }
        // First char must be a letter
        if (!Character.isLetter(email.charAt(0))) {
            throw new ValidationException("Invalid Email");
        }
        // Must contain exactly one '@'
        long atCount = email.chars().filter(c -> c == '@').count();
        if (atCount != 1) {
            throw new ValidationException("Invalid Email");
        }

        String[] parts = email.split("@");
        String localPart  = parts[0];
        String domainPart = parts[1];

        // Local part: only letters, digits, '_', '.'
        if (!localPart.matches("[A-Za-z0-9._]+")) {
            throw new ValidationException("Invalid Email");
        }
        // No special chars other than '_' and '.' in local part
        if (!localPart.matches("[A-Za-z0-9_.]+")) {
            throw new ValidationException("Invalid Email");
        }

        // Domain part: companyname.tld (e.g. gmail.com, yahoo.com)
        if (!domainPart.matches("[A-Za-z0-9]+\\.[A-Za-z]{2,}")) {
            throw new ValidationException("Invalid Email");
        }
    }

    // ── Address ───────────────────────────────────────────────────────────────

    public void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new ValidationException("Check your address once");
        }
        // No 5 or more consecutive identical letters
        if (hasConsecutiveChars(address.toLowerCase(), 5)) {
            throw new ValidationException("Check your address once");
        }
    }

    // ── Password (Global) ─────────────────────────────────────────────────────

    public void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Invalid Password");
        }
        // No spaces
        if (password.contains(" ")) {
            throw new ValidationException("Invalid Password");
        }
        // Literal "null" word not allowed (any case)
        if (password.equalsIgnoreCase("NULL")) {
            throw new ValidationException("Invalid Password");
        }
        // Minimum 8 characters
        if (password.length() < 8) {
            throw new ValidationException("Invalid Password");
        }
        // At least 1 uppercase
        if (!password.matches(".*[A-Z].*")) {
            throw new ValidationException("Invalid Password");
        }
        // At least 1 lowercase
        if (!password.matches(".*[a-z].*")) {
            throw new ValidationException("Invalid Password");
        }
        // At least 1 digit
        if (!password.matches(".*[0-9].*")) {
            throw new ValidationException("Invalid Password");
        }
        // At least 1 special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new ValidationException("Invalid Password");
        }
        // No 3 or more consecutive identical letters or digits
        if (hasConsecutiveChars(password.toLowerCase(), 3)) {
            throw new ValidationException("Invalid Password");
        }
    }

    // ── Age ───────────────────────────────────────────────────────────────────

    public void validateAge(Integer age) {
        if (age == null || age <= 0) {
            throw new ValidationException("Invalid Age");
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Returns true if the string has `count` or more consecutive identical characters.
     */
    private boolean hasConsecutiveChars(String s, int count) {
        if (s == null || s.length() < count) return false;
        int consecutive = 1;
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == s.charAt(i - 1)) {
                consecutive++;
                if (consecutive >= count) return true;
            } else {
                consecutive = 1;
            }
        }
        return false;
    }
}
