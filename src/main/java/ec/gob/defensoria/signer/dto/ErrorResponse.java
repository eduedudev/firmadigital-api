package ec.gob.defensoria.signer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response estandarizado para errores
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Timestamp del error
     */
    private LocalDateTime timestamp;
    
    /**
     * Código HTTP del error
     */
    private Integer status;
    
    /**
     * Mensaje del error
     */
    private String error;
    
    /**
     * Mensaje detallado
     */
    private String mensaje;
    
    /**
     * Ruta del endpoint
     */
    private String path;
    
    /**
     * Errores de validación (si aplica)
     */
    private List<String> erroresValidacion;
    
    public ErrorResponse(LocalDateTime timestamp, Integer status, String error, String mensaje, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.path = path;
    }
}
