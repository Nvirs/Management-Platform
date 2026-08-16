import { readFileSync } from 'node:fs';

// Docker/Kubernetes mount secret files here 
function readSecret(secretName, envVarName, fallback) {
  try {
    return readFileSync(`/run/secrets/${secretName}`, 'utf-8').trim();
  } catch {
    return process.env[envVarName] || fallback;
  }
}

export const config = {
  port: process.env.PORT || 8084,
  rabbitmq: {
    hostname: process.env.RABBITMQ_HOST || 'localhost',
    port: Number(process.env.RABBITMQ_PORT) || 5672,
    username: process.env.RABBITMQ_USER || 'guest',
    password: readSecret('rabbitmq_password', 'RABBITMQ_PASSWORD', 'guest'),
  },
};
