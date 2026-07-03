/** 만 원 단위 금액을 읽기 쉬운 문자열로 변환 */
export function formatAmount(manwon: number): string {
  if (manwon >= 10000) {
    const eok = manwon / 10000;
    return `${eok % 1 === 0 ? eok.toLocaleString("ko-KR") : eok.toLocaleString("ko-KR", { maximumFractionDigits: 2 })}억 원`;
  }
  return `${manwon.toLocaleString("ko-KR")}만 원`;
}

export function formatRatio(ratio: number): string {
  return `${ratio % 1 === 0 ? ratio : ratio.toFixed(2)}%`;
}
