package com.mama1emon.impl.data

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.mama1emon.api.data.Repository
import com.mama1emon.api.data.net.ApiMapper
import com.mama1emon.impl.model.converter.StockListContentConverter
import com.mama1emon.impl.model.converter.StockQuoteContentConverter
import com.mama1emon.impl.model.data.StockQuoteResponse
import com.mama1emon.impl.model.data.StockSetResponse
import com.mama1emon.impl.model.domain.Stock
import com.mama1emon.impl.model.domain.StockQuote
import com.squareup.okhttp.Response
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit

/**
 * Реализация интерфейса репозитория приложения
 *
 * @param apiMapper апи маппер приложения
 *
 * @author Andrey Khokhlov on 26.03.21
 */
class RepositoryImpl(private val apiMapper: ApiMapper) : Repository {
    private val objectMapper = ObjectMapper()

    private val stockListContentConverter = StockListContentConverter()
    private val stockQuoteContentConverter = StockQuoteContentConverter()

    override fun getStockSetContent(): Single<Set<Stock>> {
        return Single.fromCallable {
            val response = apiMapper.requestStockListContent()
            unpackResponse(
                response,
                objectMapper.readValue(response.body().string(), StockSetResponse::class.java)
            ) {
                stockListContentConverter.convert(it)
            }
        }
    }

    override fun getStockQuote(ticker: String): Observable<StockQuote> {
        return Observable
            .interval(INTERVAL_BETWEEN_CALL_IN_SECONDS, TimeUnit.SECONDS)
            .map {
                val response = apiMapper.requestStockQuote(ticker)
                unpackResponse(
                    response,
                    objectMapper.readValue(response.body().string(), StockQuoteResponse::class.java)
                ) {
                    stockQuoteContentConverter.convert(it.apply {
                        this.ticker = ticker
                    })
                }
            }
    }

    private fun <T, R> unpackResponse(response: Response, responseBody: T, converter: (T) -> R): R {
        return if (response.isSuccessful && response.body() != null) {
            converter.invoke(responseBody)
        } else {
            Log.i("unpackResponse()", "Can't unpack response")
            throw Exception()
        }
    }

    companion object {
        const val INTERVAL_BETWEEN_CALL_IN_SECONDS = 5L
    }
}