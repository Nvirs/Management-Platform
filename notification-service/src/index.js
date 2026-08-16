import { createApp } from './app.js';
import { config } from './config.js';
import { handleRegistrationConfirmed } from './handlers/registrationConfirmed.js';
import { connectWithRetry, startConsumer } from './rabbitmq.js';

const app = createApp();

app.listen(config.port, () => {
  console.log(`notification-service listening on port ${config.port}`);
});

connectWithRetry(config.rabbitmq)
  .then((connection) => startConsumer(connection, handleRegistrationConfirmed))
  .then(() => console.log('Consuming registration.confirmed events'))
  .catch((err) => {
    console.error('Could not connect to RabbitMQ, exiting:', err.message);
    process.exit(1);
  });
