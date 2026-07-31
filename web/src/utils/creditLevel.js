// 信用分工具：兜底 / 等级 / 配色（阈值与后端 CreditConstant 一致）
export function creditScore(value) {
    return value == null ? 60 : Number(value)
}

export function creditLevel(value) {
    const s = creditScore(value)
    if (s < 60) return '待提升'
    if (s < 80) return '良好'
    return '优秀'
}

export function creditColor(value) {
    const s = creditScore(value)
    if (s < 60) return 'info'
    if (s < 80) return 'primary'
    return 'success'
}
