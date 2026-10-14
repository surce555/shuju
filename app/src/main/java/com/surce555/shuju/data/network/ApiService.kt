package com.surce555.shuju.data.network

import com.surce555.shuju.data.models.DataSource
import com.surce555.shuju.data.models.KlineData
import com.surce555.shuju.utils.AppLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val binancePeriods = listOf("1M", "1w", "3d", "1d", "12h", "6h", "4h", "1h")
    private val okxPeriodMap = mapOf(
        "1M" to "1M", "1w" to "1W", "3d" to "3D", "1d" to "1D",
        "12h" to "12H", "6h" to "6H", "4h" to "4H", "1h" to "1H"
    )
    private val okxSymbolMap = mapOf(
        "BTCUSDT" to "BTC-USDT",
        "ETHUSDT" to "ETH-USDT"
    )

    fun fetchKlines(symbol: String, interval: String, dataSource: DataSource): List<KlineData>? {
        return when (dataSource) {
            DataSource.BINANCE_VISION -> fetchBinanceKlines(
                "https://data-api.binance.vision/api/v3/klines", symbol, interval
            ) ?: fetchBinanceKlines("https://api.binance.com/api/v3/klines", symbol, interval)
                ?: fetchOkxKlines(symbol, interval)

            DataSource.BINANCE_MAIN -> fetchBinanceKlines(
                "https://api.binance.com/api/v3/klines", symbol, interval
            ) ?: fetchBinanceKlines(
                "https://data-api.binance.vision/api/v3/klines", symbol, interval
            ) ?: fetchOkxKlines(symbol, interval)

            DataSource.OKX -> fetchOkxKlines(symbol, interval)
                ?: fetchBinanceKlines("https://data-api.binance.vision/api/v3/klines", symbol, interval)
        }
    }

    private fun fetchBinanceKlines(baseUrl: String, symbol: String, interval: String): List<KlineData>? {
        return try {
            val url = "$baseUrl?symbol=$symbol&interval=$interval&limit=250"
            AppLogger.log("[API] GET $url")
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                AppLogger.log("[API] Failed $url -> HTTP ${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val arr = JSONArray(body)
            val result = mutableListOf<KlineData>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONArray(i)
                result.add(
                    KlineData(
                        openTime = item.getLong(0),
                        open = item.getString(1).toDouble(),
                        high = item.getString(2).toDouble(),
                        low = item.getString(3).toDouble(),
                        close = item.getString(4).toDouble(),
                        volume = item.getString(5).toDouble()
                    )
                )
            }
            AppLogger.log("[API] OK $url -> ${result.size} klines")
            result
        } catch (e: Exception) {
            AppLogger.log("[API] Exception fetching Binance $baseUrl $symbol $interval: ${e.message}")
            null
        }
    }

    private fun fetchOkxKlines(symbol: String, interval: String): List<KlineData>? {
        return try {
            val okxSymbol = okxSymbolMap[symbol] ?: symbol
            val okxInterval = okxPeriodMap[interval] ?: interval
            val url = "https://www.okx.com/api/v5/market/candles?instId=$okxSymbol&bar=$okxInterval&limit=250"
            AppLogger.log("[API] GET $url")
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                AppLogger.log("[API] Failed $url -> HTTP ${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val dataArr = json.getJSONArray("data")
            val result = mutableListOf<KlineData>()
            // OKX returns newest first, so reverse
            for (i in dataArr.length() - 1 downTo 0) {
                val item = dataArr.getJSONArray(i)
                result.add(
                    KlineData(
                        openTime = item.getString(0).toLong(),
                        open = item.getString(1).toDouble(),
                        high = item.getString(2).toDouble(),
                        low = item.getString(3).toDouble(),
                        close = item.getString(4).toDouble(),
                        volume = item.getString(5).toDouble()
                    )
                )
            }
            AppLogger.log("[API] OK $url -> ${result.size} klines")
            result
        } catch (e: Exception) {
            AppLogger.log("[API] Exception fetching OKX $symbol $interval: ${e.message}")
            null
        }
    }

    fun fetchFundingRate(symbol: String): Double? {
        // Try Binance FAPI first
        return try {
            val url = "https://fapi.binance.com/fapi/v1/premiumIndex?symbol=$symbol"
            AppLogger.log("[API] FundingRate GET $url")
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return tryOkxFundingRate(symbol)
            val body = response.body?.string() ?: return tryOkxFundingRate(symbol)
            val json = JSONObject(body)
            val rate = json.optString("lastFundingRate", "").toDoubleOrNull()
            AppLogger.log("[API] FundingRate $symbol -> $rate")
            rate?.times(100)
        } catch (e: Exception) {
            AppLogger.log("[API] FundingRate exception: ${e.message}")
            tryOkxFundingRate(symbol)
        }
    }

    private fun tryOkxFundingRate(binanceSymbol: String): Double? {
        return try {
            val okxSymbol = okxSymbolMap[binanceSymbol] ?: return null
            val instId = okxSymbol + "-SWAP"
            val url = "https://www.okx.com/api/v5/public/funding-rate?instId=$instId"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val data = json.getJSONArray("data")
            if (data.length() == 0) return null
            val rate = data.getJSONObject(0).optString("fundingRate", "").toDoubleOrNull()
            rate?.times(100)
        } catch (e: Exception) {
            null
        }
    }

    fun fetch24hChange(symbol: String): Double? {
        return try {
            val url = "https://api.binance.com/api/v3/ticker/24hr?symbol=$symbol"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return fetchVisionChange(symbol)
            val body = response.body?.string() ?: return fetchVisionChange(symbol)
            val json = JSONObject(body)
            json.optString("priceChangePercent", "").toDoubleOrNull()
        } catch (e: Exception) {
            fetchVisionChange(symbol)
        }
    }

    private fun fetchVisionChange(symbol: String): Double? {
        return try {
            val url = "https://data-api.binance.vision/api/v3/ticker/24hr?symbol=$symbol"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            json.optString("priceChangePercent", "").toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
