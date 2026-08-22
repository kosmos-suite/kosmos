package de.oppahansi.kosmos.library.naming.dto;

import de.oppahansi.kosmos.library.naming.NamingSettings;

public record NamingSettingsResponse(
    String contentType, String folderTemplate, String fileTemplate, String seasonFolderTemplate) {

  public static NamingSettingsResponse from(NamingSettings settings) {
    return new NamingSettingsResponse(
        settings.contentType,
        settings.folderTemplate,
        settings.fileTemplate,
        settings.seasonFolderTemplate);
  }
}
