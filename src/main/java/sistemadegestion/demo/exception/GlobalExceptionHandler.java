package sistemadegestion.demo.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import sistemadegestion.demo.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	
	@ExceptionHandler(NoSuchBucketException.class)
	public ResponseEntity<ErrorResponse> handleNoSuchBucketException(NoSuchBucketException ex, WebRequest request) {

		log.error("Bucket no encontrado: {}", ex.getMessage());

		ErrorResponse error =
				ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.NOT_FOUND.value())
						.error("Bucket Not Found").message("El bucket especificado no existe en S3")
						.details(ex.getMessage()).path(getPath(request)).build();

		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}


	@ExceptionHandler(NoSuchKeyException.class)
	public ResponseEntity<ErrorResponse> handleNoSuchKeyException(NoSuchKeyException ex, WebRequest request) {

		log.error("Objeto no encontrado: {}", ex.getMessage());

		ErrorResponse error =
				ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.NOT_FOUND.value())
						.error("Object Not Found").message("El objeto especificado no existe en el bucket")
						.details(ex.getMessage()).path(getPath(request)).build();

		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}


	@ExceptionHandler(S3BucketNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleS3BucketNotFoundException(S3BucketNotFoundException ex,
			WebRequest request) {

		log.error("Bucket no encontrado: {}", ex.getMessage());

		ErrorResponse error =
				ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.NOT_FOUND.value())
						.error("Bucket Not Found").message(ex.getMessage()).path(getPath(request)).build();

		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}

	
	@ExceptionHandler(S3ObjectNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleS3ObjectNotFoundException(S3ObjectNotFoundException ex,
			WebRequest request) {

		log.error("Objeto no encontrado: {}", ex.getMessage());

		ErrorResponse error =
				ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.NOT_FOUND.value())
						.error("Object Not Found").message(ex.getMessage()).path(getPath(request)).build();

		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}

	
	@ExceptionHandler(S3AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleS3AccessDeniedException(S3AccessDeniedException ex, WebRequest request) {

		log.error("Acceso denegado: {}", ex.getMessage());

		ErrorResponse error = ErrorResponse.builder().timestamp(LocalDateTime.now())
				.status(HttpStatus.FORBIDDEN.value()).error("Access Denied").message(ex.getMessage())
				.details("Verifique las credenciales y permisos de IAM en AWS").path(getPath(request)).build();

		return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
	}

	
	@ExceptionHandler(S3UploadException.class)
	public ResponseEntity<ErrorResponse> handleS3UploadException(S3UploadException ex, WebRequest request) {

		log.error("Error al subir archivo: {}", ex.getMessage());

		ErrorResponse error =
				ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.INTERNAL_SERVER_ERROR.value())
						.error("Upload Failed").message(ex.getMessage()).path(getPath(request)).build();

		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	
	@ExceptionHandler(S3OperationException.class)
	public ResponseEntity<ErrorResponse> handleS3OperationException(S3OperationException ex, WebRequest request) {

		log.error("Error en operación S3: {}", ex.getMessage());

		ErrorResponse error =
				ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.INTERNAL_SERVER_ERROR.value())
						.error("S3 Operation Failed").message(ex.getMessage()).path(getPath(request)).build();

		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	
	@ExceptionHandler(InvalidFileException.class)
	public ResponseEntity<ErrorResponse> handleInvalidFileException(InvalidFileException ex, WebRequest request) {

		log.error("Archivo inválido: {}", ex.getMessage());

		ErrorResponse error =
				ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value())
						.error("Invalid File").message(ex.getMessage()).path(getPath(request)).build();

		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
			WebRequest request) {

		log.error("Archivo excede tamaño máximo: {}", ex.getMessage());

		ErrorResponse error =
				ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.PAYLOAD_TOO_LARGE.value())
						.error("File Too Large").message("El archivo excede el tamaño máximo permitido")
						.details(ex.getMessage()).path(getPath(request)).build();

		return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
	}

	
	@ExceptionHandler(S3Exception.class)
	public ResponseEntity<ErrorResponse> handleS3Exception(S3Exception ex, WebRequest request) {

		log.error("Error de S3: {} - Código: {}", ex.getMessage(), ex.statusCode());

		HttpStatus status = HttpStatus.valueOf(ex.statusCode());

		ErrorResponse error = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(status.value())
				.error("S3 Error").message("Error al realizar la operación en S3")
				.details(ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage())
				.path(getPath(request)).build();

		return new ResponseEntity<>(error, status);
	}

	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {

		log.error("Error inesperado: {}", ex.getMessage(), ex);

		ErrorResponse error =
				ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.INTERNAL_SERVER_ERROR.value())
						.error("Internal Server Error").message("Ha ocurrido un error inesperado en el servidor")
						.details(ex.getMessage()).path(getPath(request)).build();

		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	
	private String getPath(WebRequest request) {
		return request.getDescription(false).replace("uri=", "");
	}
}
