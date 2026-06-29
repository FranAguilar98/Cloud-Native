package sistemadegestion.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sistemadegestion.demo.dto.GuiaDto;
import sistemadegestion.demo.dto.GuiaRequestDto;
import sistemadegestion.demo.dto.S3ObjectDto;
import sistemadegestion.demo.service.AwsS3Service;
import sistemadegestion.demo.service.EfsService;
import sistemadegestion.demo.service.GuiaPdfService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/guias")
@RequiredArgsConstructor
public class AwsS3Controller {

    private final AwsS3Service awsS3Service;
    private final EfsService efsService;
    private final GuiaPdfService guiaPdfService;

    @PostMapping("/{bucket}/generar")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> generarYSubirGuia(
            @PathVariable String bucket,
            @Valid @RequestBody GuiaRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Generando guía N° {}", request.getNumeroGuia());

        byte[] pdfBytes = guiaPdfService.generarGuia(
                request.getNumeroGuia(),
                request.getTransportista(),
                request.getFecha(),
                request.getDestinatario(),
                request.getDireccion(),
                request.getDescripcion(),
                request.getPeso(),
                request.getBultos()
        );

        String key = "pdf/" + request.getFecha() + "/" + request.getTransportista()
                + "/" + request.getNumeroGuia() + ".pdf";

        // ⚠️ EFS comentado temporalmente para pruebas sin EFS montado
        // efsService.saveToEfs(key, pdfBytes);

        awsS3Service.uploadBytes(bucket, key, pdfBytes, "application/pdf");

        log.info("Guía N° {} generada y subida exitosamente. Key: {}", request.getNumeroGuia(), key);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "mensaje", "Guía generada y subida exitosamente",
                    "numeroGuia", request.getNumeroGuia(),
                    "s3Key", key
                ));
    }

    @PostMapping("/{bucket}/subir")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> subirGuia(
            @PathVariable String bucket,
            @RequestParam String transportista,
            @RequestParam String fecha,
            @RequestParam String numeroGuia,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Subiendo guía archivo: {}", numeroGuia);

        String key = "pdf/" + fecha + "/" + transportista + "/" + numeroGuia + ".pdf";

        try {
            // ⚠️ EFS comentado temporalmente para pruebas sin EFS montado
            // efsService.saveToEfs(key, file);
            awsS3Service.upload(bucket, key, file);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al subir la guía: " + e.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "mensaje", "Guía subida exitosamente",
                    "s3Key", key
                ));
    }

    @GetMapping("/{bucket}/object")
    // @PreAuthorize("hasAnyRole('DESCARGA', 'ADMIN')")
    public ResponseEntity<byte[]> descargarGuia(
            @PathVariable String bucket,
            @RequestParam String key,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Descargando objeto: {}", key);

        byte[] fileBytes = awsS3Service.downloadAsBytes(bucket, key);
        String filename = key.contains("/") ? key.substring(key.lastIndexOf("/") + 1) : key;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileBytes);
    }

    @PutMapping("/{bucket}/object")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> modificarGuia(
            @PathVariable String bucket,
            @RequestParam String sourceKey,
            @RequestParam String destKey,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Modificando
