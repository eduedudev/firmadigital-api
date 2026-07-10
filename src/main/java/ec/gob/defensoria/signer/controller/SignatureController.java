package ec.gob.defensoria.signer.controller;

import ec.gob.defensoria.signer.dto.*;
import ec.gob.defensoria.signer.service.SignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Controlador REST para operaciones de firma digital
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Digital Signature", description = "APIs for signing and verifying documents digitally")
public class SignatureController {
    
    private final SignatureService firmaService;
    
    /**
     * Firma un document digitalmente
     */
    @PostMapping(value = "/sign", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sign document", 
               description = "Digitally sign a document (PDF, DOCX, XLSX, etc.) with a .p12 digital certificate")
    public ResponseEntity<SignResponse> sign(@Valid @RequestBody SignRequest request) {
        log.info("POST /api/sign - Iniciando firma de document");
        SignResponse response = firmaService.sign(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verifica las signatures de un document
     */
    @PostMapping(value = "/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verify signatures", 
               description = "Verify the digital signatures of a signed document")
    public ResponseEntity<VerifyResponse> verify(@Valid @RequestBody VerifyRequest request) {
        log.info("POST /api/verify - Verificando signatures del document");
        VerifyResponse response = firmaService.verify(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifica las signatures de un document subido en binario
     */
    @PostMapping(value = "/verify/binary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verify signatures (Binary)", 
               description = "Verify the digital signatures of a signed document uploaded as a binary file")
    public ResponseEntity<VerifyResponse> verifyBinary(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("POST /api/verify/binary - Verificando signatures del document binario: {}", file.getOriginalFilename());
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        
        VerifyResponse response = firmaService.verify(file.getBytes());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Valida y analiza un certificado digital extrayendo toda la información disponible
     */
    @PostMapping(value = "/certificate/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validate Certificate", 
               description = "Validates and extracts complete information from a digital certificate (.p12)")
    public ResponseEntity<CertificateInfoResponse> validateCertificate(@Valid @RequestBody ValidateCertificateRequest request) {
        log.info("POST /api/certificate/validate - Validando y analizando certificado digital");
        CertificateInfoResponse response = firmaService.validateCertificate(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Health Check", description = "Verifica que el servicio esté disponible")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\"}");
    }
}
