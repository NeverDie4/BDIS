package com.bdis.file.policy;

/**
 * Authorizes operations on a business object that owns file relations.
 *
 * <p>Implementations must only inspect business data and authorization state. They must not read or
 * mutate file metadata, storage paths, or physical file content.
 */
public interface FileBusinessAccessPolicy {

    String bizType();

    boolean exists(Long bizId);

    boolean canView(Long bizId);

    boolean canAttach(Long bizId);

    boolean canDetach(Long bizId);

    boolean canPublish(Long bizId);
}
