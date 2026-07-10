package ec.gob.defensoria.signer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response con lista de certificados disponibles en el .p12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListCertificatesResponse {
    
    /**
     * Lista de certificados encontrados
     */
    private List<CertificateAlias> aliases;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateAlias {
        /**
         * Alias del certificate
         */
        private String alias;
        
        /**
         * Nombre común del certificate
         */
        private String nombre;
        
        /**
         * Cédula del titular
         */
        private String cedula;
        
        /**
         * Entidad certificadora
         */
        private String certifyingAuthority;
        
        /**
         * Fecha desde cuando es válido
         */
        private String validFrom;
        
        /**
         * Fecha hasta cuando es válido
         */
        private String validUntil;
        
        /**
         * Estado actual del certificate
         */
        private String currentStatus;
    }
}
