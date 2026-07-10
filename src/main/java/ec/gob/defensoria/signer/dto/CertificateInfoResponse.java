package ec.gob.defensoria.signer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Response con información completa del certificado analizado
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateInfoResponse {
    
    /**
     * Información del propietario del certificado
     */
    private OwnerInfo owner;
    
    /**
     * Información del emisor (Autoridad Certificadora)
     */
    private IssuerInfo issuer;
    
    /**
     * Información de validez temporal
     */
    private ValidityInfo validity;
    
    /**
     * Información técnica del certificado
     */
    private TechnicalInfo technical;
    
    /**
     * Extensiones del certificado
     */
    private Map<String, String> extensions;
    
    /**
     * Usos permitidos de la clave
     */
    private List<String> keyUsages;
    
    /**
     * Propósitos extendidos
     */
    private List<String> extendedKeyUsages;
    
    /**
     * Estado actual del certificado
     */
    private String status;
    
    /**
     * Mensaje adicional sobre el estado
     */
    private String statusMessage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfo {
        private String commonName;
        private String serialNumber;
        private String organization;
        private String organizationalUnit;
        private String country;
        private String email;
        private String fullDN;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssuerInfo {
        private String commonName;
        private String organization;
        private String organizationalUnit;
        private String country;
        private String fullDN;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidityInfo {
        private String validFrom;
        private String validUntil;
        private boolean isCurrentlyValid;
        private boolean isExpired;
        private Long daysUntilExpiry;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicalInfo {
        private String version;
        private String serialNumber;
        private String signatureAlgorithm;
        private String publicKeyAlgorithm;
        private Integer publicKeySize;
        private String fingerprint;
    }
}
