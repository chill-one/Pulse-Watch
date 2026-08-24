import http from "k6/http";
import { check } from "k6";

const rate = Number(__ENV.RATE || 500);
const duration = __ENV.DURATION || "30s";

export const options = {
  scenarios: {
    throughput: {
      executor: "constant-arrival-rate",
      rate: rate,
      timeUnit: "1s",
      duration: duration,

      preAllocatedVUs: 500,
      maxVUs: 1500,
    },
  },

  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<200"],
    dropped_iterations: ["count==0"],
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
}