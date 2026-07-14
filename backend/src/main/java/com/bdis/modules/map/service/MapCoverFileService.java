package com.bdis.modules.map.service;

public interface MapCoverFileService {

    String replaceCover(Long pointId, String previousFileUrl, String requestedFileUrl);

    void deleteCover(Long pointId, String fileUrl);
}
