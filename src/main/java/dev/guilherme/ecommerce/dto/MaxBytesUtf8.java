package dev.guilherme.ecommerce.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * Limite em bytes UTF-8, não em caracteres. Existe por causa do BCrypt, que recusa senha
 * acima de 72 bytes: "ã" conta 2, então 40 caracteres acentuados já estouram.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxBytesUtf8.Validador.class)
public @interface MaxBytesUtf8 {

    int value();

    String message() default "texto longo demais";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<MaxBytesUtf8, String> {

        private int max;

        @Override
        public void initialize(MaxBytesUtf8 anotacao) {
            this.max = anotacao.value();
        }

        @Override
        public boolean isValid(String valor, ConstraintValidatorContext context) {
            return valor == null || valor.getBytes(StandardCharsets.UTF_8).length <= max;
        }
    }
}
