package com.bdis.modules.spectrum.vo;

import lombok.Data;

@Data
public class HerbAtlasImportItemVO {

    private String fileName;

    private String speciesCode;

    private String status;

    private String message;

    public static HerbAtlasImportItemVO of(
            String fileName, String speciesCode, String status, String message) {
        HerbAtlasImportItemVO item = new HerbAtlasImportItemVO();
        item.setFileName(fileName);
        item.setSpeciesCode(speciesCode);
        item.setStatus(status);
        item.setMessage(message);
        return item;
    }
}
