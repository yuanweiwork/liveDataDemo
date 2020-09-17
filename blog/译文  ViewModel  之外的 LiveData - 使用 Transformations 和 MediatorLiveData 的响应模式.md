作者 ：[Jose Alcérreca](https://medium.com/@JoseAlcerreca?source=post_page-----fda520ba00b7--------------------------------)

[原文链接](https://medium.com/androiddevelopers/livedata-beyond-the-viewmodel-reactive-patterns-using-transformations-and-mediatorlivedata-fda520ba00b7)

多年来，反应式架构一直是Android中的热门话题。在Android会议上，它一直是一个不变的主题，通常使用RxJava示例进行说明（请参阅底部的Rx部分）。响应式编程是一种与数据流和更改传播有关的范例，它可以简化构建应用程序和显示来自异步操作的数据。

[LiveData](https://developer.android.com/topic/libraries/architecture/livedata)是实现某些反应性概念的一种工具。这是一个简单的可观察者，它了解观察者的生命周期。从数据源或存储库中公开LiveData是使您的体系结构更具响应性的简单方法，但存在一些潜在的陷阱。

这篇博客文章将帮助您避免陷阱，并使用一些模式来帮助您使用LiveData构建更具反应性的体系结构。

# LiveData的目的

在Android中，活动，片段和视图几乎可以随时销毁，因此任何对这些组件之一的引用都可能导致泄漏或`NullPointerException`。

LiveData旨在实现观察者模式，从而允许View控制器（活动，片段等）与UI数据源（通常为ViewModel）之间进行通信。使用LiveData，这种通信更加安全：由于具有生命周期意识，因此只有处于活动状态的View才能接收数据。

简而言之，优点是您不需要手动取消View和ViewModel之间的订阅。

![图片发布](https://miro.medium.com/max/60/1*aMu72YHtOdLMG3jc4qSoCQ.png?q=20)

![图片发布](https://miro.medium.com/max/952/1*aMu72YHtOdLMG3jc4qSoCQ.png)

*View-ViewModel交互*

# 超越ViewModel的LiveData

可观察的范例在View控制器和ViewModel之间确实很好地工作，因此您可以使用它来观察应用程序的其他组件并利用生命周期意识。例如：

- 观察[SharedPreferences中的](https://developer.android.com/reference/android/content/SharedPreferences.OnSharedPreferenceChangeListener)更改
- 在[Firestore中](https://firebase.google.com/docs/firestore/)观察文档或集合
- 使用[FirebaseAuth](https://firebase.google.com/docs/auth/)等身份验证SDK观察当前用户
- 观察[Room中](https://developer.android.com/topic/libraries/architecture/room)的查询（开箱即用地支持LiveData）

这种范例的优势在于，因为所有内容都连接在一起，所以当数据更改时，UI会自动更新。

缺点是LiveData不像Rx那样带有用于组合数据流或管理线程的工具包。

在典型应用的每一层中使用LiveData看起来像这样：

![图片发布](https://miro.medium.com/max/60/1*QXXiuXmzRTTqdaEEojyIcQ.png?q=20)

![图片发布](https://miro.medium.com/max/2110/1*QXXiuXmzRTTqdaEEojyIcQ.png)

*使用LiveData的典型应用程序架构*

为了在组件之间传递数据，我们需要一种映射和组合的方法。为此，将MediatorLiveData与Transformations类中的帮助器结合使用：

- [Transformations.map](https://developer.android.com/reference/android/arch/lifecycle/Transformations.html#map(android.arch.lifecycle.LiveData, android.arch.core.util.Function))
- [Transformations.switchMap](https://developer.android.com/reference/android/arch/lifecycle/Transformations.html#switchMap(android.arch.lifecycle.LiveData, android.arch.core.util.Function>))

> 请注意，在销毁View后，您无需拆除这些订阅，因为View的生命周期会向下游传播到后续订阅。

# **模式**

## **一对一静态转换—地图**

![图片发布](https://miro.medium.com/max/60/1*3FkrCCJEhV5dW6kJU9AUog.png?q=20)

![图片发布](https://miro.medium.com/max/1852/1*3FkrCCJEhV5dW6kJU9AUog.png)

*ViewModel观察一种数据类型并公开另一种数据类型*

在上面的示例中，ViewModel仅将数据从存储库转发到视图，然后将其转换为UI模型。每当存储库中有新数据时，ViewModel都必须拥有`map`它：

<iframe src="https://medium.com/media/06f6fe684ab4ddb408a3f1350a95c705" allowfullscreen="" frameborder="0" height="153" width="680" title="MainViewModel.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 153px;"></iframe>

这种转换非常简单。但是，如果用户可能会更改，则需要switchMap：

## 一对一动态转换— switchMap

考虑以下示例：您正在观察一个暴露用户的用户管理器，并且需要等待他们的ID后才能开始观察存储库。

![图片发布](https://miro.medium.com/max/60/1*nMHVwTGSFSSR9ooHL8TxFg.png?q=20)

![图片发布](https://miro.medium.com/max/2088/1*nMHVwTGSFSSR9ooHL8TxFg.png)

*用户管理器提供暴露结果之前存储库所需的用户ID*

您无法在ViewModel初始化时进行连接，因为用户ID不会立即可用。

您可以使用来实现`switchMap`。

<iframe src="https://medium.com/media/d7bdd631d37720e718ad621dcea59577" allowfullscreen="" frameborder="0" height="153" width="680" title="MainViewModel.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 153px;"></iframe>

A `switchMap`在内部使用MediatorLiveData，因此熟悉它很重要，因为当您要组合多个LiveData来源时需要使用它：

## 一对多依赖性-MediatorLiveData

MediatorLiveData使您可以将一个或多个数据源添加到单个可观察的LiveData中。

<iframe src="https://medium.com/media/0321806e465aac3256282c8c61685d98" allowfullscreen="" frameborder="0" height="285" width="680" title="要点：fed0218a0d5818db825132dac247c10d" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 285px;"></iframe>

当任何源更改时，此示例从[docs](https://developer.android.com/reference/android/arch/lifecycle/MediatorLiveData)更新结果。**请注意，数据不是为您合并的**。MediatorLiveData只是处理通知。

为了在示例应用程序中实现转换，我们需要将两个不同的LiveData组合为一个：

![图片发布](https://miro.medium.com/max/60/1*-Ymwmo3w4gLoc__pfAGc2g.png?q=20)

![图片发布](https://miro.medium.com/max/1466/1*-Ymwmo3w4gLoc__pfAGc2g.png)

*MediatorLiveData用于合并两个数据源*

使用MediatorLiveData合并数据的一种方法是添加源并以其他方法设置值：

<iframe src="https://medium.com/media/26f689e7abeb2f23e2d496e070df1c68" allowfullscreen="" frameborder="0" height="373" width="680" title="仓库.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 373px;"></iframe>

数据的实际组合是通过该`combineLatestData`方法完成的。

<iframe src="https://medium.com/media/6927d24dddb01f3303e0f512ad994968" allowfullscreen="" frameborder="0" height="417" width="680" title="仓库.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 417px;"></iframe>

它检查值是否已准备好或正确，并发出结果（*加载*，*错误*或*成功*）

请参阅下面的“奖励”部分，以了解如何使用Kotlin的扩展功能进行清理。

# 何时不使用LiveData

即使您想“变得被动”，也需要在将LiveData添加到各处之前了解其优势。如果您的应用程序组件没有与UI的连接，则可能不需要LiveData。

例如，您应用中的用户管理器会监听您的身份验证提供程序中的更改（例如Firebase Auth），并将唯一令牌上载到您的服务器。

![图片发布](https://miro.medium.com/max/60/1*w-XMAsDiCgpjUBEFi8SHvA.png?q=20)

![图片发布](https://miro.medium.com/max/1100/1*w-XMAsDiCgpjUBEFi8SHvA.png)

*令牌上传者和用户管理者之间的互动应该是被动的吗？*

令牌上载者可以观察用户管理器，但具有谁的生命周期？此操作与视图完全无关。此外，如果视图被破坏，则用户令牌可能永远不会被上传。

另一种选择是使用令牌上传器中的[observeForever](https://developer.android.com/reference/android/arch/lifecycle/LiveData#observeforever)（），并以某种方式挂接到用户管理器的生命周期中，以在完成后删除订阅。

但是，您无需使所有内容均可观察。让用户经理直接调用令牌上传器（或在您的体系结构中有意义的任何调用）。

![图片发布](https://miro.medium.com/max/60/1*u2dKCA0uWtS2k7zNGgBumw.png?q=20)

![图片发布](https://miro.medium.com/max/1100/1*u2dKCA0uWtS2k7zNGgBumw.png)

*与UI不相关的操作不需要使用LiveData*

> 如果您的应用程序的一部分不影响UI，则您可能不需要LiveData。

# 反模式：共享LiveData实例

当一个类将LiveData公开给其他类时，请仔细考虑是否要公开相同或不同的LiveData实例。

<iframe src="https://medium.com/media/f1a623a675fae26f3408cc1b27c956db" allowfullscreen="" frameborder="0" height="263" width="680" title="共享LiveDataSource.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 263px;"></iframe>

如果此类在您的应用中是单例（只有一个实例），则您始终可以返回相同的LiveData，对吗？不一定：此类可能有多个使用者。

例如，考虑以下一个：

<iframe src="https://medium.com/media/2101e961637a6aa6377e80c86c88ffe4" allowfullscreen="" frameborder="0" height="109" width="680" title="消费者1.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 109px;"></iframe>

第二个消费者也使用它：

<iframe src="https://medium.com/media/267bd0bfa347e383b0109a2d79faa442" allowfullscreen="" frameborder="0" height="109" width="680" title="消费者2.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 109px;"></iframe>

第一消费者将接收具有属于用户“ 2”的数据的更新。

即使您认为仅从一个使用者使用此类，您也可能最终会因使用此模式而产生错误。例如，当从一个活动的一个实例导航到另一个实例时，**新实例可能会暂时从上一个实例接收数据**。请记住，LiveData将最新值分配给新的观察者。此外，在Lollipop中引入了活动转换，它们带来了一个有趣的优势案例：**两个活动处于活动状态**。这意味着可能有两个LiveData唯一使用者的实例，其中一个实例可能会显示错误的数据。

解决此问题的方法只是为每个使用者返回一个新的LiveData。

<iframe src="https://medium.com/media/382a3960f55184cf4b29ed401cd738db" allowfullscreen="" frameborder="0" height="197" width="680" title="共享LiveDataSource.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 197px;"></iframe>

> Think carefully before sharing a LiveData instance across consumers.

# MediatorLiveData的味道：在初始化之外添加源

使用观察者模式比持有对视图的引用（在MVP体系结构中通常会做的）更安全。但是，这并不意味着您可以忘记泄漏！

考虑以下数据源：

<iframe src="https://medium.com/media/b4fa2a3913b6a3b80ebc784365317768" allowfullscreen="" frameborder="0" height="373" width="680" title="SlowRandomNumberGenerator.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 373px;"></iframe>

它只是在500毫秒后返回一个带有随机值的新LiveData。没有错。

在ViewModel中，我们需要公开一个`randomNumber`属性，该属性使用生成器中的数字。为此，使用MediatorLiveData是不理想的，因为它要求您每次需要新的数字时都添加源：

<iframe src="https://medium.com/media/a1d9527cd1787077fd31471258e73bfa" allowfullscreen="" frameborder="0" height="351" width="680" title="MainViewModel.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 351px;"></iframe>

如果用户每次单击按钮，我们都会向MediatorLiveData添加源，则该应用程序将按预期运行。但是，我们泄漏了所有以前的LiveData，这些LiveData将不再发送更新，因此很浪费。

您可以存储对源的引用，然后在添加新引用之前将其删除。（扰流器：这是做什么的`Transformations.switchMap`！请参阅下面的解决方案。）

而不是使用MediatorLiveData，让我们尝试（失败）通过以下方法解决此问题`Transformation.map`：

# 转换气味：初始化之外的转换

使用前面的示例，这将不起作用：

<iframe src="https://medium.com/media/7c3d330360f75c4f1e59a41f4f06e95d" allowfullscreen="" frameborder="0" height="263" width="680" title="MainViewModel.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 263px;"></iframe>

这里有一个重要的问题需要理解：转换在被调用时（`map`和`switchMap`）都会创建一个新的LiveData 。在此示例`randomNumber`中，视图是公开的，但每次用户单击按钮时都会重新分配。很容易错过**观察者只会在订阅时收到分配给var的LiveData的更新的情况**。

<iframe src="https://medium.com/media/6aefce903175b2f61e48bbee5337e1fa" allowfullscreen="" frameborder="0" height="109" width="680" title="MainActivity.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 109px;"></iframe>

发生这种订阅的原因是`onCreate()`，如果`viewmodel.randomNumber`LiveData实例此后发生更改，则永远不会再次调用该观察器。

换一种说法：

> 不要在var中使用Livedata。初始化时进行接线转换。

# 解决方案：初始化期间进行接线转换

初始化公开的LiveData作为转换：

<iframe src="https://medium.com/media/6e6e0159d0140035f401fbd128edba16" allowfullscreen="" frameborder="0" height="153" width="680" title="MainViewModel.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 153px;"></iframe>

使用LiveData中的[事件](https://medium.com/google-developers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150)来指示何时请求新号码：

<iframe src="https://medium.com/media/86d3fb0f3b98d5c4a9ca142f23eed578" allowfullscreen="" frameborder="0" height="175" width="680" title="MainViewModel.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 175px;"></iframe>

如果您不熟悉此模式，请参阅[有关事件的文章](https://medium.com/google-developers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150)。

------

# 奖金部分

## 整理Kotlin

上面的MediatorLiveData示例显示了一些代码重复，因此我们可以利用Kotlin的扩展功能：

<iframe src="https://medium.com/media/753191abdfbe25a2a41be5ea93b9d34d" allowfullscreen="" frameborder="0" height="593" width="680" title="MediatorExtensions.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 592.984px;"></iframe>

现在，该存储库看起来更加干净：

<iframe src="https://medium.com/media/15e23e3a1c0cca9331d26eb948e0a664" allowfullscreen="" frameborder="0" height="263" width="680" title="仓库.kt" class="t u v cz aj" scrolling="auto" style="box-sizing: inherit; position: absolute; top: 0px; left: 0px; width: 680px; height: 263px;"></iframe>

## LiveData和RxJava

最后，让我们谈谈房间里的大象。LiveData旨在允许View观察ViewModel。一定要用它！即使您已经使用Rx，也可以与[LiveDataReactiveStreams](https://developer.android.com/reference/android/arch/lifecycle/LiveDataReactiveStreams) * 进行通信。

如果要在表示层之外使用LiveData，则可能会发现MediatorLiveData没有像RxJava提供的那样可以组合和操作数据流的工具包。但是，Rx具有陡峭的学习曲线。LiveData转换（和Kotlin魔术）的组合可能足以满足您的情况，但是如果您（和您的团队）已经投资学习RxJava，则可能不需要LiveData。

*如果使用[auto-dispose](https://github.com/uber/AutoDispose)，那么使用LiveData将是多余的。