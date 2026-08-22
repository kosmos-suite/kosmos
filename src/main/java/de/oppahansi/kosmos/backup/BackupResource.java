package de.oppahansi.kosmos.backup;

import de.oppahansi.kosmos.backup.dto.BackupFileResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Triggering a backup is not here — {@code POST /jobs/database-backup/run} already does it, same as
 * every other job's "Run Now" (see {@link BackupJob}). This only lists/removes the files it writes.
 */
@Path("/backups")
@Produces(MediaType.APPLICATION_JSON)
public class BackupResource {

  @Inject BackupService backupService;

  @GET
  public List<BackupFileResponse> list() {
    return backupService.listBackups().stream().map(BackupFileResponse::from).toList();
  }

  @DELETE
  @Path("/{filename}")
  public Response delete(@PathParam("filename") String filename) {
    backupService.deleteBackup(filename);
    return Response.noContent().build();
  }
}
