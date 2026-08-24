import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    stages: [
    { duration: "10s", target: 100 },
    { duration: "30s", target: 100 },
    { duration: "10s", target: 0 },
    ],

  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<200"],
  },
};

export default function () {
  const response = http.get(
    "http://localhost:8080/monitors"
  );

  check(response, {
    "GET /monitors returns 200": (res) =>
      res.status === 200,
  });

  sleep(1);
}