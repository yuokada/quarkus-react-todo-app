#!/usr/bin/env bash
# Initialize DynamoDB Local tables for local development
# Usage: ./scripts/init-dynamodb.sh [endpoint]
# Default endpoint: http://localhost:8000

set -euo pipefail

ENDPOINT="${1:-http://localhost:8000}"
REGION="ap-northeast-1"

AWS_ARGS="--endpoint-url $ENDPOINT --region $REGION"
# Use dummy credentials for DynamoDB Local
export AWS_ACCESS_KEY_ID=dummy
export AWS_SECRET_ACCESS_KEY=dummy

echo "Initializing DynamoDB Local at $ENDPOINT ..."

# Create todo_tasks table
if aws dynamodb describe-table --table-name todo_tasks $AWS_ARGS > /dev/null 2>&1; then
  echo "  todo_tasks: already exists, skipping"
else
  aws dynamodb create-table \
    --table-name todo_tasks \
    --attribute-definitions AttributeName=id,AttributeType=N \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    $AWS_ARGS > /dev/null
  echo "  todo_tasks: created"
fi

# Create app_counters table
if aws dynamodb describe-table --table-name app_counters $AWS_ARGS > /dev/null 2>&1; then
  echo "  app_counters: already exists, skipping"
else
  aws dynamodb create-table \
    --table-name app_counters \
    --attribute-definitions AttributeName=counterName,AttributeType=S \
    --key-schema AttributeName=counterName,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    $AWS_ARGS > /dev/null
  echo "  app_counters: created"
fi

echo "Done."
