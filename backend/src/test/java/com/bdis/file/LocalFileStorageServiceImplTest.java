package com.bdis.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.file.service.FileStorageService;
import com.bdis.file.service.impl.LocalFileStorageServiceImpl;
import com.bdis.file.vo.FileContentVO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageServiceImplTest {

    @TempDir private Path tempDir;

    @Test
    void saveLoadAndDeleteFile() {
        LocalFileStorageServiceImpl storageService =
                new LocalFileStorageServiceImpl(tempDir.toString());
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "demo.txt",
                        "text/plain",
                        "hello bdis".getBytes(StandardCharsets.UTF_8));

        FileStorageService.StoredFile storedFile = storageService.save(file);

        assertThat(storedFile.storedName()).endsWith(".txt");
        assertThat(storedFile.storagePath()).contains(".txt");

        FileContentVO content =
                storageService.load(
                        storedFile.storagePath(),
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getSize());

        assertThat(content.getResource().exists()).isTrue();
        assertThat(content.getFileName()).isEqualTo("demo.txt");
        assertThat(content.getContentType()).isEqualTo("text/plain");
        assertThat(content.getFileSize()).isEqualTo(file.getSize());

        storageService.delete(storedFile.storagePath());

        assertThat(content.getResource().exists()).isFalse();
    }
}
