import { NextRequest, NextResponse } from "next/server";

export async function DELETE(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ success: false, message: "요청 형식이 올바르지 않습니다." }, { status: 400 });
  }

  const res = await fetch(`${process.env.BACKEND_INTERNAL_URL}/api/images`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      "X-Internal-Key": process.env.INTERNAL_API_KEY ?? "",
    },
    body: JSON.stringify(body),
  });

  return new NextResponse(null, { status: res.status });
}
