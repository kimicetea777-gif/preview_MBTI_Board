import { NextRequest, NextResponse } from "next/server";

export async function DELETE(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ success: false, message: "요청 형식이 올바르지 않습니다." }, { status: 400 });
  }

  const res = await fetch(`${process.env.IMAGE_SERVER_URL}/images/release`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Api-Key": process.env.IMAGE_SERVER_API_KEY ?? "",
    },
    body: JSON.stringify(body),
  });

  return new NextResponse(null, { status: res.status });
}
