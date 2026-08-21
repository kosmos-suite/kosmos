package de.oppahansi.kosmos.media.dto;

/**
 * One tile in Discover/Home's "Studios"/"Networks" rows — {@code logoPath} is a TMDB image path.
 */
public record StudioTile(int id, String name, String logoPath) {}
