const API_URL =
  process.env.PULSEWATCH_API_URL ?? "http://localhost:8080";

export async function PATCH(
  request: Request,
  {
    params,
  }: {
    params: Promise<{ id: string }>;
  }
) {
  const { id } = await params;
  const body = await request.json();

  const response = await fetch(
    `${API_URL}/monitors/${id}`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    }
  );

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


export async function DELETE(
  request: Request,
  {
    params,
  }: {
    params: Promise<{ id: string }>;
  }
) {
  const { id } = await params;

  const response = await fetch(
    `${API_URL}/monitors/${id}`,
    {
      method: "DELETE",
    }
  );

  if (response.status === 204) {
    return new Response(null, {
      status: 204,
    });
  }

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