#!/bin/bash
# wait-for-rabbitmq.sh

set -e

HOST=$1
PORT=$2

until nc -z -v -w30 $HOST $PORT
do
  echo "Waiting for RabbitMQ..."
  sleep 1
done

echo "RabbitMQ is up - starting the application"
exec "$@"