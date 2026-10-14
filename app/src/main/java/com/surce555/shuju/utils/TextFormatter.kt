package com.surce555.shuju.utils

import com.surce555.shuju.data.models.CoinData
import com.surce555.shuju.data.models.MarketSnapshot
import java.text.SimpleDateFormat
import java.util.*

object TextFormatter {
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val displayPeriods = listOf("1M", "1w", "3d", "1d", "12h", "6h", "4h", "1h")
    private val periodDisplayMap = mapOf(
        "1M" to "1M", "1w" to "1W", "3d" to "3D", "1d" to "1D",
        "12h" to "12H", "6h" to "6H", "4h" to "4H", "1h" to "1H"
    )

    fun format(snapshot: MarketSnapshot): String {
        val sb = StringBuilder()
        val timeStr = sdf.format(Date(snapshot.timestamp))

        sb.appendLine("请使用 crypto-mtf-analyzer 技能分析今日虚拟币大盘，以下为最新实盘 8 周期指标数据（采集时间：$timeStr）：")
        sb.appendLine()
        sb.appendLine("【市场概览】")

        val btc = snapshot.btc
        val eth = snapshot.eth

        if (btc != null) {
            val fundingStr = if (btc.fundingRate != null) "%.4f".format(btc.fundingRate) else "N/A"
            sb.appendLine("- BTCUSDT 现价：${"%,.2f".format(btc.currentPrice)} (24h: ${"%.2f".format(btc.change24h)}%) | 资金费率：${fundingStr}%")
        }
        if (eth != null) {
            val fundingStr = if (eth.fundingRate != null) "%.4f".format(eth.fundingRate) else "N/A"
            sb.appendLine("- ETHUSDT 现价：${"%,.2f".format(eth.currentPrice)} (24h: ${"%.2f".format(eth.change24h)}%) | 资金费率：${fundingStr}%")
        }
        val ethBtcStr = if (snapshot.ethBtcRatio != null) "%.6f".format(snapshot.ethBtcRatio) else "N/A"
        sb.appendLine("- ETH/BTC 汇率：$ethBtcStr")
        sb.appendLine()

        if (btc != null) {
            sb.appendLine("【BTCUSDT 八周期实盘数据】")
            sb.appendLine(formatCoinTable(btc))
        }
        sb.appendLine()
        if (eth != null) {
            sb.appendLine("【ETHUSDT 八周期实盘数据】")
            sb.appendLine(formatCoinTable(eth))
        }
        sb.appendLine()
        sb.appendLine("请根据以上 8 周期数据执行顶底共振分析，给出今日大盘做多/做空建议，并严格计算出 BTC 和 ETH 的头仓（35%）、加仓（65%）、综合均价、止损点位（结合ATR）、第一止盈（TP1）和第二止盈（TP2）。")
        sb.appendLine()
        sb.appendLine("数据来源：${snapshot.dataSource}")

        return sb.toString()
    }

    private fun formatCoinTable(coin: CoinData): String {
        val sb = StringBuilder()
        sb.appendLine("| 周期 | 趋势状态 | 现价 | EMA20 / EMA50 / EMA200 | RSI(14) | MACD(DIF/DEA/Hist状态) | BOLL(上/中/下轨) | ATR(14) | 近20K高/低点 |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|")

        for (period in displayPeriods) {
            val p = coin.periods[period]
            val displayPeriod = periodDisplayMap[period] ?: period
            if (p == null) {
                sb.appendLine("| $displayPeriod | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A |")
                continue
            }
            val close = "%,.2f".format(p.close)
            val ema20s = p.ema20?.let { "%,.2f".format(it) } ?: "N/A"
            val ema50s = p.ema50?.let { "%,.2f".format(it) } ?: "N/A"
            val ema200s = p.ema200?.let { "%,.2f".format(it) } ?: "N/A"
            val rsis = p.rsi14?.let { "%.1f".format(it) } ?: "N/A"
            val difs = p.macdDif?.let { "%.2f".format(it) } ?: "N/A"
            val deas = p.macdDea?.let { "%.2f".format(it) } ?: "N/A"
            val hists = p.macdHist?.let { "%.2f".format(it) } ?: "N/A"
            val macdStr = "$difs/$deas/$hists ${p.macdStatus}"
            val bollUps = p.bollUp?.let { "%,.2f".format(it) } ?: "N/A"
            val bollMids = p.bollMid?.let { "%,.2f".format(it) } ?: "N/A"
            val bollDns = p.bollDn?.let { "%,.2f".format(it) } ?: "N/A"
            val bollStr = "$bollUps/$bollMids/$bollDns"
            val atrs = p.atr14?.let { "%,.2f".format(it) } ?: "N/A"
            val sh20 = "%,.2f".format(p.swingHigh20)
            val sl20 = "%,.2f".format(p.swingLow20)
            val swing20 = "$sh20/$sl20"

            sb.appendLine("| $displayPeriod | ${p.trendStatus} | $close | $ema20s / $ema50s / $ema200s | $rsis | $macdStr | $bollStr | $atrs | $swing20 |")
        }
        return sb.toString()
    }
}
