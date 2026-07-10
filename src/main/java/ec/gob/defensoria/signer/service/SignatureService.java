package ec.gob.defensoria.signer.service;

import ec.gob.defensoria.signer.dto.*;
import ec.gob.firmadigital.libreria.certificate.CertEcUtils;
import ec.gob.firmadigital.libreria.certificate.to.Certificado;
import ec.gob.firmadigital.libreria.certificate.to.DatosUsuario;
import ec.gob.firmadigital.libreria.certificate.to.Documento;
import ec.gob.firmadigital.libreria.sign.DigestAlgorithm;
import ec.gob.firmadigital.libreria.sign.SignInfo;
import ec.gob.firmadigital.libreria.sign.pdf.BasePdfSigner;
import ec.gob.firmadigital.libreria.sign.pdf.PadesEnhancedSigner;
import ec.gob.firmadigital.libreria.sign.PrivateKeySigner;
import ec.gob.firmadigital.libreria.utils.Utils;
import com.itextpdf.kernel.pdf.PdfReader;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para firma y verificación digital de documentos
 */
@Slf4j
@Service
public class SignatureService {
    
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Firma un document digitalmente
     */
    public SignResponse sign(SignRequest request) {
        try {
            log.info("Iniciando proceso de firma digital");
            
            // 1. Decodificar Base64
            byte[] documentoBytes = Base64.getDecoder().decode(request.getDocument());
            byte[] certificadoBytes = Base64.getDecoder().decode(request.getCertificate());
            
            // 2. Cargar KeyStore
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certificadoBytes), request.getPassword().toCharArray());
            
            // 3. Auto-detectar primer certificado disponible
            Enumeration<String> aliases = keyStore.aliases();
            if (!aliases.hasMoreElements()) {
                throw new RuntimeException("No se encontraron certificados en el archivo .p12");
            }
            String alias = aliases.nextElement();
            log.info("Certificado detectado: {}", alias);
            
