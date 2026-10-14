package com.surce555.shuju.data.repository

import com.surce555.shuju.data.models.*
import com.surce555.shuju.data.network.ApiService
import com.surce555.shuju.utils.IndicatorCalculator
import com.surce555.shuju.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

object MarketRepository {
    private val periods = listOf("1M", "1w", "3d", "1d", "12h", "6h", "4h", "1h")

    suspend fun fetchMarketSnapshot(dataSource: DataSource): MarketSnapshot = coroutineScope {
        AppLogger.log("[Repo] Start fetchMarketSnapshot source=${dataSource.displayName}")
        val btcPeriodsDeferred = async(Dispatchers.IO) { fetchAllPeriods("BTCUSDT", dataSource) }
        val ethPeriodsDeferred = async(Dispatchers.IO) { fetchAllPeriods("ETHUSDT", dataSource) }
        val btcFundingDeferred = async(Dispatchers.IO) { safeGet { ApiService.fetchFundingRate("BTCUSDT") } }
        val ethFundingDeferred = async(Dispatchers.IO) { safeGet { ApiService.fetchFundingRate("ETHUSDT") } }
        val btcChangeDeferred = async(Dispatchers.IO) { safeGet { ApiService.fetch24hChange("BTCUSDT") } }
        val ethChangeDeferred = async(Dispatchers.IO) { safeGet { ApiService.fetch24hChange("ETHUSDT") } }

        val btcPeriods = btcPeriodsDeferred.await()
        val ethPeriods = ethPeriodsDeferred.await()
        val btcFunding = btcFundingDeferred.await()
        val ethFunding = ethFundingDeferred.await()
        val btcChange = btcChangeDeferred.await()
        val ethChange = ethChangeDeferred.await()

        val btcCurrentPrice = btcPeriods["1h"]?.close ?: btcPeriods.values.firstOrNull()?.close ?: 0.0
        val ethCurrentPrice = ethPeriods["1h"]?.close ?: ethPeriods.values.firstOrNull()?.close ?: 0.0
        val ethBtcRatio = if (btcCurrentPrice > 0 && ethCurrentPrice > 0) ethCurrentPrice / btcCurrentPrice else null

        val btcData = CoinData(
            symbol = "BTCUSDT",
            currentPrice = btcCurrentPrice,
            change24h = btcChange ?: 0.0,
            fundingRate = btcFunding,
            periods = btcPeriods
        )
        val ethData = CoinData(
            symbol = "ETHUSDT",
            currentPrice = ethCurrentPrice,
            change24h = ethChange ?: 0.0,
            fundingRate = ethFunding,
            periods = ethPeriods
        )

        AppLogger.log("[Repo] Snapshot complete BTC=${btcCurrentPrice} ETH=${ethCurrentPrice}")
        MarketSnapshot(
            timestamp = System.currentTimeMillis(),
            btc = btcData,
            eth = ethData,
            ethBtcRatio = ethBtcRatio,
            dataSource = dataSource.displayName
        )
    }

    private suspend fun fetchAllPeriods(symbol: String, dataSource: DataSource): Map<String, PeriodIndicators> =
        coroutineScope {
            val deferreds = periods.map { period ->
                period to async(Dispatchers.IO) {
                    try {
                        val klines = ApiService.fetchKlines(symbol, period, dataSource)
                        if (klines != null && klines.isNotEmpty()) {
                            IndicatorCalculator.calculate(period, klines)
                        } else {
                            AppLogger.log("[Repo] No klines for $symbol $period")
                            null
                        }
                    } catch (e: Exception) {
                        AppLogger.log("[Repo] Error $symbol $period: ${e.message}")
                        null
                    }
                }
            }
            deferreds.mapNotNull { (period, deferred) ->
                val result = deferred.await()
                if (result != null) period to result else null
            }.toMap()
        }

    private suspend fun <T> safeGet(block: suspend () -> T?): T? = withContext(Dispatchers.IO) {
        try { block() } catch (e: Exception) { null }
    }
}
