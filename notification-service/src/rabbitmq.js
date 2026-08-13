import amqp from 'amqplib';

export const EXCHANGE = 'events';
export const QUEUE = 'notification-service.registration-confirmed';
export const ROUTING_KEY = 'registration.confirmed';

export async function connectWithRetry(url, { retries = 10, delayMs = 3000 } = {}) {
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      return await amqp.connect(url);
    } catch (err) {
      const isLastAttempt = attempt === retries;
      console.error(
        `RabbitMQ connection failed (attempt ${attempt}/${retries}): ${err.message}`
      );
      if (isLastAttempt) throw err;
      await new Promise((resolve) => setTimeout(resolve, delayMs));
    }
  }
}

export async function startConsumer(connection, handler) {
  const channel = await connection.createChannel();

  await channel.assertExchange(EXCHANGE, 'topic', { durable: true });
  await channel.assertQueue(QUEUE, { durable: true });
  await channel.bindQueue(QUEUE, EXCHANGE, ROUTING_KEY);

  channel.consume(QUEUE, async (msg) => {
    if (!msg) return;

    try {
      const payload = JSON.parse(msg.content.toString());
      await handler(payload);
      channel.ack(msg);
    } catch (err) {
      console.error('Failed to process message, discarding:', err.message);
      channel.nack(msg, false, false);
    }
  });

  return channel;
}