            // 4. Obtener clave privada y cadena de certificados
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, request.getPassword().toCharArray());
            Certificate[] certChain = keyStore.getCertificateChain(alias);
            X509Certificate cert = (X509Certificate) certChain[0];
            
            // 5. Configurar propiedades de firma
            Properties params = new Properties();
            if (request.getConfiguration() != null) {
                SignRequest.SignatureConfig config = request.getConfiguration();
                params.setProperty("signingReason", config.getReason());
                params.setProperty("signingLocation", config.getLocation());
                params.setProperty("signTime", DATE_FORMAT.format(new Date()));
                
                // Tipo de firma: QR visible o invisible
                if (config.getVisibleSignature() != null && config.getVisibleSignature()) {
                    params.setProperty("typeSignature", "QR");
                    if (config.getInfoQR() != null && !config.getInfoQR().isBlank()) {
                        params.setProperty("infoQR", config.getInfoQR());
                    }
                    
                    // Para QR, solo se usan las coordenadas lower-left
                    // La librería crea un rectángulo de 110x36 automáticamente
                    if (config.getPosition() != null) {
                        SignRequest.Position pos = config.getPosition();
                        params.setProperty("PositionOnPageLowerLeftX", String.valueOf(pos.getX()));
                        params.setProperty("PositionOnPageLowerLeftY", String.valueOf(pos.getY()));
                        log.info("Posición QR: x={}, y={}", pos.getX(), pos.getY());
                    }
                } else {
                    // Para firma sin QR, se usan las 4 coordenadas
                    if (config.getPosition() != null) {
                        SignRequest.Position pos = config.getPosition();
                        params.setProperty("PositionOnPageLowerLeftX", String.valueOf(pos.getX()));
                        params.setProperty("PositionOnPageLowerLeftY", String.valueOf(pos.getY()));
                        params.setProperty("PositionOnPageUpperRightX", String.valueOf(pos.getX() + pos.getWidth()));
                        params.setProperty("PositionOnPageUpperRightY", String.valueOf(pos.getY() + pos.getHeight()));
                    }
                }
                
                if (config.getPage() != null) {
                    params.setProperty("page", String.valueOf(config.getPage()));
                }
            }
            
            // 6. Cédula del certificado como identificacion para el TSA
            DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(cert);
            String cedula = datosUsuario != null ? datosUsuario.getCedula() : Utils.getUID(cert);
            params.setProperty("identificacion", cedula != null ? cedula : "");
            log.info("Firmando con sello de tiempo (cédula: {})", cedula);

            // 7. Firmar — PadesEnhancedSigner incluye el sello de tiempo (TSA) internamente
            PrivateKeySigner signer = new PrivateKeySigner(privateKey, DigestAlgorithm.SHA256);
            PadesEnhancedSigner enhancedSigner = new PadesEnhancedSigner(signer);
            byte[] signedDocument = enhancedSigner.sign(new ByteArrayInputStream(documentoBytes), privateKey, certChain, params);

            // 7. Preparar respuesta
            SignResponse response = new SignResponse();
            response.setSignedDocument(Base64.getEncoder().encodeToString(signedDocument));
            
            // Generar nombre del documento firmado
            String suggestedName;
            if (request.getDocumentName() != null && !request.getDocumentName().isBlank()) {
                // Si viene el nombre original, agregar -signed antes de la extensión
                String originalName = request.getDocumentName();
                int lastDot = originalName.lastIndexOf('.');
                if (lastDot > 0) {
                    String nameWithoutExt = originalName.substring(0, lastDot);
                    String extension = originalName.substring(lastDot);
                    suggestedName = nameWithoutExt + "-signed" + extension;
                } else {
                    suggestedName = originalName + "-signed.pdf";
                }
            } else {
                // Si no viene nombre, usar genérico con timestamp
                String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                suggestedName = "document-signed-" + timestamp + ".pdf";
            }
            response.setSuggestedName(suggestedName);
            response.setTimestamp(LocalDateTime.now());
            
            // Información del certificate
            SignResponse.CertificateInfo infoCert = new SignResponse.CertificateInfo();
            infoCert.setFullName(cert.getSubjectX500Principal().getName());
            infoCert.setCertifyingAuthority(cert.getIssuerX500Principal().getName());
            infoCert.setValidFrom(DATE_FORMAT.format(cert.getNotBefore()));
            infoCert.setValidUntil(DATE_FORMAT.format(cert.getNotAfter()));
            response.setCertificate(infoCert);
            
            log.info("Documento firmado exitosamente");
            return response;
            
        } catch (Exception e) {
            log.error("Error al sign document", e);
            throw new RuntimeException("Error al sign document: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verifica las signatures de un document
     */
    public VerifyResponse verify(VerifyRequest request) {
        // 1. Decodificar document
        byte[] documentoBytes = Base64.getDecoder().decode(request.getDocument());
        return verify(documentoBytes);
    }

    /**
     * Verifica las signatures de un document en bytes
     */
    public VerifyResponse verify(byte[] documentoBytes) {
        try {
            log.info("Iniciando verificación de signatures");

            // Obtener documento con info completa (incluyendo sello de tiempo).
            // Se usa el overload pdfToDocumento(PdfReader, signInfos) para evitar
            // el bug en Utils.pdfToDocumento(InputStream) que lee el stream dos veces.
            BasePdfSigner verifier = new BasePdfSigner();
            List<SignInfo> signInfos = verifier.getSigners(documentoBytes);
            PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(documentoBytes));
            Documento documento = Utils.pdfToDocumento(pdfReader, signInfos);

            List<Certificado> certificados = documento.getCertificados();

            if (certificados == null || certificados.isEmpty()) {
                VerifyResponse response = new VerifyResponse();
                response.setDocumentValid(false);
                response.setSignatures(Collections.emptyList());
                return response;
            }

            List<VerifyResponse.SignatureInfo> infoFirmas = certificados.stream().map(cert -> {
                VerifyResponse.SignatureInfo info = new VerifyResponse.SignatureInfo();

                // Datos del firmante
                if (cert.getDatosUsuario() != null) {
                    String nombre = cert.getDatosUsuario().getNombre() != null ? cert.getDatosUsuario().getNombre() : "";
                    String apellido = cert.getDatosUsuario().getApellido() != null ? cert.getDatosUsuario().getApellido() : "";
                    info.setSigner((nombre + " " + apellido).trim());
                    info.setCedula(cert.getDatosUsuario().getCedula());
                    info.setCertifyingAuthority(cert.getIssuedBy());

                    VerifyResponse.SignerData datosCompletos = new VerifyResponse.SignerData();
                    datosCompletos.setNombre(nombre);
                    datosCompletos.setApellido(apellido);
                    datosCompletos.setInstitucion(cert.getDatosUsuario().getInstitucion());
                    datosCompletos.setCargo(cert.getDatosUsuario().getCargo());
                    datosCompletos.setSerialNumber(cert.getSerial());
                    info.setDatosCompletos(datosCompletos);
                }

                // Fecha de firma
                if (cert.getSignGenerated() != null) {
                    info.setSignDate(DATE_FORMAT.format(cert.getSignGenerated().getTime()));
                }

                // Validez al momento de firma
                info.setValidAtSignDate(cert.getSignVerify() != null ? cert.getSignVerify() : false);
                info.setRevoked(false);

                // Sello de tiempo
                boolean tieneTimestamp = cert.getDocTimeStamp() != null;
                info.setHasTimestamp(tieneTimestamp);
                if (tieneTimestamp) {
                    String tsDate     = DATE_FORMAT.format(cert.getDocTimeStamp());
                    String tsIssuedBy = cert.getDocTimeStampIssuedBy();
                    String tsCN       = cert.getCnTimeStamp();
                    boolean tsValid   = cert.getDocValidTimeStamp() != null ? cert.getDocValidTimeStamp() : false;

                    info.setTimestampDate(tsDate);
                    info.setTimestampIssuedBy(tsIssuedBy);
                    info.setTimestampCN(tsCN);
                    info.setTimestampValid(tsValid);
                    info.setTimestampMessage(
                            tsDate + " " +
                            "hora de Ecuador " +
                            (tsCN != null ? tsCN : "") + " " +
                            "emitido por " + (tsIssuedBy != null ? tsIssuedBy : "")
                    );
                } else {
                    info.setHasTimestamp(false);
                    info.setTimestampValid(false);
                }

                return info;
            }).collect(Collectors.toList());

            boolean documentValid = documento.getSignValidate() != null ? documento.getSignValidate() : false;

            VerifyResponse response = new VerifyResponse();
            response.setDocumentValid(documentValid);
            response.setSignatures(infoFirmas);

            log.info("Verificación completada. Firmas encontradas: {}", infoFirmas.size());
            return response;

        } catch (Exception e) {
            log.error("Error al verify document", e);
            throw new RuntimeException("Error al verify document: " + e.getMessage(), e);
        }
    }
    
    /**
     * Valida y extrae información completa de un certificado digital
     */
    public CertificateInfoResponse validateCertificate(ValidateCertificateRequest request) {
        try {
            log.info("Analizando certificado digital");
            
            // 1. Decodificar y cargar KeyStore
            byte[] certificadoBytes = Base64.getDecoder().decode(request.getCertificate());
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certificadoBytes), request.getPassword().toCharArray());
            keyStore.load(new ByteArrayInputStream(certificadoBytes), request.getPassword().toCharArray());
            
            // 2. Obtener primer certificado
            Enumeration<String> aliases = keyStore.aliases();
            if (!aliases.hasMoreElements()) {
                throw new RuntimeException("No se encontraron certificados en el archivo .p12");
            }
            
            String alias = aliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            
            // 3. Extraer información del propietario (Subject)
            String subjectDN = cert.getSubjectX500Principal().getName();
            CertificateInfoResponse.OwnerInfo owner = CertificateInfoResponse.OwnerInfo.builder()
                .fullDN(subjectDN)
                .commonName(extractDNField(subjectDN, "CN"))
                .serialNumber(extractDNField(subjectDN, "SERIALNUMBER"))
                .organization(extractDNField(subjectDN, "O"))
                .organizationalUnit(extractDNField(subjectDN, "OU"))
                .country(extractDNField(subjectDN, "C"))
                .email(extractDNField(subjectDN, "E"))
                .build();
            
            // 4. Extraer información del emisor (Issuer)
            String issuerDN = cert.getIssuerX500Principal().getName();
            CertificateInfoResponse.IssuerInfo issuer = CertificateInfoResponse.IssuerInfo.builder()
                .fullDN(issuerDN)
                .commonName(extractDNField(issuerDN, "CN"))
                .organization(extractDNField(issuerDN, "O"))
                .organizationalUnit(extractDNField(issuerDN, "OU"))
                .country(extractDNField(issuerDN, "C"))
                .build();
            
            // 5. Información de validez
            Date now = new Date();
            Date notBefore = cert.getNotBefore();
            Date notAfter = cert.getNotAfter();
            boolean isCurrentlyValid = now.after(notBefore) && now.before(notAfter);
            boolean isExpired = now.after(notAfter);
            
            Long daysUntilExpiry = null;
            if (!isExpired) {
                daysUntilExpiry = (notAfter.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
            }
            
            CertificateInfoResponse.ValidityInfo validity = CertificateInfoResponse.ValidityInfo.builder()
                .validFrom(DATE_FORMAT.format(notBefore))
                .validUntil(DATE_FORMAT.format(notAfter))
                .isCurrentlyValid(isCurrentlyValid)
                .isExpired(isExpired)
                .daysUntilExpiry(daysUntilExpiry)
                .build();
            
            // 6. Información técnica
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, request.getPassword().toCharArray());
            int keySize = getKeySize(privateKey);
            
            CertificateInfoResponse.TechnicalInfo technical = CertificateInfoResponse.TechnicalInfo.builder()
                .version("V" + cert.getVersion())
                .serialNumber(cert.getSerialNumber().toString(16).toUpperCase())
                .signatureAlgorithm(cert.getSigAlgName())
                .publicKeyAlgorithm(cert.getPublicKey().getAlgorithm())
                .publicKeySize(keySize)
                .fingerprint(generateFingerprint(cert))
                .build();
            
            // 7. Extensiones y usos
            Map<String, String> extensions = extractExtensions(cert);
            List<String> keyUsages = extractKeyUsages(cert);
            List<String> extendedKeyUsages = extractExtendedKeyUsages(cert);
            
            // 8. Estado y mensaje
            String status;
            String statusMessage;
            if (now.before(notBefore)) {
                status = "NOT_YET_VALID";
                statusMessage = "El certificado aún no es válido";
            } else if (isExpired) {
                status = "EXPIRED";
                statusMessage = "El certificado ha expirado";
            } else if (daysUntilExpiry != null && daysUntilExpiry < 30) {
                status = "VALID_EXPIRING_SOON";
                statusMessage = "El certificado expira en " + daysUntilExpiry + " días";
            } else {
                status = "VALID";
                statusMessage = "El certificado es válido";
            }
            
            // 9. Construir respuesta
            return CertificateInfoResponse.builder()
                .owner(owner)
                .issuer(issuer)
                .validity(validity)
                .technical(technical)
                .extensions(extensions)
                .keyUsages(keyUsages)
                .extendedKeyUsages(extendedKeyUsages)
                .status(status)
                .statusMessage(statusMessage)
                .build();
            
        } catch (Exception e) {
            log.error("Error al validar certificado", e);
            throw new RuntimeException("Error al validar certificado: " + e.getMessage(), e);
        }
    }
    
    private String extractDNField(String dn, String field) {
        try {
            String[] parts = dn.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.startsWith(field + "=")) {
                    return trimmed.substring(field.length() + 1);
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo extraer campo {} del DN", field);
        }
        return null;
    }
    
    private int getKeySize(PrivateKey privateKey) {
        try {
            if (privateKey instanceof java.security.interfaces.RSAPrivateKey) {
                return ((java.security.interfaces.RSAPrivateKey) privateKey).getModulus().bitLength();
            } else if (privateKey instanceof java.security.interfaces.ECPrivateKey) {
                return ((java.security.interfaces.ECPrivateKey) privateKey).getParams().getOrder().bitLength();
            }
        } catch (Exception e) {
            log.warn("No se pudo determinar tamaño de clave");
        }
        return 0;
    }
    
    private String generateFingerprint(X509Certificate cert) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
                hexString.append(':');
            }
            return hexString.substring(0, hexString.length() - 1).toUpperCase();
        } catch (Exception e) {
            log.warn("No se pudo generar fingerprint");
            return null;
        }
    }
    
    private Map<String, String> extractExtensions(X509Certificate cert) {
        Map<String, String> extensions = new java.util.HashMap<>();
        try {
            Set<String> criticalOIDs = cert.getCriticalExtensionOIDs();
            Set<String> nonCriticalOIDs = cert.getNonCriticalExtensionOIDs();
            
            if (criticalOIDs != null) {
                for (String oid : criticalOIDs) {
                    extensions.put(oid + " (critical)", getOIDName(oid));
                }
            }
            
            if (nonCriticalOIDs != null) {
                for (String oid : nonCriticalOIDs) {
                    extensions.put(oid, getOIDName(oid));
                }
            }
        } catch (Exception e) {
            log.warn("Error extrayendo extensiones");
        }
        return extensions;
    }
    
    private String getOIDName(String oid) {
        Map<String, String> oidNames = Map.of(
            "2.5.29.15", "Key Usage",
            "2.5.29.37", "Extended Key Usage",
            "2.5.29.17", "Subject Alternative Name",
            "2.5.29.19", "Basic Constraints",
            "2.5.29.14", "Subject Key Identifier",
            "2.5.29.35", "Authority Key Identifier",
            "2.5.29.31", "CRL Distribution Points"
        );
        return oidNames.getOrDefault(oid, "Unknown Extension");
    }
    
    private List<String> extractKeyUsages(X509Certificate cert) {
        List<String> usages = new ArrayList<>();
        boolean[] keyUsage = cert.getKeyUsage();
        if (keyUsage != null) {
            String[] usageNames = {
                "Digital Signature", "Non Repudiation", "Key Encipherment",
                "Data Encipherment", "Key Agreement", "Certificate Sign",
                "CRL Sign", "Encipher Only", "Decipher Only"
            };
            for (int i = 0; i < keyUsage.length && i < usageNames.length; i++) {
                if (keyUsage[i]) {
                    usages.add(usageNames[i]);
                }
            }
        }
        return usages;
    }
    
    private List<String> extractExtendedKeyUsages(X509Certificate cert) {
        List<String> usages = new ArrayList<>();
        try {
            List<String> extendedKeyUsage = cert.getExtendedKeyUsage();
            if (extendedKeyUsage != null) {
                Map<String, String> oidPurposes = Map.of(
                    "1.3.6.1.5.5.7.3.1", "TLS Web Server Authentication",
                    "1.3.6.1.5.5.7.3.2", "TLS Web Client Authentication",
                    "1.3.6.1.5.5.7.3.3", "Code Signing",
                    "1.3.6.1.5.5.7.3.4", "Email Protection",
                    "1.3.6.1.5.5.7.3.8", "Time Stamping"
                );
                for (String oid : extendedKeyUsage) {
                    usages.add(oidPurposes.getOrDefault(oid, oid));
                }
            }
        } catch (Exception e) {
            log.warn("Error extrayendo extended key usages");
        }
        return usages;
    }
}
