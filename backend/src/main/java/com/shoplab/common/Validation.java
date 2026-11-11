package com.shoplab.common;

/**
 * Validation rules shared by several request classes. They copy the frontend's rules
 * (frontend/index.html) so the UI and the API accept and reject exactly the same input.
 */
public final class Validation {

    private Validation() {
    }

    /** Same as the frontend's EMAIL_RE. An empty value is allowed here; @NotBlank reports that case. */
    public static final String EMAIL = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$";

    /** At least 8 characters, one uppercase letter, one digit (empty allowed, see above). */
    public static final String PASSWORD = "^$|^(?=.*[A-Z])(?=.*\\d).{8,}$";
}
