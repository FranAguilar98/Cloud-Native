# Sistema de Gestión de Pedidos y Guías de Despacho
## Cloud Native — Spring Boot + AWS S3/EFS + Azure AD B2C + API Gateway

---

## Arquitectura

```
Cliente → API Gateway → Spring Boot (EC2) → AWS S3
                   ↕                    ↘ EFS (temporal)
              Azure AD B2C (JWT)
```

---

## Requisitos previos

- JDK 17+
- Maven 3.8+
- Docker
- Cuenta AWS (S3 bucket + EFS montado en `/app/efs`)
- Tenant Azure AD B2C configurado

---

## Configuración Azure AD B2C

### 1. Crear el Tenant B2C
1. En Azure Portal → **Crear recurso** → *Azure AD B2C*
2. Anota el **nombre del tenant** (ej: `miempresa`)

### 2. Registrar la aplicación
1. Azure AD B2C → **Registros de aplicaciones** → Nueva
2. Tipo de cuenta: *Cuentas en cualquier directorio organizacional*
3. Anota el **Application (client) ID**

### 3. Crear el User Flow
1. Azure AD B2C → **Flujos de usuario** → Nuevo
2. Tipo: *Inicio de sesión y registro (B2C_1_signupsignin)*
3. Anota el nombre del flow (ej: `B2C_1_signupsignin`)

### 4. Crear los 2 roles de aplicación
En el registro de la app → **Roles de aplicación** → Crear:

| Nombre visible | Valor       | Descripción                           |
|---------------|-------------|---------------------------------------|
| Admin          | `ADMIN`     | Acceso completo a todos los endpoints |
| Descarga       | `DESCARGA`  | Solo puede descargar guías            |

### 5. Asignar roles a usuarios
Azure AD B2C → **Usuarios empresariales** → seleccionar usuario →
**Roles asignados** → Agregar asignación → elegir rol.

---

## Variables de entorno

Configura estas variables antes de ejecutar (o en el `docker run`):

```bash
AZURE_TENANT_NAME=miempresa
AZURE_USER_FLOW=B2C_1_signupsignin
AZURE_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_DEFAULT_REGION=us-east-1
```

---

## Endpoints

| Método | Ruta                          | Descripción                        | Rol requerido           |
|--------|-------------------------------|------------------------------------|-------------------------|
| POST   | `/guias/{bucket}/generar`     | Genera PDF automáticamente y sube  | ADMIN                   |
| POST   | `/guias/{bucket}/subir`       | Sube un PDF ya existente           | ADMIN                   |
| GET    | `/guias/{bucket}/object`      | Descarga una guía desde S3         | DESCARGA **o** ADMIN    |
| PUT    | `/guias/{bucket}/object`      | Modifica/renombra una guía         | ADMIN                   |
| DELETE | `/guias/{bucket}/object`      | Elimina una guía                   | ADMIN                   |
| GET    | `/guias/{bucket}/filtrar`     | Filtra guías por transportista+fecha| ADMIN                  |
| GET    | `/guias/{bucket}/objects`     | Lista todos los objetos del bucket  | ADMIN                  |
| GET    | `/actuator/health`            | Health check (sin autenticación)   | Público                 |

---

## Ejemplos de uso (Postman / cURL)

### Obtener token Azure AD B2C
```bash
curl -X POST \
  "https://<TENANT>.b2clogin.com/<TENANT>.onmicrosoft.com/<USER_FLOW>/oauth2/v2.0/token" \
  -d "grant_type=password&client_id=<CLIENT_ID>&username=<USER>&password=<PASS>&scope=openid"
```

### Generar guía automáticamente (ADMIN)
```bash
curl -X POST http://localhost:8080/guias/sgdemob/generar \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "numeroGuia": "GD-2024-001",
    "transportista": "Transportes-ABC",
    "fecha": "2024-06-27",
    "destinatario": "Juan Pérez",
    "direccion": "Av. Principal 123, Santiago",
    "descripcion": "Electrónica - Laptop",
    "peso": 2.5,
    "bultos": 1
  }'
```

### Descargar guía (DESCARGA o ADMIN)
```bash
curl -X GET "http://localhost:8080/guias/sgdemob/object?key=pdf/2024-06-27/Transportes-ABC/GD-2024-001.pdf" \
  -H "Authorization: Bearer <TOKEN>" \
  --output guia.pdf
```

### Filtrar guías por transportista y fecha (ADMIN)
```bash
curl -X GET "http://localhost:8080/guias/sgdemob/filtrar?transportista=Transportes-ABC&fecha=2024-06-27" \
  -H "Authorization: Bearer <TOKEN>"
```

### Eliminar guía (ADMIN)
```bash
curl -X DELETE "http://localhost:8080/guias/sgdemob/object?key=pdf/2024-06-27/Transportes-ABC/GD-2024-001.pdf" \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Configurar API Gateway (Azure API Management)

1. **Crear instancia APIM** en Azure Portal
2. **Importar API** → OpenAPI o manual → URL backend: `http://<EC2-IP>:8080`
3. **Registrar cada endpoint** de la tabla anterior
4. **Configurar política de validación JWT**:
   - En cada operación → **Políticas** → agregar `validate-jwt`
   - Issuer: `https://<TENANT>.b2clogin.com/<TENANT>.onmicrosoft.com/<USER_FLOW>/v2.0`
   - Audiences: `<CLIENT_ID>`
5. **Asignar suscripción** a los consumidores del API

---

## Ejecutar con Docker

```bash
# Build
docker build -t ms-guias-despacho .

# Run
docker run -p 8080:8080 \
  -e AZURE_TENANT_NAME=miempresa \
  -e AZURE_USER_FLOW=B2C_1_signupsignin \
  -e AZURE_CLIENT_ID=tu-client-id \
  -e AWS_ACCESS_KEY_ID=tu-key \
  -e AWS_SECRET_ACCESS_KEY=tu-secret \
  ms-guias-despacho
```

---

## Despliegue en EC2 (GitHub Actions)

El pipeline `.github/workflows/deploy.yml` automatiza:
1. Build de la imagen Docker
2. Push a Docker Hub
3. SSH al EC2 → pull + restart del contenedor

Configura estos **Secrets** en el repositorio GitHub:
- `DOCKER_USERNAME` / `DOCKER_PASSWORD`
- `EC2_HOST` / `EC2_SSH_KEY`
- `AZURE_TENANT_NAME` / `AZURE_USER_FLOW` / `AZURE_CLIENT_ID`
- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`
