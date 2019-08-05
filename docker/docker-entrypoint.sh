#!/bin/bash

: ${GRAPH_FILE_TARGET_PATH="/code/otpdata/norway/Graph.obj"}
: ${FILE_TMP_PATH="/tmp/graph_obj_from_gcs"}
# Notice ending slash here, it is correct
: ${MARDUK_GCP_BASE="gs://marduk/"}

echo "GRAPH_FILE_TARGET_PATH: $GRAPH_FILE_TARGET_PATH"

echo "Activating marduk blobstore service account"
/code/google-cloud-sdk/bin/gcloud auth activate-service-account --key-file /etc/marduk/marduk-blobstore-credentials.json

FILENAME=$(/code/google-cloud-sdk/bin/gsutil cat ${MARDUK_GCP_BASE}graphs/current)

DOWNLOAD="${MARDUK_GCP_BASE}${FILENAME}"
echo "Downloading $DOWNLOAD"
/code/google-cloud-sdk/bin/gsutil cp $DOWNLOAD $FILE_TMP_PATH

# Testing exists and has a size greater than zero
if [ -s $FILE_TMP_PATH ] ;
then
  echo "Overwriting $GRAPH_FILE_TARGET_PATH"
  mv $FILE_TMP_PATH $GRAPH_FILE_TARGET_PATH
else
  echo "** WARNING: Downloaded file ($FILE_TMP_PATH) is empty or not present**"
  echo "** Not overwriting $GRAPH_FILE_TARGET_PATH**"
  wget -q --header 'Content-Type: application/json' --post-data='{"source":"otp", "message":":no_entry: Downloaded file is empty or not present. This makes OTP fail! Please check logs"}' http://hubot/hubot/say/
  echo "Now sleeping 5m in the hope that this will be manually resolved in the mean time, and then restarting."
  sleep 5m
  exit 1
fi

exec "$@"
