const API_URL =
  process.env.PULSEWATCH_API_URL ?? "http://localhost:8080";

export async function POST(request: Request) {
  const body = await request.json();

  const response = await fetch(`${API_URL}/monitors`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  const responseBody = await response.text();

  return new Response(responseBody, {
    status: response.status,
    headers: {
      "Content-Type":
        response.headers.get("content-type") ??
        "application/json",
    },
  });
}