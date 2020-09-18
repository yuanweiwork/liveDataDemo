作者 ：[Jose Alcérreca](https://medium.com/@JoseAlcerreca?source=post_page-----fda520ba00b7--------------------------------)

[原文链接](https://medium.com/androiddevelopers/livedata-beyond-the-viewmodel-reactive-patterns-using-transformations-and-mediatorlivedata-fda520ba00b7)

多年来，反应式架构一直是Android中的热门话题。在Android会议上，它一直是一个不变的主题，通常使用RxJava示例进行说明（请参阅底部的Rx部分）。响应式编程是一种与数据流和更改传播有关的范例，它可以简化构建应用程序和显示来自异步操作的数据。

[LiveData](https://developer.android.com/topic/libraries/architecture/livedata)是实现某些反应性概念的一种工具。这是一个简单的可观察者，它了解观察者的生命周期。从数据源或存储库中公开LiveData是使您的体系结构更具响应性的简单方法，但存在一些潜在的陷阱。

这篇博客文章将帮助您避免陷阱，并使用一些模式来帮助您使用LiveData构建更具反应性的体系结构。

# LiveData的目的

在Android中，activities, fragments 和 views 几乎可以随时销毁，因此任何对这些组件之一的引用都可能导致泄漏或`NullPointerException`。

LiveData旨在实现观察者模式，从而允许View （activities, fragments, 等）与UI数据源（通常为ViewModel）之间进行通信。使用LiveData，使种通信更加安全：由于具有生命周期意识，因此只有处于活动状态的View才能接收数据。

简而言之，优点是您不需要手动取消View和ViewModel之间的订阅。

​	*View-ViewModel交互*

![图片发布](https://raw.githubusercontent.com/yuanweiwork/liveDataDemo/master/blog/img/1_aMu72YHtOdLMG3jc4qSoCQ.png)



# 超越ViewModel的LiveData

可观察的范例在View控制器和ViewModel之间确实很好地工作，因此您可以使用它来观察应用程序的其他组件并利用生命周期意识。例如：

- 观察[SharedPreferences中的](https://developer.android.com/reference/android/content/SharedPreferences.OnSharedPreferenceChangeListener)更改
- 在[Firestore中](https://firebase.google.com/docs/firestore/)观察文档或集合
- 使用[FirebaseAuth](https://firebase.google.com/docs/auth/)等身份验证SDK观察当前用户
- 观察[Room中](https://developer.android.com/topic/libraries/architecture/room)的查询（开箱即用地支持LiveData）

这种范例的优势在于，因为所有内容都连接在一起，所以当数据更改时，UI会自动更新。

缺点是LiveData不像Rx那样带有用于组合数据流或管理线程的工具包。

在典型应用的每一层中使用LiveData看起来像这样：

![图片发布](https://raw.githubusercontent.com/yuanweiwork/liveDataDemo/master/blog/img/1_QXXiuXmzRTTqdaEEojyIcQ.png)

*使用LiveData的典型应用程序架构*

为了在组件之间传递数据，我们需要一种映射和组合的方法。为此，将MediatorLiveData与Transformations类中的帮助器结合使用：

- [Transformations.map](https://developer.android.com/reference/android/arch/lifecycle/Transformations.html#map(android.arch.lifecycle.LiveData, android.arch.core.util.Function))
- [Transformations.switchMap](https://developer.android.com/reference/android/arch/lifecycle/Transformations.html#switchMap(android.arch.lifecycle.LiveData, android.arch.core.util.Function>))

> 请注意，在销毁View后，您无需拆除这些订阅，因为View的生命周期会向下游传播到后续订阅。

# **模式**

## **一对一 ** transformation-map



![图片发布](https://raw.githubusercontent.com/yuanweiwork/liveDataDemo/master/blog/img/1_3FkrCCJEhV5dW6kJU9AUog.png)

*ViewModel观察一种数据类型并公开另一种数据类型*

在上面的示例中，ViewModel仅将数据从存储库转发到视图，然后将其转换为UI模型。每当存储库中有新数据时，ViewModel都必须拥有更新它：

```kotlin
class MainViewModel {
  val viewModelResult = Transformations.map(repository.getDataForUser()) { data ->
     convertDataToMainUIModel(data)
  }
}
```

这种转换非常简单。但是，如果用户可能会更改，则需要switchMap：

## 一对一  动态  transformation-switchMap

以下示例：您正在观察一个暴露用户的用户管理器，并且需要等待他们的ID后才能开始观察存储库。

![图片发布](https://raw.githubusercontent.com/yuanweiwork/liveDataDemo/master/blog/img/1_nMHVwTGSFSSR9ooHL8TxFg.png)

*用户管理器提供暴露结果之前存储库所需的用户ID*

您无法在ViewModel初始化时进行连接，因为用户ID不会立即可用。

您可以使用`switchMap`来实现。

```kotlin
class MainViewModel {
  val repositoryResult = Transformations.switchMap(userManager.user) { user ->
     repository.getDataForUser(user)
  }
}
```

 `switchMap`在内部使用MediatorLiveData，需要熟练使用它，因为当您要组合多个LiveData来源时需要使用它：

## 一对多  MediatorLiveData

MediatorLiveData使您可以将一个或多个数据源添加到单个可观察的LiveData中。

```kotlin

val liveData1: LiveData<Int> = ...
val liveData2: LiveData<Int> = ...

val result = MediatorLiveData<Int>()

result.addSource(liveData1) { value ->
    result.setValue(value)
}
result.addSource(liveData2) { value ->
    result.setValue(value)
}
```

这个例子来自于文档，当任何源代码更改时更新结果。注意，这些数据没有为您合并。MediatorLiveData只负责通知。

为了在示例应用程序中实现转换，我们需要将两个不同的LiveData组合为一个：



![图片发布](https://raw.githubusercontent.com/yuanweiwork/liveDataDemo/master/blog/img/1_-Ymwmo3w4gLoc__pfAGc2g.png)

*MediatorLiveData用于合并两个数据源*

使用MediatorLiveData合并数据的一种方法是添加源并以其他方法设置值：

```kotlin
fun blogpostBoilerplateExample(newUser: String): LiveData<UserDataResult> {

    val liveData1 = userOnlineDataSource.getOnlineTime(newUser)
    val liveData2 = userCheckinsDataSource.getCheckins(newUser)

    val result = MediatorLiveData<UserDataResult>()

    result.addSource(liveData1) { value ->
        result.value = combineLatestData(liveData1, liveData2)
    }
    result.addSource(liveData2) { value ->
        result.value = combineLatestData(liveData1, liveData2)
    }
    return result
}
```

数据的实际组合是通过该`combineLatestData`方法完成的。

```kotlin

private fun combineLatestData(
        onlineTimeResult: LiveData<Long>,
        checkinsResult: LiveData<CheckinsResult>
): UserDataResult {

    val onlineTime = onlineTimeResult.value
    val checkins = checkinsResult.value

    // Don't send a success until we have both results
    if (onlineTime == null || checkins == null) {
        return UserDataLoading()
    }

    // TODO: Check for errors and return UserDataError if any.

    return UserDataSuccess(timeOnline = onlineTime, checkins = checkins)
}
```

它检查值是否已准备好或正确，并发出结果（*loading*, *error* or *success*）

请参阅下面的附加部分，了解如何使用Kotlin的扩展函数来清除这些问题。

# 何时不使用LiveData

即使您想“go reactive”，也需要在将LiveData添加到各处之前了解其优势。如果您的应用程序组件没有与UI的连接，则可能不需要LiveData。

例如，您应用中的用户管理器会监听您的身份验证提供程序中的更改（例如Firebase Auth），并将唯一令牌上载到您的服务器。

![图片发布](https://raw.githubusercontent.com/yuanweiwork/liveDataDemo/master/blog/img/1_w-XMAsDiCgpjUBEFi8SHvA.png)

*令牌上传者和用户管理者之间的互动应该是被动的吗？*

令牌上载者可以观察用户管理器，但具有谁的生命周期？此操作与视图完全无关。此外，如果视图被破坏，则用户令牌可能永远不会被上传。

另一种选择是使用令牌上传器中的[observeForever](https://developer.android.com/reference/android/arch/lifecycle/LiveData#observeforever)（），并以某种方式挂接到用户管理器的生命周期中，以在完成后删除订阅。

但是，您无需使所有内容均可观察。让用manager直接token上传（或在您的体系结构中有意义的任何调用）。

![图片发布](https://raw.githubusercontent.com/yuanweiwork/liveDataDemo/master/blog/img/1_u2dKCA0uWtS2k7zNGgBumw.png)

*与UI不相关的操作不需要使用LiveData*

> *If part of your app doesn’t affect the UI, you probably don’t need LiveData.*

# 反模式：共享LiveData实例

当一个类将LiveData公开给其他类时，请仔细考虑是否要公开相同或不同的LiveData实例。

```kotlin

class SharedLiveDataSource(val dataSource: MyDataSource) {

    // Caution: this LiveData is shared across consumers
    private val result = MutableLiveData<Long>()

    fun loadDataForUser(userId: String): LiveData<Long> {
        result.value = dataSource.getOnlineTime(userId)
        return result
    }
}
```

如果此类在您的应用中是单例（只有一个实例），则您始终可以返回相同的LiveData，对吗？不一定：此类可能有多个使用者。

例如，考虑以下一个：

```kotlin
sharedLiveDataSource.loadDataForUser("1").observe(this, Observer {
   // Show result on screen
}) 
```

第二个消费者也使用它：

```kotlin
sharedLiveDataSource.loadDataForUser("2").observe(this, Observer {
   // Show result on screen
}) 
```

第一消费者将接收具有属于用户“ 2”的数据的更新。

即使您认为您只从一个消费者那里使用这个类，使用这个模式也可能会导致错误。例如，当从一个活动的一个实例	导航到另一个实例时，**新实例可能暂时从前一个实例接收数据**。请记住，LiveData将最新值分配给新的观察者。此外，在Lollipop中引入了活动转换，它们带来了一个有趣的优势案例：**两个activity处于活动状态**。这意味着可能有两个LiveData唯一使用者的实例，其中一个实例可能会显示错误的数据。	

解决此问题的方法只是为每个使用者返回一个新的LiveData。

```kotlin
class SharedLiveDataSource(val dataSource: MyDataSource) {
    fun loadDataForUser(userId: String): LiveData<Long> {
        val result = MutableLiveData<Long>()
        result.value = dataSource.getOnlineTime(userId)
        return result
    }
}
```



> 在跨使用者共享一个LiveData实例之前，请仔细考虑。

# MediatorLiveData：在初始化之外添加源

使用观察者模式比持有对视图的引用（在MVP体系结构中通常会做的）更安全。但是，这并不意味着您可以忘记泄漏！

考虑以下数据源：

```kotlin
class SlowRandomNumberGenerator {
    private val rnd = Random()

    fun getNumber(): LiveData<Int> {
        val result = MutableLiveData<Int>()

        // Send a random number after a while
        Executors.newSingleThreadExecutor().execute {
            Thread.sleep(500)
            result.postValue(rnd.nextInt(1000))
        }

        return result
    }
}
```

它只是在500毫秒后返回一个带有随机值的新LiveData。没有错。

在ViewModel中，我们需要公开一个`randomNumber`属性，该属性使用生成器中的数字。为此，使用MediatorLiveData是不理想的，因为它要求您每次需要新的数字时都添加源：

```kotlin
val randomNumber = MediatorLiveData<Int>()

/**
* *Don't do this.*
*
* Called when the user clicks on a button
*
* This function adds a new source to the result but it doesn't remove the previous ones.
*/
fun onGetNumber() {
   randomNumber.addSource(numberGenerator.getNumber()) {
       randomNumber.value = it
   }
}
```

如果用户每次单击按钮，我们都会向MediatorLiveData添加源，则该应用程序将按预期运行。但是，我们泄漏了所有以前的LiveData，这些LiveData将不再发送更新，因此很浪费。

您可以存储对源的引用，然后在添加新引用之前将其删除。（扰流器：这是做什么的`Transformations.switchMap`！请参阅下面的解决方案。）

而不是使用MediatorLiveData，让我们尝试（失败）通过以下方法解决此问题`Transformation.map`：

# Transformation：Transformation外部初始化

​	

```kotlin
var lateinit randomNumber: LiveData<Int>

/**
 * Called on button click.
 */
fun onGetNumber() {
   randomNumber = Transformations.map(numberGenerator.getNumber()) {
       it
   }
}
```

这里有一个重要的问题需要理解：转换在被调用时（`map`和`switchMap`）都会创建一个新的LiveData 。在此示例`randomNumber`中，视图是公开的，但每次用户单击按钮时都会重新分配。很容易错过**观察者只会在订阅时收到分配给var的LiveData的更新的情况**。

```kotlin
viewmodel.randomNumber.observe(this, Observer { number ->
    numberTv.text = resources.getString(R.string.random_text, number)
})
```

发生这种订阅的原因是`onCreate()`，如果`viewmodel.randomNumber`LiveData实例此后发生更改，则永远不会再次调用该观察器。

换一种说法：

> 不要在var中使用Livedata。初始化时进行接线转换。

# 解决方案：初始化期间进行接线转换

初始化LiveData时 transformation：

```kotlin
private val newNumberEvent = MutableLiveData<Event<Any>>()

val randomNumber: LiveData<Int> = Transformations.switchMap(newNumberEvent) {
   numberGenerator.getNumber()
}
```

使用LiveData中的[Event](https://medium.com/google-developers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150)来指示何时请求新号码：

```kotlin
/**
* Notifies the event LiveData of a new request for a random number.
*/
fun onGetNumber() {
   newNumberEvent.value = Event(Unit)
}
```

如果您不熟悉此模式，请参阅[有关事件的文章](https://medium.com/google-developers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150)。

------

# 附加部分

## 整理Kotlin

上面的MediatorLiveData示例显示了一些代码重复，因此我们可以利用Kotlin的扩展功能：

```kotlin
/**
* Sets the value to the result of a function that is called when both `LiveData`s have data
* or when they receive updates after that.
*/
fun <T, A, B> LiveData<A>.combineAndCompute(other: LiveData<B>, onChange: (A, B) -> T): MediatorLiveData<T> {

   var source1emitted = false
   var source2emitted = false

   val result = MediatorLiveData<T>()

   val mergeF = {
       val source1Value = this.value
       val source2Value = other.value

       if (source1emitted && source2emitted) {
           result.value = onChange.invoke(source1Value!!, source2Value!! )
       }
   }

   result.addSource(this) { source1emitted = true; mergeF.invoke() }
   result.addSource(other) { source2emitted = true; mergeF.invoke() }

   return result
}
```

现在，该代码看起来更加干净：

```kotlin
fun getDataForUser(newUser: String?): LiveData<UserDataResult> {
   if (newUser == null) {
       return MutableLiveData<UserDataResult>().apply { value = null }
   }

   return userOnlineDataSource.getOnlineTime(newUser)
           .combineAndCompute(userCheckinsDataSource.getCheckins(newUser)) { a, b ->
       UserDataSuccess(a, b)
   }
}
```



## LiveData和RxJava

最后，让我们来解决这个棘手的问题。LiveData旨在允许View观察ViewModel。一定要用它！即使您已经使用Rx，也可以与[LiveDataReactiveStreams](https://developer.android.com/reference/android/arch/lifecycle/LiveDataReactiveStreams) * 进行通信。

如果要在表示层之外使用LiveData，则可能会发现MediatorLiveData没有像RxJava提供的那样可以组合和操作数据流的工具包。但是，Rx具有陡峭的学习曲线。LiveData转换（和Kotlin magic）的组合可能足以满足您的情况，但是如果您（和您的团队）已经投资学习RxJava，则可能不需要LiveData。

*如果使用[auto-dispose](https://github.com/uber/AutoDispose)，那么使用LiveData将是多余的。