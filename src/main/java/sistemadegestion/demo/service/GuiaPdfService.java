package sistemadegestion.demo.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class GuiaPdfService {


    public byte[] generarGuia(
            String numeroGuia,
            String transportista,
            String fecha,
            String destinatario,
            String direccion,
            String descripcion,
            double peso,
            int bultos) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // ── ENCABEZADO ──────────────────────────────────────────────
            Paragraph titulo = new Paragraph("GUÍA DE DESPACHO")
                    .setFontSize(22)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.WHITE)
                    .setBackgroundColor(ColorConstants.DARK_GRAY)
                    .setPadding(10);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Sistema de Gestión de Pedidos y Despacho")
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(15);
            document.add(subtitulo);

            // ── DATOS DE LA GUÍA ────────────────────────────────────────
            Table tablaGuia = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(15);

            agregarFilaTabla(tablaGuia, "N° Guía:", numeroGuia, true);
            agregarFilaTabla(tablaGuia, "Fecha de Emisión:", fecha, false);
            agregarFilaTabla(tablaGuia, "Generado el:", LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), true);
            document.add(tablaGuia);

         
            document.add(seccionTitulo("DATOS DEL TRANSPORTISTA"));
            Table tablaTransp = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(15);
            agregarFilaTabla(tablaTransp, "Empresa Transportista:", transportista, false);
            document.add(tablaTransp);

            document.add(seccionTitulo("DATOS DEL DESTINATARIO"));
            Table tablaDestinatario = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(15);
            agregarFilaTabla(tablaDestinatario, "Destinatario:", destinatario, false);
            agregarFilaTabla(tablaDestinatario, "Dirección de Entrega:", direccion, true);
            document.add(tablaDestinatario);

         
            document.add(seccionTitulo("DETALLE DEL ENVÍO"));
            Table tablaDetalle = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(15);
            agregarFilaTabla(tablaDetalle, "Descripción:", descripcion, false);
            agregarFilaTabla(tablaDetalle, "Peso Total (kg):", String.valueOf(peso), true);
            agregarFilaTabla(tablaDetalle, "Cantidad de Bultos:", String.valueOf(bultos), false);
            document.add(tablaDetalle);

          
            document.add(new Paragraph("\n\n"));
            Table tablaFirmas = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginTop(20);

            Cell firmaEmisor = new Cell()
                    .add(new Paragraph("\n\n_________________________\nFirma Emisor")
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBorder(null);
            Cell firmaReceptor = new Cell()
                    .add(new Paragraph("\n\n_________________________\nFirma Receptor")
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBorder(null);

            tablaFirmas.addCell(firmaEmisor);
            tablaFirmas.addCell(firmaReceptor);
            document.add(tablaFirmas);

           
            document.add(new Paragraph(
                "Documento generado automáticamente por el Sistema de Gestión de Pedidos. " +
                "Guía N° " + numeroGuia)
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar la guía PDF: " + e.getMessage(), e);
        }
    }

   

    private Paragraph seccionTitulo(String texto) {
        return new Paragraph(texto)
                .setFontSize(12)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(ColorConstants.GRAY)
                .setPadding(5)
                .setMarginTop(10);
    }

    private void agregarFilaTabla(Table tabla, String etiqueta, String valor, boolean sombreado) {
        var bgColor = sombreado
                ? new com.itextpdf.kernel.colors.DeviceRgb(240, 240, 240)
                : null;

        Cell celdaEtiqueta = new Cell()
                .add(new Paragraph(etiqueta).setBold().setFontSize(10))
                .setPadding(6);
        Cell celdaValor = new Cell()
                .add(new Paragraph(valor != null ? valor : "-").setFontSize(10))
                .setPadding(6);

        if (bgColor != null) {
            celdaEtiqueta.setBackgroundColor(bgColor);
            celdaValor.setBackgroundColor(bgColor);
        }

        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }
}
