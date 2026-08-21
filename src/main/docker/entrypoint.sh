#!/bin/sh
set -e

# Defaults match the image's baked-in non-root user (185:root) — unset PUID/PGID reproduces the
# previous fixed-UID behavior exactly.
PUID="${PUID:-185}"
PGID="${PGID:-0}"

if [ "$(id -u)" = "0" ]; then
    # Not recursive: /library can be a real, large media collection, and chowning the whole tree
    # on every container start doesn't scale. Files/directories the app creates from here on
    # inherit PUID:PGID from the process itself; only the mount point needs to be writable upfront.
    chown "${PUID}:${PGID}" /library

    # UBI9 has no setpriv/gosu/su-exec, and coreutils su doesn't accept a numeric UID with no
    # matching /etc/passwd entry — chroot --userspec does, without needing to fabricate one.
    exec chroot --userspec="${PUID}:${PGID}" / /opt/jboss/container/java/run/run-java.sh
fi

exec /opt/jboss/container/java/run/run-java.sh
