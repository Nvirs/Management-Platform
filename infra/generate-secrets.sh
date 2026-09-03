#!/usr/bin/
set -eu

secrets_dir="$(dirname "$0")/secrets"
cd "$secrets_dir"

random_value() {
  openssl rand -hex 32
}

for name in db_password jwt_secret rabbitmq_password; do
  file="$name.txt"
  if [ -f "$file" ]; then
    echo "skip:    $file already exists"
  else
    random_value >"$file"
    chmod 644 "$file"
    echo "created: $file"
  fi
done
chmod 700 "$secrets_dir"
