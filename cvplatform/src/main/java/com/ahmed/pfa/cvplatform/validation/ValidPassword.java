package com.ahmed.pfa.cvplatform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Annotation de validation pour les mots de passe
 *
 * Règles appliquées:
 * - Minimum 8 caractères
 * - Maximum 128 caractères
 * - Au moins 1 majuscule (A-Z)
 * - Au moins 1 minuscule (a-z)
 * - Au moins 1 chiffre (0-9)
 * - Au moins 1 caractère spécial (@$!%*?&#)
 *
 * Usage:
 * <pre>
 * {@code
 * @ValidPassword
 * private String motDePasse;
 * }
 * </pre>
 *
 * @author Ahmed
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Documented
public @interface ValidPassword {

    /**
     * Message d'erreur par défaut
     */
    String message() default "Le mot de passe ne respecte pas les critères de sécurité";

    /**
     * Groupes de validation
     */
    Class<?>[] groups() default {};

    /**
     * Payload pour métadonnées
     */
    Class<? extends Payload>[] payload() default {};
}