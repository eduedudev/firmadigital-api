package ec.gob.defensoria.signer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response de document firmado
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignResponse {
    
    /**
     * Documento firmado en Base64
     */
    private String signedDocument;
    
    /**
     * Nombre sugerido para el archivo
     */
    private String suggestedName;
    
    /**
     * Timestamp de la firma
     */
    private LocalDateTime timestamp;
    
    /**
     * Información del certificate usado
     */
    private CertificateInfo certificate;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateInfo {
        private String fullName;
        private String cedula;
        private String certifyingAuthority;
        private String validFrom;
        private String validUntil;
    }
}
