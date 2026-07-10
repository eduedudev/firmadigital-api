package ec.gob.defensoria.signer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request para validar y analizar un certificado digital
 */
@Data
public class ValidateCertificateRequest {
    
    /**
     * Certificado .p12 en Base64
     */
    @NotBlank(message = "El certificate es requerido")
    private String certificate;
    
    /**
     * Contraseña del certificate
     */
    @NotBlank(message = "La contraseña es requerida")
    private String password;
}
