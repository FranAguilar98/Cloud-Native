package sistemadegestion.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class GuiaRequestDto {

    @NotBlank(message = "El número de guía es obligatorio")
    private String numeroGuia;

    @NotBlank(message = "El transportista es obligatorio")
    private String transportista;

    @NotBlank(message = "La fecha es obligatoria (formato yyyy-MM-dd)")
    private String fecha;

    @NotBlank(message = "El destinatario es obligatorio")
    private String destinatario;

    @NotBlank(message = "La dirección de entrega es obligatoria")
    private String direccion;

    @NotBlank(message = "La descripción del contenido es obligatoria")
    private String descripcion;

    @Positive(message = "El peso debe ser mayor a 0")
    private double peso;

    @Positive(message = "La cantidad de bultos debe ser mayor a 0")
    private int bultos;
}
