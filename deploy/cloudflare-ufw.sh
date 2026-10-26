#!/usr/bin/env bash
# Allow 80/443 ONLY from Cloudflare, so nobody can bypass Cloudflare/WAF or spoof CF-Connecting-IP.
# Run once as root, re-run occasionally (Cloudflare ranges change rarely).
set -euo pipefail
ufw default deny incoming
ufw default allow outgoing
ufw allow OpenSSH
for ip in $(curl -fsS https://www.cloudflare.com/ips-v4) $(curl -fsS https://www.cloudflare.com/ips-v6); do
  ufw allow from "$ip" to any port 80,443 proto tcp
done
ufw --force enable
ufw status | head -20
