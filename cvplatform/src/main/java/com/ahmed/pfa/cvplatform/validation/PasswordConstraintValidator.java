package com.ahmed.pfa.cvplatform.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Validateur de contraintes pour les mots de passe
 *
 * Implémente les règles de sécurité suivantes:
 * - Longueur entre 8 et 128 caractères
 * - Au moins une lettre majuscule
 * - Au moins une lettre minuscule
 * - Au moins un chiffre
 * - Au moins un caractère spécial parmi: @$!%*?&#
 *
 * Messages d'erreur spécifiques pour chaque règle violée
 *
 * @author Ahmed
 */
public class PasswordConstraintValidator
        implements ConstraintValidator<ValidPassword, String> {

    private static final Logger logger = LoggerFactory.getLogger(PasswordConstraintValidator.class);

    // Constantes de longueur
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    // Patterns de validation
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[@$!%*?&#]");

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // Initialisation si nécessaire (actuellement vide)
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Null check
        if (password == null) {
            buildViolation(context, "Le mot de passe ne peut pas être vide");
            return false;
        }

        // Désactiver le message par défaut pour utiliser des messages spécifiques
        context.disableDefaultConstraintViolation();

        // Validation de la longueur minimale
        if (password.length() < MIN_LENGTH) {
            String message = String.format(
                    "Le mot de passe doit contenir au moins %d caractères",
                    MIN_LENGTH
            );
            buildViolation(context, message);
            logger.debug("Validation échouée: longueur minimale ({} < {})",
                    password.length(), MIN_LENGTH);
            return false;
        }

        // Validation de la longueur maximale
        if (password.length() > MAX_LENGTH) {
            String message = String.format(
                    "Le mot de passe ne peut pas dépasser %d caractères",
                    MAX_LENGTH
            );
            buildViolation(context, message);
            logger.debug("Validation échouée: longueur maximale ({} > {})",
                    password.length(), MAX_LENGTH);
            return false;
        }

        // Validation de la présence d'une majuscule
        if (!UPPERCASE.matcher(password).find()) {
            buildViolation(context, "Le mot de passe doit contenir au moins une lettre majuscule (A-Z)");
            logger.debug("Validation échouée: pas de majuscule");
            return false;
        }

        // Validation de la présence d'une minuscule
        if (!LOWERCASE.matcher(password).find()) {
            buildViolation(context, "Le mot de passe doit contenir au moins une lettre minuscule (a-z)");
            logger.debug("Validation échouée: pas de minuscule");
            return false;
        }

        // Validation de la présence d'un chiffre
        if (!DIGIT.matcher(password).find()) {
            buildViolation(context, "Le mot de passe doit contenir au moins un chiffre (0-9)");
            logger.debug("Validation échouée: pas de chiffre");
            return false;
        }

        // Validation de la présence d'un caractère spécial
        if (!SPECIAL.matcher(password).find()) {
            buildViolation(context, "Le mot de passe doit contenir au moins un caractère spécial (@$!%*?&#)");
            logger.debug("Validation échouée: pas de caractère spécial");
            return false;
        }

        // Toutes les validations passées
        logger.debug("Mot de passe valide: toutes les contraintes respectées");
        return true;
    }

    /**
     * Helper pour construire une violation de contrainte avec message personnalisé
     */
    private void buildViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}