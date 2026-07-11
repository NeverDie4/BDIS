package com.bdis.modules.spectrum.runner;

import com.bdis.modules.spectrum.dto.HerbAtlasImportRequest;
import com.bdis.modules.spectrum.service.HerbAtlasImportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class HerbAtlasImportRunner implements CommandLineRunner {

    private final HerbAtlasImportService herbAtlasImportService;
    private final boolean autoImport;

    public HerbAtlasImportRunner(
            HerbAtlasImportService herbAtlasImportService,
            @Value("${herb.atlas.auto-import:false}") boolean autoImport) {
        this.herbAtlasImportService = herbAtlasImportService;
        this.autoImport = autoImport;
    }

    @Override
    public void run(String... args) {
        if (autoImport) {
            herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());
        }
    }
}
