package pe.com.topup.gateway.dto;

import java.util.List;

/**
 * Data Transfer Object for Error Responses.
 */
public class ErrorResponse {

    private String code;
    private String message;
    private List<String> details;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message, List<String> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
