package com.yww.demo01

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val key = MutableLiveData("string")
    var isMain = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 添加监听 数据一旦发生变更会走入回调中
        key.observe(this,
            Observer<String> { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() })

        add_btn.setOnClickListener {
            if (isMain) {
                key.value = "主线程调用"
            } else {
                Thread(Runnable {
                    key.postValue("子线程调用更换")
                }).run()
            }
            isMain = !isMain
        }

        edit_et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                key.value = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }
}
