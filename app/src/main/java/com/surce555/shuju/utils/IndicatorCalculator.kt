package com.surce555.shuju.utils

import com.surce555.shuju.data.models.KlineData
import com.surce555.shuju.data.models.PeriodIndicators
import kotlin.math.abs
import kotlin.math.sqrt

object IndicatorCalculator {

    fun calculate(period: String, klines: List<KlineData>): PeriodIndicators {
        val closes = klines.map { it.close }
        val highs = klines.map { it.high }
        val lows = klines.map { it.low }
        val n = closes.size

        val close = closes.last()
        val periodHigh = highs.last()
        val periodLow = lows.last()

        val ema20 = ema(closes, 20)
        val ema50 = ema(closes, 50)
        val ema200 = ema(closes, 200)

        val trendStatus = when {
            ema20 != null && ema50 != null && close > ema20 && ema20 > ema50 -> "多头排列"
            ema20 != null && ema50 != null && close < ema20 && ema20 < ema50 -> "空头排列"
            else -> "震荡缠绕"
        }

        val rsi14 = rsi(closes, 14)

        val (dif, dea, hist) = macd(closes, 12, 26, 9)
        val macdStatus = getMacdStatus(dif, dea, hist)

        val (bollUp, bollMid, bollDn) = boll(closes, 20, 2.0)

        val atr14 = atr(klines, 14)

        val len = minOf(n, 20)
        val swingHigh20 = highs.takeLast(len).max()
        val swingLow20 = lows.takeLast(len).min()
        val len50 = minOf(n, 50)
        val swingHigh50 = highs.takeLast(len50).max()
        val swingLow50 = lows.takeLast(len50).min()

        return PeriodIndicators(
            period = period,
            close = close,
            periodHigh = periodHigh,
            periodLow = periodLow,
            ema20 = ema20,
            ema50 = ema50,
            ema200 = ema200,
            trendStatus = trendStatus,
            rsi14 = rsi14,
            macdDif = dif,
            macdDea = dea,
            macdHist = hist,
            macdStatus = macdStatus,
            bollUp = bollUp,
            bollMid = bollMid,
            bollDn = bollDn,
            atr14 = atr14,
            swingHigh20 = swingHigh20,
            swingLow20 = swingLow20,
            swingHigh50 = swingHigh50,
            swingLow50 = swingLow50
        )
    }

    private fun ema(closes: List<Double>, period: Int): Double? {
        if (closes.size < period) return null
        val k = 2.0 / (period + 1)
        var emaVal = closes.takeLast(closes.size).take(period).average()
        val remaining = closes.drop(period)
        for (c in remaining) {
            emaVal = c * k + emaVal * (1 - k)
        }
        return emaVal
    }

    private fun rsi(closes: List<Double>, period: Int = 14): Double? {
        if (closes.size < period + 1) return null
        val diffs = closes.zipWithNext { a, b -> b - a }
        val gains = diffs.map { if (it > 0) it else 0.0 }
        val losses = diffs.map { if (it < 0) -it else 0.0 }

        var avgGain = gains.take(period).average()
        var avgLoss = losses.take(period).average()

        for (i in period until diffs.size) {
            avgGain = (avgGain * (period - 1) + gains[i]) / period
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period
        }

        return if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
    }

    private fun emaList(closes: List<Double>, period: Int): List<Double> {
        if (closes.size < period) return emptyList()
        val k = 2.0 / (period + 1)
        val result = mutableListOf<Double>()
        var emaVal = closes.take(period).average()
        result.add(emaVal)
        for (i in period until closes.size) {
            emaVal = closes[i] * k + emaVal * (1 - k)
            result.add(emaVal)
        }
        return result
    }

    private fun macd(closes: List<Double>, fastPeriod: Int, slowPeriod: Int, signalPeriod: Int): Triple<Double?, Double?, Double?> {
        if (closes.size < slowPeriod + signalPeriod) return Triple(null, null, null)
        val ema12 = emaList(closes, fastPeriod)
        val ema26 = emaList(closes, slowPeriod)
        val minLen = minOf(ema12.size, ema26.size)
        if (minLen == 0) return Triple(null, null, null)
        val ema12Tail = ema12.takeLast(minLen)
        val ema26Tail = ema26.takeLast(minLen)
        val difList = ema12Tail.zip(ema26Tail) { a, b -> a - b }
        if (difList.size < signalPeriod) return Triple(null, null, null)
        val deaList = emaList(difList, signalPeriod)
        if (deaList.isEmpty()) return Triple(null, null, null)
        val dif = difList.last()
        val dea = deaList.last()
        val hist = (dif - dea) * 2
        return Triple(dif, dea, hist)
    }

    private fun getMacdStatus(dif: Double?, dea: Double?, hist: Double?): String {
        if (dif == null || dea == null || hist == null) return "数据不足"
        return when {
            dif > 0 && dea > 0 && dif > dea && hist > 0 -> "零轴上金叉"
            dif > 0 && dea > 0 && dif < dea && hist < 0 -> "零轴上死叉"
            dif < 0 && dea < 0 && dif > dea && hist > 0 -> "零轴下金叉"
            dif < 0 && dea < 0 && dif < dea && hist < 0 -> "零轴下死叉"
            dif > 0 && hist > 0 -> "多头放量"
            dif > 0 && hist < 0 -> "多头衰减"
            dif < 0 && hist < 0 -> "空头放量"
            dif < 0 && hist > 0 -> "空头衰减"
            else -> "震荡"
        }
    }

    private fun boll(closes: List<Double>, period: Int, multiplier: Double): Triple<Double?, Double?, Double?> {
        if (closes.size < period) return Triple(null, null, null)
        val slice = closes.takeLast(period)
        val mid = slice.average()
        val variance = slice.map { (it - mid) * (it - mid) }.average()
        val stddev = sqrt(variance)
        return Triple(mid + multiplier * stddev, mid, mid - multiplier * stddev)
    }

    private fun atr(klines: List<KlineData>, period: Int): Double? {
        if (klines.size < period + 1) return null
        val trList = klines.zipWithNext { prev, curr ->
            maxOf(
                curr.high - curr.low,
                abs(curr.high - prev.close),
                abs(curr.low - prev.close)
            )
        }
        if (trList.size < period) return null
        var atrVal = trList.take(period).average()
        for (i in period until trList.size) {
            atrVal = (atrVal * (period - 1) + trList[i]) / period
        }
        return atrVal
    }
}
