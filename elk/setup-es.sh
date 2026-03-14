#!/bin/sh
# Wait for Elasticsearch to be ready, then create index template.
# This runs as a one-shot init container before filebeat starts.

ES_URL="${ES_URL:-http://elasticsearch:9200}"

echo "Waiting for Elasticsearch at ${ES_URL}..."
until curl -sf "${ES_URL}/_cluster/health" > /dev/null 2>&1; do
  sleep 2
done
echo "Elasticsearch is ready."

# Create index template (no data_stream, single shard, 0 replicas for dev)
curl -sf -X PUT "${ES_URL}/_index_template/api-server" \
  -H 'Content-Type: application/json' \
  -d '{
    "index_patterns": ["api-server-*"],
    "priority": 500,
    "template": {
      "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0
      }
    }
  }'

echo ""
echo "Index template 'api-server' created."
