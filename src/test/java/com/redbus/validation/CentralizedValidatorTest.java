package com.redbus.validation;

import com.redbus.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CentralizedValidatorTest {

    private CentralizedValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CentralizedValidator();
    }

    // ── Name ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Name Validation")
    class NameTests {
        @Test void validName()                   { assertDoesNotThrow(() -> validator.validateName("Ramesh Kumar")); }
        @Test void nullName()                    { assertThrows(ValidationException.class, () -> validator.validateName(null)); }
        @Test void specialCharsInName()          { assertThrows(ValidationException.class, () -> validator.validateName("Ram@esh")); }
        @Test void consecutiveCharsInName()      { assertThrows(ValidationException.class, () -> validator.validateName("Raaaamesh")); }
        @Test void numbersInName()               { assertThrows(ValidationException.class, () -> validator.validateName("Ram123")); }
    }

    // ── Mobile ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Mobile Number Validation")
    class MobileTests {
        @Test void validMobile()                 { assertDoesNotThrow(() -> validator.validateMobileNumber("9876543210")); }
        @Test void lessThan10Digits()            { assertThrows(ValidationException.class, () -> validator.validateMobileNumber("98765")); }
        @Test void moreThan10Digits()            { assertThrows(ValidationException.class, () -> validator.validateMobileNumber("98765432101")); }
        @Test void invalidFirstDigit()           { assertThrows(ValidationException.class, () -> validator.validateMobileNumber("1234567890")); }
        @Test void lettersInMobile()             { assertThrows(ValidationException.class, () -> validator.validateMobileNumber("9876abc210")); }
        @Test void validStartWith6()             { assertDoesNotThrow(() -> validator.validateMobileNumber("6876543210")); }
        @Test void validStartWith7()             { assertDoesNotThrow(() -> validator.validateMobileNumber("7876543210")); }
        @Test void validStartWith8()             { assertDoesNotThrow(() -> validator.validateMobileNumber("8876543210")); }
    }

    // ── DOB ───────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Date of Birth Validation")
    class DobTests {
        @Test void validDob()                    { assertDoesNotThrow(() -> validator.validateAndParseDob("01011990")); }
        @Test void invalidFormat()               { assertThrows(ValidationException.class, () -> validator.validateAndParseDob("1990-01-01")); }
        @Test void invalidMonth()                { assertThrows(ValidationException.class, () -> validator.validateAndParseDob("01132000")); }
        @Test void invalidDay()                  { assertThrows(ValidationException.class, () -> validator.validateAndParseDob("32012000")); }
        @Test void before1901()                  { assertThrows(ValidationException.class, () -> validator.validateAndParseDob("01011800")); }
        @Test void under18()                     { assertThrows(ValidationException.class, () -> validator.validateAndParseDob("01012015")); }
        @Test void feb29NonLeapYear()            { assertThrows(ValidationException.class, () -> validator.validateAndParseDob("29022001")); }
    }

    // ── Email ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Email Validation")
    class EmailTests {
        @Test void validEmail()                  { assertDoesNotThrow(() -> validator.validateEmail("ramesh@gmail.com")); }
        @Test void emailWithUnderscore()         { assertDoesNotThrow(() -> validator.validateEmail("ram_esh@yahoo.com")); }
        @Test void emailWithDot()               { assertDoesNotThrow(() -> validator.validateEmail("ram.esh@outlook.com")); }
        @Test void missingAt()                   { assertThrows(ValidationException.class, () -> validator.validateEmail("rameshgmail.com")); }
        @Test void emailWithSpaces()             { assertThrows(ValidationException.class, () -> validator.validateEmail("ram esh@gmail.com")); }
        @Test void firstCharNotLetter()          { assertThrows(ValidationException.class, () -> validator.validateEmail("1ramesh@gmail.com")); }
        @Test void invalidDomain()               { assertThrows(ValidationException.class, () -> validator.validateEmail("ramesh@gmail")); }
        @Test void specialCharAfterAt()          { assertThrows(ValidationException.class, () -> validator.validateEmail("ramesh@gm#ail.com")); }
    }

    // ── Address ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Address Validation")
    class AddressTests {
        @Test void validAddress()                { assertDoesNotThrow(() -> validator.validateAddress("123 MG Road Bengaluru")); }
        @Test void nullAddress()                 { assertThrows(ValidationException.class, () -> validator.validateAddress(null)); }
        @Test void fiveConsecutiveChars()        { assertThrows(ValidationException.class, () -> validator.validateAddress("123 aaaaa Road")); }
        @Test void fourConsecutiveCharsOk()      { assertDoesNotThrow(() -> validator.validateAddress("123 aaaa Road")); }
    }

    // ── Password ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Password Validation")
    class PasswordTests {
        @Test void validPassword()               { assertDoesNotThrow(() -> validator.validatePassword("Ram@1234")); }
        @Test void tooShort()                    { assertThrows(ValidationException.class, () -> validator.validatePassword("Ra@1")); }
        @Test void noUppercase()                 { assertThrows(ValidationException.class, () -> validator.validatePassword("ram@1234")); }
        @Test void noLowercase()                 { assertThrows(ValidationException.class, () -> validator.validatePassword("RAM@1234")); }
        @Test void noDigit()                     { assertThrows(ValidationException.class, () -> validator.validatePassword("Ram@abcd")); }
        @Test void noSpecialChar()               { assertThrows(ValidationException.class, () -> validator.validatePassword("Ram12345")); }
        @Test void hasSpaces()                   { assertThrows(ValidationException.class, () -> validator.validatePassword("Ram @1234")); }
        @Test void isNullWord()                  { assertThrows(ValidationException.class, () -> validator.validatePassword("NULL")); }
        @Test void consecutiveChars()            { assertThrows(ValidationException.class, () -> validator.validatePassword("Raaaa@1234")); }
    }
}
