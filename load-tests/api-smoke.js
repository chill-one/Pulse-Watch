import http from "k6/http";
import { check } from "k6";

export const options = {
  iterations: 10,
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