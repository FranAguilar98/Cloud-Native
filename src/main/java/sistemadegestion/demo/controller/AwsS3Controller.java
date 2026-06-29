package sistemadegestion.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, String>> generarYSubirGuia(
            @PathVariable String bucket,
            @Valid @RequestBody GuiaRequestDto request) {

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

        String key = "pdf/" + request.getFecha() + "/" +
                request.getTransportista() + "/" +
                request.getNumeroGuia() + ".pdf";

        efsService.saveToEfs(key, pdfBytes);

        awsS3Service.uploadBytes(
                bucket,
                key,
                pdfBytes,
                "application/pdf"
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "mensaje", "Guía generada y subida exitosamente",
                        "numeroGuia", request.getNumeroGuia(),
                        "s3Key", key
                ));
    }

    @PostMapping("/{bucket}/subir")
    public ResponseEntity<Map<String, String>> subirGuia(
            @PathVariable String bucket,
            @RequestParam String transportista,
            @RequestParam String fecha,
            @RequestParam String numeroGuia,
            @RequestParam("file") MultipartFile file) {

        log.info("Subiendo guía {}", numeroGuia);

        String key = "pdf/" +
                fecha +
                "/" +
                transportista +
                "/" +
                numeroGuia +
                ".pdf";

        try {

            efsService.saveToEfs(key, file);

            awsS3Service.upload(
                    bucket,
                    key,
                    file
            );

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "error",
                                    "Error al subir la guía: "
                                            + e.getMessage()
                            )
                    );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        Map.of(
                                "mensaje",
                                "Guía subida exitosamente",
                                "s3Key",
                                key
                        )
                );
    }

    @GetMapping("/{bucket}/object")
    public ResponseEntity<byte[]> descargarGuia(
            @PathVariable String bucket,
            @RequestParam String key) {

        log.info("Descargando {}", key);

        byte[] fileBytes =
                awsS3Service.downloadAsBytes(
                        bucket,
                        key
                );

        String filename =
                key.contains("/")
                        ? key.substring(
                                key.lastIndexOf("/") + 1
                        )
                        : key;

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                filename +
                                "\""
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(fileBytes);
    }

    @PutMapping("/{bucket}/object")
    public ResponseEntity<Map<String, String>> modificarGuia(
            @PathVariable String bucket,
            @RequestParam String sourceKey,
            @RequestParam String destKey) {

        log.info(
                "Moviendo {} -> {}",
                sourceKey,
                destKey
        );

        awsS3Service.moveObject(
                bucket,
                sourceKey,
                destKey
        );

        return ResponseEntity.ok(
                Map.of(
                        "mensaje",
                        "Guía actualizada exitosamente",
                        "nuevaKey",
                        destKey
                )
        );
    }

    @DeleteMapping("/{bucket}/object")
    public ResponseEntity<Map<String, String>> eliminarGuia(
            @PathVariable String bucket,
            @RequestParam String key) {

        log.info(
                "Eliminando {}",
                key
        );

        awsS3Service.deleteObject(
                bucket,
                key
        );

        return ResponseEntity.ok(
                Map.of(
                        "mensaje",
                        "Guía eliminada exitosamente",
                        "key",
                        key
                )
        );
    }

    @GetMapping("/{bucket}/filtrar")
    public ResponseEntity<List<GuiaDto>> filtrarGuias(
            @PathVariable String bucket,
            @RequestParam String transportista,
            @RequestParam String fecha) {

        log.info(
                "Filtrando guías - transportista: {}, fecha: {}",
                transportista,
                fecha
        );

        List<GuiaDto> guias =
                awsS3Service
                        .listarGuiasPorTransportistaYFecha(
                                bucket,
                                transportista,
                                fecha
                        )
                        .stream()
                        .map(
                                obj ->
                                        GuiaDto.builder()
                                                .s3Key(
                                                        obj.getKey()
                                                )
                                                .size(
                                                        obj.getSize()
                                                )
                                                .lastModified(
                                                        obj.getLastModified()
                                                )
                                                .transportista(
                                                        transportista
                                                )
                                                .fecha(
                                                        fecha
                                                )
                                                .numeroGuia(
                                                        extraerNumeroGuia(
                                                                obj.getKey()
                                                        )
                                                )
                                                .build()
                        )
                        .toList();

        return ResponseEntity.ok(guias);
    }

    @GetMapping("/{bucket}/objects")
    public ResponseEntity<List<S3ObjectDto>> listarObjetos(
            @PathVariable String bucket) {

        log.info(
                "Listando bucket {}",
                bucket
        );

        return ResponseEntity.ok(
                awsS3Service.listObjects(
                        bucket
                )
        );
    }

    private String extraerNumeroGuia(
            String key
    ) {

        if (key == null) {
            return "";
        }

        String[] parts =
                key.split("/");

        String nombre =
                parts[
                        parts.length - 1
                ];

        return nombre.endsWith(".pdf")
                ? nombre.substring(
                        0,
                        nombre.length() - 4
                )
                : nombre;
    }
}
