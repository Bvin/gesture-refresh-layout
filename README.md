# GestureRefreshLayout
[![Release](https://jitpack.io/v/Bvin/gesture-refresh-layout.svg)]
(https://jitpack.io/#Bvin/gesture-refresh-layout)

Android gesture refresh layout.

## Installation
First, in your project's build.gradle add the jitpack.io repository like this:
```
allprojects {
 repositories {
    ...
    jcenter()
    maven { url "https://jitpack.io" }
 }
}
```
*Note: do not add the jitpack.io repository under buildscript.*


Then, add the following dependency to your module's build.gradle file:
```
dependencies {
    ...
    compile 'com.github.bvin:gesture-refresh-layout:0.1.1'
}
```
If you want to get the latest feature, you can find that release the end with letter "d"(the code on
 dev branch), example "com.github.bvin:gesture-refresh-layout:0.1.4d".The stable version is no end w
 ith letter "d".
 
If in your module already has support-v4 dependency, you should exclude the inner support module, li
ke this:
```
compile ('com.github.bvin:gesture-refresh-layout:0.1.4d') {
        exclude group: 'com.android.support', module: 'support-compat'
    }
```

## Usage
```xml
<GestureRefreshLayout>
    <ContentView />
    <RefreshView />
</GestureRefreshLayout>
```

## Swipe Gesture
1. Both Translate, content and refresh view both translate, refresh view is next to content view transition.
2. Content Translate, just translate content view.
3. Refresher Translate, just translate refresh view.
4. Both Fixed, content and refresh view both are fixed.

---
中文版

Android手势刷新布局

## 安装
首先，在项目根目录的build.gradle中添加JitPack远程仓库如下：
```
allprojects {
 repositories {
    ...
    jcenter()
    maven { url "https://jitpack.io" }
 }
}
```
注意：不要把JitPack的仓库地址放在buildscript里面，否则会出现无法解析远程依赖的错误。

然后，再你需要使用的Module中的build.gradle添加以下依赖：
```
dependencies {
    ...
    compile 'com.github.bvin:gesture-refresh-layout:0.1.1'
}
```
如果需要最新的功能，可以依赖以d结尾的在dev分支上发布的Tag，如 "com.github.bvin:gesture-refresh-layout:0.
1.4d"，稳定版是不以字母d结尾的。

如果你的module中已经有support-v4依赖，应该去除内部的module，如下：
```
compile ('com.github.bvin:gesture-refresh-layout:0.1.4d') {
        exclude group: 'com.android.support', module: 'support-compat'
    }
```

## 用法
通常可以在GestureRefreshLayout布局里面添加子视图来实现刷新功能，第一个应为内容视图，第二个应为刷新视图，
不接受大于2个的子视图。

原生SwipeRefreshLayout的ChildView的宽高会强制match_parent，而我们的GestureRefreshLayout可以支持Child
View为wrap_content。

至于为何SRL（即SwipeRefreshLayout，以下通称SRL）会这样做，我猜测是因为SRL把触摸事件
从ChildView拦截到SRL自身去做事件处理，它原生是可以从ChildView的区域滑出到SRL自身的区域，Touche事件可
以无缝衔接，虽然...但是SRL和ChildView是没有间隙的，是严丝合缝的。而GRL（即GestureRefreshLayout，以下通
称GRL）的ContentView是可以支持wrap_content的，就算你的ChildView小到比TouchSlop还小，依然可以在
ChildView外的GRL区域起作用。


```xml
<GestureRefreshLayout>
    <ContentView />
    <RefreshView />
</GestureRefreshLayout>
```

## 滑动手势
1. 同步位移，即刷新视图跟随内容纵向位移，适合宽屏刷新视图。
2. 悬浮位移，即内容视图固定，刷新视图跟着手势纵向位移，适合沉浸式刷新。
3. 内容下潜位移，刷新视图固定，内容视图跟着手势纵向位移，这种情况一般是刷新视图会有吸引人的动画。
4. 不位移，内容和刷新视图都不随手势位移，虽然纵向固定，但是可以通过其他形式来表现刷新行为。

## 实现原理
               
               +--------------------+   ------> OriginOffsetTop
               |   [Refresh View]   |
               |____________________|
               |                    |  
               |                    |
               | <----------------> |   ------> TotalDragDistance/SpinnerOffsetEnd
               |   [Content View]   |
               | ------- == ------- |   ------> CurrentOffsetTop
               |                    |
               |                    |
               |                    |
               |                    |
               |                    |
               |                    |
               |                    |
               |                    |
               +--------------------+
               
 1. 用户可以定义下拉的OffsetStart,OffsetEnd，其中OffsetStart是RefreshView初始位置，刷新完成或者取消下
 拉刷新都会回到这个位置，便于隐藏；其中OffsetEnd是下拉的相对距离(CurrentOffsetTop)超过了释放刷新的距
 离，需要回到一个位置等待执行事务，通常事务完成后需要回到初始位置。
 2. TotalDragDistance是触发刷新的滑动距离边界值，超过这个值就开始刷新，否则当作取消。可以通过当前滑
 动的距离与这个值的比例设定一个progress，这个是非常有用的，比如说你要旋转角度、改变透明度或者给定一个
 ProgressBar的进度值。

