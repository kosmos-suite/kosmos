package de.oppahansi.kosmos.library.naming;

import de.oppahansi.kosmos.common.KosmosEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One content type's on-disk naming templates — see {@link NamingTemplateEngine} for the {@code
 * {Token}} syntax these are written in. Never required to exist: {@link NamingSettingsService}
 * falls back to a built-in default matching what {@code ImportService} used to hardcode, so an
 * instance nobody has customized behaves exactly as before.
 */
@Entity
@Table(name = "naming_settings")
public class NamingSettings extends KosmosEntity {

  /**
   * {@code movie}, {@code show}, or {@code anime} — matches {@code MediaItem#contentType}'s own
   * top-level values (episodes/anime_episodes are named via their owning show/anime's row).
   */
  @Column(name = "content_type", nullable = false, unique = true, length = 20)
  public String contentType;

  @Column(name = "folder_template", nullable = false, length = 500)
  public String folderTemplate;

  @Column(name = "file_template", nullable = false, length = 500)
  public String fileTemplate;

  /** Show only — null for movie/anime, which have no season subfolder. */
  @Column(name = "season_folder_template", length = 200)
  public String seasonFolderTemplate;
}
