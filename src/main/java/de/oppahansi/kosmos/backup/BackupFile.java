package de.oppahansi.kosmos.backup;

import java.time.Instant;

public record BackupFile(String filename, long sizeBytes, Instant createdAt) {}
