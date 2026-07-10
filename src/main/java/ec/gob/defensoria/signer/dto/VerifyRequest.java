package ec.gob.defensoria.signer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request para verify signatures de un document
 */
@Data
public class VerifyRequest {
    
    /**
     * Documento firmado en Base64
     */
    @NotBlank(message = "El document es requerido")
    private String document;
}
