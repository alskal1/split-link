/**
 * 모바일 기기(터치 기반 브라우저) 여부 판별
 * 데스크톱 웹에서는 토스(supertoss://) 딥링크를 실행할 앱이 없어 QR코드 대안이 필요하므로 사용
 */
export const isMobileDevice = (): boolean => {
  if (typeof navigator === "undefined") {
    return false;
  }

  return /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
};
