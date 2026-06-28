package sistemadegestion.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sistemadegestion.demo.dto.S3ObjectDto;
import sistemadegestion.demo.exception.*;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final S3Client s3Client;

    public List<S3ObjectDto> listObjects(String bucket) {
        try {
            log.info("Listando objetos del bucket: {}", bucket);
            ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).build();
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            return response.contents().stream()
                    .map(obj -> new S3ObjectDto(obj.key(), obj.size(),
                            obj.lastModified() != null ? obj.lastModified().toString() : null))
                    .collect(Collectors.toList());
        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) throw new S3AccessDeniedException("listar objetos del bucket: " + bucket, e);
            throw new S3OperationException("Error al listar objetos del bucket: " + bucket, e);
        }
    }

 
    public byte[] downloadAsBytes(String bucket, String key) {
        try {
            log.info("Descargando objeto: {} del bucket: {}", key, bucket);
            GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);
            log.info("Objeto descargado exitosamente: {}", key);
            return responseBytes.asByteArray();
        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);
        } catch (NoSuchKeyException e) {
            throw new S3ObjectNotFoundException(key, bucket, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) throw new S3AccessDeniedException("descargar el objeto: " + key, e);
            throw new S3OperationException("Error al descargar el objeto: " + key, e);
        }
    }


    public void upload(String bucket, String key, MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new InvalidFileException("El archivo está vacío o es nulo");
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank())
            throw new InvalidFileException("El nombre del archivo no es válido");

        try {
            log.info("Subiendo archivo: {} al bucket: {}, tamaño: {} bytes", key, bucket, file.getSize());
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket).key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Archivo subido exitosamente: {}", key);
        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) throw new S3AccessDeniedException("subir archivo al bucket: " + bucket, e);
            throw new S3UploadException("Error al subir el archivo a S3: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new S3UploadException("Error al leer el archivo: " + e.getMessage(), e);
        }
    }

    public void uploadBytes(String bucket, String key, byte[] bytes, String contentType) {
        try {
            log.info("Subiendo bytes: {} al bucket: {}, tamaño: {} bytes", key, bucket, bytes.length);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket).key(key)
                    .contentType(contentType)
                    .contentLength((long) bytes.length)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            log.info("Bytes subidos exitosamente: {}", key);
        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) throw new S3AccessDeniedException("subir bytes al bucket: " + bucket, e);
            throw new S3UploadException("Error al subir bytes a S3: " + e.getMessage(), e);
        }
    }


    public void moveObject(String bucket, String sourceKey, String destKey) {
        try {
            log.info("Moviendo objeto de {} a {} en el bucket: {}", sourceKey, destKey, bucket);
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucket).sourceKey(sourceKey)
                    .destinationBucket(bucket).destinationKey(destKey)
                    .build();
            s3Client.copyObject(copyRequest);
            deleteObject(bucket, sourceKey);
            log.info("Objeto movido exitosamente de {} a {}", sourceKey, destKey);
        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);
        } catch (NoSuchKeyException e) {
            throw new S3ObjectNotFoundException(sourceKey, bucket, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) throw new S3AccessDeniedException("mover objeto en el bucket: " + bucket, e);
            throw new S3OperationException("Error al mover el objeto de " + sourceKey + " a " + destKey, e);
        }
    }

   
    public void deleteObject(String bucket, String key) {
        try {
            log.info("Eliminando objeto: {} del bucket: {}", key, bucket);
            DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucket).key(key).build();
            s3Client.deleteObject(request);
            log.info("Objeto eliminado exitosamente: {}", key);
        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) throw new S3AccessDeniedException("eliminar objeto del bucket: " + bucket, e);
            throw new S3OperationException("Error al eliminar el objeto: " + key, e);
        }
    }


    public List<S3ObjectDto> listarGuiasPorTransportistaYFecha(String bucket, String transportista, String fecha) {
        try {
            String prefix = "pdf/" + fecha + "/" + transportista + "/";
            log.info("Filtrando guías con prefijo: {}", prefix);
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket).prefix(prefix).build();
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            log.info("Se encontraron {} guías para transportista={} fecha={}", 
                    response.contents().size(), transportista, fecha);
            return response.contents().stream()
                    .map(obj -> new S3ObjectDto(obj.key(), obj.size(),
                            obj.lastModified() != null ? obj.lastModified().toString() : null))
                    .collect(Collectors.toList());
        } catch (NoSuchBucketException e) {
            throw new S3BucketNotFoundException(bucket, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) throw new S3AccessDeniedException("listar guías del bucket: " + bucket, e);
            throw new S3OperationException("Error al filtrar guías", e);
        }
    }
}
