package com.yww.demo02

import kotlin.concurrent.thread

class StockManager(symbol: String) {
    var listener: SimplePriceListener? = null
    var count = 1

    init {
        thread(start = true) {
            while (true) {
                count++
                Thread.sleep(1000)
                listener?.sendMessage("$symbol:信息${count}")
            }
        }
    }


    fun requestPriceUpdates(l: SimplePriceListener) {
        listener = l
    }

    fun removeUpdates(l: SimplePriceListener) {
        listener = null
    }
}
