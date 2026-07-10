# firmadigital-api

API REST para firma digital de documentos PDF con certificados electrónicos ecuatorianos.

Desarrollada con **Spring Boot** y la librería oficial **FIRMAEC** del Mintel. Inserta la rúbrica (sello visible con QR) directamente en el PDF sin pasar por servidores del Mintel. Para el sello de tiempo (TSA) utiliza **Uanataca**, conforme a la disposición del Mintel.

Repositorio: https://github.com/eduedudev/firmadigital-api

## Características

- **Firma PAdES** — cumple con el estándar PDF avanzado para firmas electrónicas
- **Sello visible con QR** — inserta un sello gráfico con código QR que contiene los datos del firmante
- **Firma local** — toda la operación criptográfica se realiza en el servidor, sin enviar documentos a terceros
- **Sello de tiempo (TSA)** — utiliza el servicio de Uanataca (dispuesto por Mintel) para sellar temporalmente cada firma
- **Verificación de firmas** — valida firmas existentes, incluyendo cadena de certificados y sello de tiempo
- **Web UI integrada** — interfaz web para subir PDF, posicionar visualmente la firma con vista previa página por página, y descargar el resultado
- **OpenAPI / Swagger** — documentación interactiva de la API
- **Certificados .p12** — soporta certificados emitidos por entidades ecuatorianas (Security Data, Banco Central, etc.)

## Requisitos

- Java 21
- Gradle (incluye wrapper)

## Inicio rápido

```bash
git clone --recurse-submodules https://github.com/eduedudev/firmadigital-api.git
cd firmadigital-api
./gradlew bootRun
```

La API arranca en `http://localhost:8081`.  
Swagger UI: `http://localhost:8081/swagger-ui/index.html`  
Web UI: `http://localhost:8081`

## Uso de la Web UI

1. Sube un archivo PDF
2. Navega entre páginas y haz clic para posicionar el sello de firma
3. Selecciona tu certificado `.p12` e ingresa la contraseña
4. Presiona **Firmar documento**
5. El PDF firmado se descarga automáticamente

## API

### Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/sign` | Firmar documento PDF |
| POST | `/api/verify` | Verificar firmas en un documento |
| POST | `/api/verify/binary` | Verificar firmas desde binario |
| POST | `/api/certificate/validate` | Validar certificado digital |
| GET | `/api/health` | Health check |

### Ejemplo de firma

```bash
curl -X POST http://localhost:8081/api/sign \
  -H "Content-Type: application/json" \
  -d '{
    "document": "<PDF en base64>",
    "certificate": "<archivo .p12 en base64>",
    "password": "contraseña",
    "documentName": "documento.pdf",
    "configuration": {
      "visibleSignature": true,
      "page": 1,
      "position": {
        "x": 150, "y": 120,
        "width": 110, "height": 36
      }
    }
  }'
```

## Arquitectura

```
                    ┌─────────────┐
                    │   Web UI    │
                    │  (HTML/JS)  │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  Spring Boot│
                    │    API      │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
       ┌──────▼─────┐     │     ┌──────▼──────┐
       │ FIRMAEC    │     │     │ TSA         │
       │ Librería   │     │     │ Uanataca    │
       │ (submódulo)│     │     │             │
       └────────────┘     │     └─────────────┘
                          │
                   ┌──────▼──────┐
                   │ Certificado │
                   │ .p12 (temp) │
                   └─────────────┘
```

## Build

```bash
./gradlew clean build
```

El JAR se genera en `build/libs/`.

## Licencia

MIT
