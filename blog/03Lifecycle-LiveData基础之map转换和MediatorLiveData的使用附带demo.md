[01LiveData基础之基本用法附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/01Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8B%E5%9F%BA%E6%9C%AC%E7%94%A8%E6%B3%95%E9%99%84%E5%B8%A6demo.md)
[02Lifecycle-LiveData基础之livedata扩展附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/02Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8B%E8%87%AA%E5%AE%9A%E4%B9%89livedata%E9%99%84%E5%B8%A6demo.md)
[03Lifecycle-LiveData基础之map使用附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/03Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8Bmap%E4%BD%BF%E7%94%A8%E9%99%84%E5%B8%A6demo.md)
[04Lifecycle-LiveData基础之配合room 或者配合协程使用附带demo](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/04Lifecycle-LiveData%E5%9F%BA%E7%A1%80%E4%B9%8B%E9%85%8D%E5%90%88room%20%E6%88%96%E8%80%85%E9%85%8D%E5%90%88%E5%8D%8F%E7%A8%8B%E4%BD%BF%E7%94%A8%E9%99%84%E5%B8%A6demo.md)

## 本篇两个内容 简介

### 1.Transformations 中 map（） 和 switch（） 方法

### 2.MediatorLiveData 的使用

### map() switchMap()区别和使用

Livedata 附带Transformations 类 提供了转换功能  例如网络数据回传后的类型转换  

[`Transformations.map()`](https://developer.android.google.cn/reference/androidx/lifecycle/Transformations?hl=zh_cn#map(android.arch.lifecycle.LiveData, android.arch.core.util.Function))

[`Transformations.switchMap()`](https://developer.android.google.cn/reference/androidx/lifecycle/Transformations?hl=zh_cn#switchMap(android.arch.lifecycle.LiveData, android.arch.core.util.Function>))

##### 使用与区别

```kotlin
//Transformations.map()
//被监听数据
 private val count = MutableLiveData<Int>(1)

 //监听 count  如果它变动   则会触发监听 并赋值给 mapCount 
 private val mapCount: LiveData<Int> = Transformations.map()(count) {
   //返回一个值就可以
        it + 1
 }

 //监听 count  如果它变动   则会触发监听 并赋值给 switchMapCount
 //与map()的区别是必须返回一个 MutableLiveData 对象
 private val switchMapCount: LiveData<Int> = Transformations.switchMap(mapCount) {
        MutableLiveData<Int>(it!! + 1)
 }
```



#### MediatorLiveData的使用

```kotlin
 //可以监听多个值
 private val mediatorLiveData = MediatorLiveData<String>()

//使用
 mediatorLiveData.addSource(mapCount) （ 
        val s = "mapCount$it"
        Log.e("MainActivity:mapCount--", s)
//      mediatorLiveData.postValue(s)
}
 mediatorLiveData.addSource(switchMapCount) {   
        val s = "switchMapCount$it"
        Log.e("MainActivity:switchMapCount--", s)
//      mediatorLiveData.postValue(s)
}
```

注意点：

1.mapCount 和switchMapCount 值的变动 会触发各自addSource监听的回调

2.addSource 不会赋值给mediatorLiveData  需要手动调用 setValue方法

最后  推荐看下 大佬文章  我翻译了一下  [连接](https://github.com/yuanweiwork/liveDataDemo/blob/master/blog/%E8%AF%91%E6%96%87%20%20ViewModel%E4%B9%8B%E5%A4%96%E7%9A%84LiveData-%E4%BD%BF%E7%94%A8Transformations%E5%92%8CMediatorLiveData%E7%9A%84%E5%8F%8D%E5%BA%94%E6%A8%A1%E5%BC%8F.md)

本章[代码demo03](https://github.com/yuanweiwork/liveDataDemo)







