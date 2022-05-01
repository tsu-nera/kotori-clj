#!/usr/bin/env bash
clojure -Tjib build :aliases "[:run]"
gcloud run deploy kotori --image asia.gcr.io/dmm-fanza/kotori --platform managed --allow-unauthenticated --region asia-northeast1
