package pe.com.topup.gateway.infrastructure.adapter.in.web.errorhandler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import pe.com.topup.gateway.infrastructure.adapter.in.web.dto.ErrorResponse;

import java.util.List;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

        @Override
        public Response toResponse(ConstraintViolationException exception) {
                List<String> details = exception.getConstraintViolations().stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.toList());

                ErrorResponse errorResponse = new ErrorResponse();
                errorResponse.setCode("400");
                errorResponse.setMessage("Validation Error");
                errorResponse.setDetails(details);

                return Response.status(Response.Status.BAD_REQUEST)
                                .entity(errorResponse)
                                .build();
        }
}
