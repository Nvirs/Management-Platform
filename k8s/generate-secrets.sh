#!/usr/bin/env bash
set -eu

namespace="eventplatform"
secret_name="eventplatform-secrets"

random_value() {
  openssl rand -hex 32
}

kubectl create namespace "$namespace" --dry-run=client -o yaml | kubectl apply -f -

if kubectl get secret "$secret_name" -n "$namespace" >/dev/null 2>&1; then
  echo "skip:    secret/$secret_name already exists in namespace $namespace"
  exit 0
fi

kubectl create secret generic "$secret_name" \
  --namespace "$namespace" \
  --from-literal=db_password="$(random_value)" \
  --from-literal=jwt_secret="$(random_value)" \
  --from-literal=rabbitmq_password="$(random_value)"

echo "created: secret/$secret_name in namespace $namespace"
