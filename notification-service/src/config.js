export const config = {
  port: process.env.PORT || 8084,
  rabbitmqUrl: process.env.RABBITMQ_URL || 'amqp://guest:guest@localhost:5672',
};
