#!/usr/bin/env bash
# Nightly MySQL dump + uploads archive, keeps 14 days. Run from cron on the VPS:
#   0 2 * * * /opt/sgs/deploy/backup.sh >> /var/log/sgs-backup.log 2>&1
# Then copy /opt/sgs/backups OFF the server (rclone to Google Drive / R2 / Backblaze). A backup on the same disk is not a backup.
set -euo pipefail
cd /opt/sgs
set -a; . ./.env; set +a
mkdir -p backups
STAMP=$(date +%F_%H%M)
docker compose exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers sgs' \
  | gzip > "backups/sgs_db_${STAMP}.sql.gz"
docker compose run --rm --no-deps -v "$(pwd)/backups:/backup" --entrypoint sh api -c \
  "tar -czf /backup/sgs_uploads_${STAMP}.tar.gz -C /data uploads" || true
find backups -type f -mtime +14 -delete
echo "backup ${STAMP} done"
