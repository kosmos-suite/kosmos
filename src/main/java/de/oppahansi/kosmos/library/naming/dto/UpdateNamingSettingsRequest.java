package de.oppahansi.kosmos.library.naming.dto;

public record UpdateNamingSettingsRequest(
    String folderTemplate, String fileTemplate, String seasonFolderTemplate) {}
