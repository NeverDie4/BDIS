package com.bdis.modules.herb.service;

public interface HerbSpeciesCoverFileService {

    String replaceCover(Long speciesId, String previousFileUrl, String requestedFileUrl);

    void deleteCover(Long speciesId, String fileUrl);
}
