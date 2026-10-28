// Load test for the nearby search: the geohash endpoint against the naive "check every place" endpoint.
//
//   k6 run -e ENDPOINT=nearby       load-test/nearby.js
//   k6 run -e ENDPOINT=nearby-naive load-test/nearby.js
//
// Settings (all optional): BASE_URL, ENDPOINT (nearby | nearby-naive), RADIUS_KM, VUS, DURATION.
// Every request searches from a random point in the dense core of Hyderabad, so a run does not just hit one cached answer.
import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ENDPOINT = __ENV.ENDPOINT || 'nearby';
const RADIUS_KM = __ENV.RADIUS_KM || '2';

// How many places each search returned (both endpoints must return the same numbers).
const placesReturned = new Trend('places_returned');

export const options = {
  vus: Number(__ENV.VUS || 50),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],   // fail the run if more than 1% of requests error
  },
};

export default function () {
  const lat = 17.30 + Math.random() * 0.20;   // 17.30 - 17.50 N
  const lng = 78.30 + Math.random() * 0.25;   // 78.30 - 78.55 E
  // The constant "name" tag keeps k6 from creating one metric series per random URL.
  const res = http.get(`${BASE_URL}/api/places/${ENDPOINT}?lat=${lat}&lng=${lng}&radiusKm=${RADIUS_KM}`,
    { tags: { name: `GET /api/places/${ENDPOINT}` } });
  const ok = check(res, { 'status is 200': (r) => r.status === 200 });
  if (ok) {
    placesReturned.add(res.json().length);
  }
}
