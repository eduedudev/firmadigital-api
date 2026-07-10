package ec.gob.defensoria.signer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request para listar aliases de un certificate .p12
 */
@Data
public class ListCertificatesRequest {
    
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
