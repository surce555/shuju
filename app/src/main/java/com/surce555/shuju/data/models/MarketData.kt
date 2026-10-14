package com.surce555.shuju.data.models

data class KlineData(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class PeriodIndicators(
    val period: String,
    val close: Double,
    val periodHigh: Double,
    val periodLow: Double,
    val ema20: Double?,
    val ema50: Double?,
    val ema200: Double?,
    val trendStatus: String,
    val rsi14: Double?,
    val macdDif: Double?,
    val macdDea: Double?,
    val macdHist: Double?,
    val macdStatus: String,
    val bollUp: Double?,
    val bollMid: Double?,
    val bollDn: Double?,
    val atr14: Double?,
    val swingHigh20: Double,
    val swingLow20: Double,
    val swingHigh50: Double,
    val swingLow50: Double
)

data class CoinData(
    val symbol: String,
    val currentPrice: Double,
    val change24h: Double,
    val fundingRate: Double?,
    val periods: Map<String, PeriodIndicators>
)

data class MarketSnapshot(
    val timestamp: Long,
    val btc: CoinData?,
    val eth: CoinData?,
    val ethBtcRatio: Double?,
    val dataSource: String,
    val error: String? = null
)

enum class DataSource(val displayName: String) {
    BINANCE_VISION("Binance镜像(推荐)"),
    BINANCE_MAIN("Binance主站"),
    OKX("OKX")
}
