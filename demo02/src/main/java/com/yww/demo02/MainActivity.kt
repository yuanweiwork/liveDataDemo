package com.yww.demo02

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val owner: Owner = Owner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        add.setOnClickListener {
            StockLiveData.get("name").observe(owner, Observer<String> {
                // Update the UI.
                message.text = it
            })
            owner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }

        change_state.setOnClickListener {
            owner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }

    inner class Owner : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }
    }
}
