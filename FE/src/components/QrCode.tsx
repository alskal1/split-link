import { useEffect, useState } from "react";
import QRCode from "qrcode";

interface QrCodeProps {
  value: string;
  size?: number;
}

/**
 * 문자열을 QR 코드 이미지로 렌더링 (외부 API 호출 없이 클라이언트에서 직접 생성)
 * 계좌번호 등 민감정보가 포함된 값을 외부 서버로 보내지 않기 위해 클라이언트 생성 방식 사용
 * @param value QR 코드로 인코딩할 문자열
 * @param size 이미지 한 변의 크기(px)
 */
export default function QrCode({ value, size = 160 }: QrCodeProps) {
  const [dataUrl, setDataUrl] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    QRCode.toDataURL(value, { width: size, margin: 1 })
      .then((url) => {
        if (!cancelled) {
          setDataUrl(url);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDataUrl(null);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [value, size]);

  if (!dataUrl) {
    return null;
  }

  return (
    <img
      src={dataUrl}
      alt="송금 QR 코드"
      width={size}
      height={size}
      className="rounded-[10px]"
    />
  );
}
