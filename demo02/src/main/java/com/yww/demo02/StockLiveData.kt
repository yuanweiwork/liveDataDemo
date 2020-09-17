package com.yww.demo02

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData

class StockLiveData(symbol: String) : LiveData<String>() {
    private val stockManager = StockManager(symbol)

    private val listener = object : SimplePriceListener {
        override fun sendMessage(message: String) {
            postValue(message)
        }
    }

    override fun onActive() {
        stockManager.requestPriceUpdates(listener)
    }

    override fun onInactive() {
        stockManager.removeUpdates(listener)
    }

    companion object {
        private lateinit var sInstance: StockLiveData

        @MainThread
        fun get(symbol: String): StockLiveData {
            sInstance = if (::sInstance.isInitialized) sInstance else StockLiveData(symbol)
            return sInstance
        }
    }
}