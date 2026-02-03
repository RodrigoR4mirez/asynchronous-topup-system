package pe.com.topup.gateway.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import pe.com.topup.gateway.dto.ErrorResponse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exception Mapper for Jakarta Bean Validation exceptions.
 * Converts ConstraintViolationException to a standard ErrorResponse with 400
 * Bad Request.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        List<String> details = violations.stream()
                .map(violation -> {
                    String fieldName = "";
                    if (violation.getPropertyPath() != null) {
                        fieldName = violation.getPropertyPath().toString();
                        // Optional: clean up method parameter prefixes if needed, but keeping it simple
                        // is usually fine or preferred for debugging
                    }
                    return fieldName + ": " + violation.getMessage();
                })
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "Input validation failed",
                details);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
    }
}
