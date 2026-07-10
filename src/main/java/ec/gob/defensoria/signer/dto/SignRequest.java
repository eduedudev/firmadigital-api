package ec.gob.defensoria.signer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request para sign un document digitalmente
 */
@Data
public class SignRequest {
    
    /**
     * Documento en Base64 (PDF, DOCX, XLSX, etc)
     */
    @NotBlank(message = "El document es requerido")
    private String document;
    
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
    
    /**
     * Nombre del documento original (opcional, para generar nombre del firmado)
     */
    private String documentName;
    
    /**
     * Configuración de firma (opcional para PDFs)
     */
    private SignatureConfig configuration;
    
    @Data
    public static class SignatureConfig {
        /**
         * Tipo de firma: true = con QR visible, false = solo criptográfica invisible
         */
        private Boolean visibleSignature = true;
        
        /**
         * Número de página donde insertar firma visible (-1 para última)
         */
        private Integer page = 1;
        
        /**
         * Posición de la firma en el PDF
         */
        private Position position;
        
        /**
         * Razón de la firma
         */
        private String reason = "Firmado digitalmente";
        
        /**
         * Ubicación de la firma
         */
        private String location = "Ecuador";
        
        /**
         * Información adicional para el QR (opcional)
         */
        private String infoQR = "";
    }
    
    @Data
    public static class Position {
        private Integer x = 10;
        private Integer y = 830;
        private Integer width = 200;
        private Integer height = 50;
    }
}
