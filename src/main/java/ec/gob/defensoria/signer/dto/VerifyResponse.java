package ec.gob.defensoria.signer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response con información de signatures del document
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResponse {
    
    /**
     * Indica si el document tiene signatures válidas
     */
    private Boolean documentValid;
    
    /**
     * Lista de signatures encontradas
     */
    private List<SignatureInfo> signatures;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignatureInfo {
        /**
         * Nombre del signer
         */
        private String signer;
        
        /**
         * Cédula del signer
         */
        private String cedula;
        
        /**
         * Entidad certificadora
         */
        private String certifyingAuthority;
        
        /**
         * Fecha de la firma
         */
        private String signDate;
        
        /**
         * Certificado era válido al momento de sign
         */
        private Boolean validAtSignDate;
        
        /**
         * Certificado está revoked
         */
        private Boolean revoked;
        
        /**
         * Datos adicionales del signer
         */
        private SignerData datosCompletos;

        /**
         * Indica si la firma cuenta con sello de tiempo (TSA)
         */
        private Boolean hasTimestamp;

        /**
         * Fecha del sello de tiempo
         */
        private String timestampDate;

        /**
         * Entidad que emitió el sello de tiempo
         */
        private String timestampIssuedBy;

        /**
         * Nombre común (CN) de la TSA
         */
        private String timestampCN;

        /**
         * Indica si el sello de tiempo es válido
         */
        private Boolean timestampValid;

        /**
         * Mensaje descriptivo del sello de tiempo con formato legible
         */
        private String timestampMessage;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignerData {
        private String nombre;
        private String apellido;
        private String institucion;
        private String cargo;
        private String serialNumber;
    }
}
