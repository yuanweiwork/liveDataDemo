01LiveData基础之基本用法附带demo

### 介绍 及官方文档

> [`LiveData`](https://developer.android.google.cn/reference/androidx/lifecycle/LiveData?hl=zh_cn) 是一种可观察的数据存储器类。与常规的可观察类不同，LiveData 具有生命周期感知能力，意指它遵循其他应用组件（如 Activity、Fragment 或 Service）的生命周期。这种感知能力可确保 LiveData 仅更新处于活跃生命周期状态的应用组件观察者。

[官方文档](https://developer.android.google.cn/topic/libraries/architecture/livedata?hl=zh_cn)

个人理解：livedata 专注于数据  可以通过observe()监听数据的变化(不限于基本的数据类型 对象也可以)  

#### 1.导入

```kotlin
implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.2.0"
```

具体最新版本可以去官网查

#### 2.基本用法

```kotlin
//创建一个key值  "string"为默认值
val key = MutableLiveData("string")

//获取 值
key.getValue()

//key值更改为 "新值"  官方 建议setValue 只在主线程使用
key.setValue("新值")

//key值更改为 "postValue 值"  官方 建议不在主线程时  要使用 postValue 更改值
key.postValue("postValue 值")

//给key 增加监听  当key中的值改变时 则会走回调  observe() 第一个参数  LifecycleOwner 传入 activity/Fragment/Service  
//LifecycleOwner不在本章节讨论  可以看Lifecyc(具体就是标记了各个生命周期)
key.observe(this@MainActivity,
            Observer<String> { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()})

```

#### 3.setValue()和postValue的区别

干了这碗源码

```java

protected void postValue(T value) {
        boolean postTask;
        synchronized (mDataLock) {
            postTask = mPendingData == NOT_SET;
            mPendingData = value;
        }
        if (!postTask) {
            return;
        }
        ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
    }

 @MainThread
    protected void setValue(T value) {
        assertMainThread("setValue");
        mVersion++;
        mData = value;
        dispatchingValue(null);
    }

//可以看到 postvalue 增加 synchronized  线程安全   postvalue 饶了一圈 最后还是调用了 setValue 

//我再测试的时候 分别在 主线程和子线程分别都调用了 postvalue 和 setValue  保证不同时调用的话  子线程 setValue 也是会走入回调中的  具体生产环境 还是 按照官方文档建议走吧

```

#### 4.observe 添加订阅(数据监听器)

```java
 
//ComponentActivity 和 fragment 都实现了这个接口 
public interface LifecycleOwner {
    /**
     * Returns the Lifecycle of the provider.
     *
     * @return The lifecycle of the provider.
     */
    @NonNull
    Lifecycle getLifecycle();
}

//这个是LiveData的订阅部分源码
//可以看到 owner.getLifecycle().getCurrentState() == DESTROYED 就return了  
//DESTROYED 基本标记生命周期销毁了 
public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        assertMainThread("observe");
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        owner.getLifecycle().addObserver(wrapper);
    }



```


[01LiveData基础之基本用法附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/01Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8B%E5%9F%BA%E6%9C%AC%E7%94%A8%E6%B3%95%E9%99%84%E5%B8%A6demo.md)
[02Lifecycle-LiveData基础之自定义livedata附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/02Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8B%E8%87%AA%E5%AE%9A%E4%B9%89livedata%E9%99%84%E5%B8%A6demo.md)
[03Lifecycle-LiveData基础之map使用附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/03Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8Bmap%E4%BD%BF%E7%94%A8%E9%99%84%E5%B8%A6demo.md)
[04Lifecycle-LiveData基础之配合room 或者配合协程使用附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/04Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8B%E9%85%8D%E5%90%88room%20%E6%88%96%E8%80%85%E9%85%8D%E5%90%88%E5%8D%8F%E7%A8%8B%E4%BD%BF%E7%94%A8%E9%99%84%E5%B8%A6demo.md)




这篇就到这里 有新的 我会把链接放到下面来 demo稍后再发