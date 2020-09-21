package com.yww.demo03

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val count = MutableLiveData<Int>(1)

    //返回一个值就可以
    private val mapCount: LiveData<Int> = Transformations.map(count) {
        it + 1
    }

    //必须返回一个 MutableLiveData 对象
    private val switchMapCount: LiveData<Int> = Transformations.switchMap(mapCount) {
        MutableLiveData<Int>(it!! + 1)
    }

    //可以监听多个值
    private val mediatorLiveData = MediatorLiveData<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        count.observe(this, Observer<Int> {
            count_tv.text = "count=$it"
        })
        mapCount.observe(this, Observer<Int> {
            count_1_tv.text = "mapCount=$it"
        })
        switchMapCount.observe(this, Observer {
            count_2_tv.text = "switchMapCount=$it"
        })


        mediatorLiveData.addSource(mapCount) {
            val s = "mapCount$it"
            Log.e("MainActivity:mapCount--", s)
//            mediatorLiveData.postValue(s)
        }
        mediatorLiveData.addSource(switchMapCount) {
            val s = "switchMapCount$it"
            Log.e("MainActivity:switchMapCount--", s)

//            mediatorLiveData.postValue(s)
        }

        mediatorLiveData.observe(this, Observer {
            count_3_tv.text = "mediatorLiveData=$it"
        })

        add_btn.setOnClickListener {
            count.value = count.value!! + 1
        }
    }
}
