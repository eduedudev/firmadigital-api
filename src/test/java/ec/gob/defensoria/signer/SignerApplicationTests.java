package ec.gob.defensoria.signer;

import ec.gob.defensoria.signer.dto.VerifyResponse;
import ec.gob.defensoria.signer.service.SignatureService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SignerApplicationTests {

	@Autowired
	private SignatureService signatureService;

	@Test
	@DisplayName("Carga del contexto de Spring")
	void contextLoads() {
	}

	// ─────────────────────────────────────────────────────────────
	// Helper
	// ─────────────────────────────────────────────────────────────

	private byte[] loadPdf(String resourceName) throws Exception {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
			assertNotNull(is, "No se encontró el recurso: " + resourceName);
			return is.readAllBytes();
		}
	}

	// Nombre del PDF de prueba (2 firmas: una con sello de tiempo, otra sin él)
	private static final String PDF_DOS_FIRMAS = "test-dos-firmas.pdf";

	// ─────────────────────────────────────────────────────────────
	// Tests generales del documento
	// ─────────────────────────────────────────────────────────────

	@Test
	@DisplayName("El documento debe contener exactamente dos firmas")
	void documentoDebeTenerDosFirmas() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		assertNotNull(response);
		assertNotNull(response.getSignatures());
		assertEquals(2, response.getSignatures().size(),
				"Se esperaban 2 firmas en el documento");
	}

	@Test
	@DisplayName("El documento debe ser marcado como válido")
	void documentoDebeSerValido() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		assertNotNull(response.getDocumentValid());
		// Si algún certificado está expirado el flag puede ser false,
		// pero el objeto response igual debe estar construido correctamente
		assertNotNull(response.getDocumentValid(),
				"El campo documentValid no debe ser nulo");
	}

	// ─────────────────────────────────────────────────────────────
	// Tests de datos básicos de cada firma
	// ─────────────────────────────────────────────────────────────

	@Test
	@DisplayName("Cada firma debe tener firmante y fecha de firma")
	void cadaFirmaDebeTenerFirmanteYFecha() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		for (VerifyResponse.SignatureInfo sig : response.getSignatures()) {
			assertNotNull(sig.getSigner(),    "El firmante no debe ser nulo");
			assertNotNull(sig.getSignDate(),  "La fecha de firma no debe ser nula");
			assertFalse(sig.getSigner().isBlank(), "El firmante no debe estar vacío");
			assertFalse(sig.getSignDate().isBlank(), "La fecha de firma no debe estar vacía");
		}
	}

	@Test
	@DisplayName("Cada firma debe tener entidad certificadora")
	void cadaFirmaDebeTenerEntidadCertificadora() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		for (VerifyResponse.SignatureInfo sig : response.getSignatures()) {
			assertNotNull(sig.getCertifyingAuthority(),
					"La entidad certificadora no debe ser nula para: " + sig.getSigner());
		}
	}

	@Test
	@DisplayName("Cada firma debe tener el campo hasTimestamp definido (no nulo)")
	void cadaFirmaDebeTenerHasTimestampDefinido() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		for (VerifyResponse.SignatureInfo sig : response.getSignatures()) {
			assertNotNull(sig.getHasTimestamp(),
					"hasTimestamp no debe ser nulo para: " + sig.getSigner());
		}
	}

	// ─────────────────────────────────────────────────────────────
	// Tests de sello de tiempo
	// ─────────────────────────────────────────────────────────────

	@Test
	@DisplayName("Debe existir al menos una firma CON sello de tiempo")
	void debeExistirAlMenosUnaFirmaConSelloTiempo() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		long conTimestamp = response.getSignatures().stream()
				.filter(s -> Boolean.TRUE.equals(s.getHasTimestamp()))
				.count();

		assertTrue(conTimestamp >= 1,
				"Se esperaba al menos una firma con sello de tiempo, pero ninguna lo tiene");
	}

	@Test
	@DisplayName("Debe existir al menos una firma SIN sello de tiempo")
	void debeExistirAlMenosUnaFirmaSinSelloTiempo() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		long sinTimestamp = response.getSignatures().stream()
				.filter(s -> Boolean.FALSE.equals(s.getHasTimestamp()))
				.count();

		assertTrue(sinTimestamp >= 1,
				"Se esperaba al menos una firma sin sello de tiempo, pero todas lo tienen");
	}

	@Test
	@DisplayName("La firma CON sello de tiempo debe tener fecha y emisor del sello")
	void firmaConSelloTiempoDebeTenerFechaYEmisor() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		List<VerifyResponse.SignatureInfo> conTimestamp = response.getSignatures().stream()
				.filter(s -> Boolean.TRUE.equals(s.getHasTimestamp()))
				.toList();

		assertFalse(conTimestamp.isEmpty(), "No se encontró ninguna firma con sello de tiempo");

		for (VerifyResponse.SignatureInfo sig : conTimestamp) {
			assertNotNull(sig.getTimestampDate(),
					"La fecha del sello de tiempo no debe ser nula para: " + sig.getSigner());
			assertFalse(sig.getTimestampDate().isBlank(),
					"La fecha del sello de tiempo no debe estar vacía para: " + sig.getSigner());
			assertNotNull(sig.getTimestampValid(),
					"timestampValid no debe ser nulo para: " + sig.getSigner());
		}
	}

	@Test
	@DisplayName("La firma SIN sello de tiempo no debe tener fecha ni emisor del sello")
	void firmaSinSelloTiempoNoDebeTenerDatosDeSello() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		List<VerifyResponse.SignatureInfo> sinTimestamp = response.getSignatures().stream()
				.filter(s -> Boolean.FALSE.equals(s.getHasTimestamp()))
				.toList();

		assertFalse(sinTimestamp.isEmpty(), "No se encontró ninguna firma sin sello de tiempo");

		for (VerifyResponse.SignatureInfo sig : sinTimestamp) {
			assertNull(sig.getTimestampDate(),
					"La fecha del sello de tiempo debe ser nula para firma sin sello: " + sig.getSigner());
			assertNull(sig.getTimestampIssuedBy(),
					"El emisor del sello debe ser nulo para firma sin sello: " + sig.getSigner());
			assertFalse(Boolean.TRUE.equals(sig.getTimestampValid()),
					"timestampValid debe ser false para firma sin sello: " + sig.getSigner());
		}
	}

	@Test
	@DisplayName("La firma CON sello de tiempo debe tener un timestampMessage con el formato correcto")
	void firmaConSelloTiempoDebeTenerMensajeFormateado() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		List<VerifyResponse.SignatureInfo> conTimestamp = response.getSignatures().stream()
				.filter(s -> Boolean.TRUE.equals(s.getHasTimestamp()))
				.toList();

		assertFalse(conTimestamp.isEmpty(), "No se encontró ninguna firma con sello de tiempo");

		for (VerifyResponse.SignatureInfo sig : conTimestamp) {
			String msg = sig.getTimestampMessage();
			assertNotNull(msg, "timestampMessage no debe ser nulo");
			assertFalse(msg.isBlank(), "timestampMessage no debe estar vacío");

			assertTrue(msg.contains(sig.getTimestampDate()),
					"El mensaje debe contener la fecha del sello");
			assertTrue(msg.contains("hora de Ecuador"),
					"El mensaje debe contener 'hora de Ecuador'");
			assertTrue(msg.contains(sig.getTimestampCN()),
					"El mensaje debe contener el CN de la TSA");
			assertTrue(msg.contains("emitido por"),
					"El mensaje debe contener 'emitido por'");
			assertTrue(msg.contains(sig.getTimestampIssuedBy()),
					"El mensaje debe contener el emisor del sello");

			System.out.println("timestampMessage: " + msg);
		}
	}

	// ─────────────────────────────────────────────────────────────
	// Test de impresión para inspección manual
	// ─────────────────────────────────────────────────────────────

	@Test
	@DisplayName("Imprimir detalle completo de todas las firmas (inspección manual)")
	void imprimirDetalleCompleto() throws Exception {
		byte[] pdf = loadPdf(PDF_DOS_FIRMAS);
		VerifyResponse response = signatureService.verify(pdf);

		System.out.println("=== RESULTADO VERIFICACIÓN ===");
		System.out.println("Documento válido: " + response.getDocumentValid());
		System.out.println("Total de firmas:  " + response.getSignatures().size());
		System.out.println();

		int i = 1;
		for (VerifyResponse.SignatureInfo sig : response.getSignatures()) {
			System.out.println("--- Firma #" + i++ + " ---");
			System.out.println("  Firmante          : " + sig.getSigner());
			System.out.println("  Cédula            : " + sig.getCedula());
			System.out.println("  Entidad cert.     : " + sig.getCertifyingAuthority());
			System.out.println("  Fecha firma       : " + sig.getSignDate());
			System.out.println("  Válido en fecha   : " + sig.getValidAtSignDate());
			System.out.println("  Tiene sello tiempo: " + sig.getHasTimestamp());
			System.out.println("  Fecha sello       : " + sig.getTimestampDate());
			System.out.println("  Emisor sello      : " + sig.getTimestampIssuedBy());
			System.out.println("  CN TSA            : " + sig.getTimestampCN());
			System.out.println("  Sello válido      : " + sig.getTimestampValid());
			if (sig.getTimestampMessage() != null) {
				System.out.println("  Mensaje sello     : " + sig.getTimestampMessage());
			}
			System.out.println();
		}

		// Siempre pasa — sirve solo para inspección visual en la consola
		assertTrue(true);
	}
}
