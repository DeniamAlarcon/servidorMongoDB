package services;

public class Respuestas {
    private String status;      // "success" o "error"
    private String errorCode;   // Código de error (si aplica)
    private String message;     // Mensaje informativo
    private Object data;        // Datos en respuestas exitosas
    private int httpCode;       // Código HTTP

    // 🔴 Constructor para respuestas de error
    public Respuestas(String status, String errorCode, String message, int httpCode) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.httpCode = httpCode;
        this.data = null;  // No hay datos en errores
    }

    // ✅ Constructor para respuestas de éxito
    public Respuestas(String status, String message, int httpCode) {
        this.status = status;
        this.errorCode = null; // No hay código de error en éxito
        this.message = message;
        this.httpCode = httpCode;
    }

    // Getters y Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public int getHttpCode() { return httpCode; }
    public void setHttpCode(int httpCode) { this.httpCode = httpCode; }
}
