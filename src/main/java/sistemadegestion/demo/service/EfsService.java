package sistemadegestion.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
@Service
public class EfsService {

    @Value("${efs.path}")
    private String efsPath;

 
    public File saveToEfs(String key, MultipartFile multipartFile) throws IOException {
        File dest = resolverRuta(key);
        multipartFile.transferTo(dest);
        log.info("Archivo guardado en EFS: {}", dest.getAbsolutePath());
        return dest;
    }

    
    public File saveToEfs(String key, byte[] bytes) {
        File dest = resolverRuta(key);
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(bytes);
            log.info("Bytes guardados en EFS: {}", dest.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar en EFS: " + e.getMessage(), e);
        }
        return dest;
    }

    private File resolverRuta(String key) {
        File dest = new File(efsPath, key);
        File parentDir = dest.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        return dest;
    }
}
