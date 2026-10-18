package com.recipebook.repository;

import com.recipebook.model.Emails;
import com.recipebook.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByOidcSubject(String oidcSubject);

    /** Adressen sind klein gespeichert (V31); die Eingabe wird genauso normalisiert. */
    default Optional<User> findByAddress(String email) {
        String normalized = Emails.normalize(email);
        return normalized == null || normalized.isEmpty() ? Optional.empty() : findByEmail(normalized);
    }
}
