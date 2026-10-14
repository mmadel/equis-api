package com.equestrian.equis.common.error;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;
import java.util.Optional;

// Lets a service map a unique-key clash to its own 409, by the constraint's name in the migration.
public final class Constraints {
    private Constraints() {}

    // The database constraint name behind the exception, lower-cased, if Hibernate reported one.
    public static Optional<String> nameOf(DataIntegrityViolationException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException c && c.getConstraintName() != null) {
                return Optional.of(c.getConstraintName().toLowerCase(Locale.ROOT));
            }
        }
        return Optional.empty();
    }
}
