package sistemadegestion.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuiaDto {
    private String numeroGuia;
    private String transportista;
    private String fecha;
    private String s3Key;
    private Long size;
    private String lastModified;
}
