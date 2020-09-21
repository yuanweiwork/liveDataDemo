[01LiveData基础之基本用法附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/01Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8B%E5%9F%BA%E6%9C%AC%E7%94%A8%E6%B3%95%E9%99%84%E5%B8%A6demo.md)

[02Lifecycle-LiveData基础之livedata扩展附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/02Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8B%E8%87%AA%E5%AE%9A%E4%B9%89livedata%E9%99%84%E5%B8%A6demo.md)

[03Lifecycle-LiveData基础之map使用附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/03Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8Bmap%E8%BD%AC%E6%8D%A2%E5%92%8CMediatorLiveData%E7%9A%84%E4%BD%BF%E7%94%A8%E9%99%84%E5%B8%A6demo.md)

[04Lifecycle-LiveData基础之配合room 或者配合协程使用附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/04Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8B%E9%85%8D%E5%90%88room%20%E6%88%96%E8%80%85%E9%85%8D%E5%90%88%E5%8D%8F%E7%A8%8B%E4%BD%BF%E7%94%A8%E9%99%84%E5%B8%A6demo.md)

本篇livedata的扩展

主要是 LifecycleOwner 的 Lifecycle.State状态会触发 LiveData 中的 onActive() 和 onInactive() 方法  

像Activity/Fragment 都实现了LifecycleOwner 接口   达到了 生命周期 在不同生命周期 来做不同的事情 

可以先看下官网的介绍 

## [文档](https://developer.android.google.cn/topic/libraries/architecture/livedata?hl=zh_cn#extend_livedata)

我参照写了小的demo比较粗暴

继承类

```kotlin
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
```

activity

``` kotlin
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
  //这里就是比较粗暴的 想获取 State的状态 只是为了看效果
    inner class Owner : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }
    }
}
```

代码

[Demo02](https://github.com/yuanweiwork/liveDataDemo/tree/master)