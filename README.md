# GestureRefreshLayout
[![Release](https://jitpack.io/v/Bvin/gesture-refresh-layout.svg)]
(https://jitpack.io/#Bvin/gesture-refresh-layout)

Android gesture refresh layout.

## Swipe Gesture
1. Both Translate, content and refresh view both translate, refresh view is next to content view transition.
2. Content Translate, just translate content view.
3. Refresher Translate, just translate refresh view.
4. Both Fixed, content and refresh view both are fixed.

## Usage
```xml
<GestureRefreshLayout>
    <ContentView />
    <RefreshView />
</GestureRefreshLayout>
```
java example code:
```java
mGestureRefreshLayout = (GestureRefreshLayout) findViewById(R.id.gesture_refresh_layout);
mGestureRefreshLayout.setEnabled(false);//false disable gesture refrehsh,else enable
mGestureRefreshLayout.setTranslateContent(false);//false to pin refreshview,other side move contentview
mGestureRefreshLayout.setOnRefreshListener(new GestureRefreshLayout.OnRefreshListener() {
    @Override
    public void onRefresh() {
        // do something.
        mGestureRefreshLayout.setRefreshing(false);
        // after close refresh.
    }
});
mGestureRefreshLayout.setOnGestureChangeListener(new GestureRefreshLayout.OnGestureStateChangeListener() {
    @Override
    public void onStartDrag(float startY) {
        mRefreshText.setText("pull to refresh");
    }

    @Override
    public void onDragging(float draggedDistance, float releaseDistance) {
         //rotate angle: 360*draggedDistance/releaseDistance
         mProgressBar.setProgress((int) (draggedDistance/releaseDistance*100));
         Log.d(TAG, "onDragging: "+draggedDistance+","+releaseDistance);
         if (draggedDistance>releaseDistance){
             mRefreshText.setText("release to update");
         }else {
             mRefreshText.setText("refreshing...");
         }

    }

    @Override
    public void onFinishDrag(float endY) {
        mRefreshText.setText("update...");
    }
});   
                         
```
trigger refresh any where:
```java
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.refresh) {
        mGestureRefreshLayout.setRefreshing(true);
    }
    return super.onOptionsItemSelected(item);
}
```

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
    compile 'com.github.bvin:gesture-refresh-layout:0.1.7'
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

---
中文版

Android手势刷新布局

## 滑动手势
1. 同步位移，即刷新视图跟随内容纵向位移，适合宽屏刷新视图。
2. 悬浮位移，即内容视图固定，刷新视图跟着手势纵向位移，适合沉浸式刷新。
3. 内容下潜位移，刷新视图固定，内容视图跟着手势纵向位移，这种情况一般是刷新视图会有吸引人的动画。
4. 不位移，内容和刷新视图都不随手势位移，虽然纵向固定，但是可以通过其他形式来表现刷新行为。

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
通常可以在GestureRefreshLayout布局里面添加子视图来实现刷新功能，第一个应为内容视图，第二个应为刷新视图。

原生SwipeRefreshLayout的ChildView的宽高会强制match_parent，而我们的GestureRefreshLayout可以支持Child
View为wrap_content。宽度不足match_parent的RefreshView将会处于水平居中位置，未来可提供gravity和margin
的支持。

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
可以设置GestureRefreshLayout.OnRefreshListener监听器来判断什么时候执行刷新操作（如异步请求），操作完
成后应通过mGestureRefreshLayout.setRefreshing(false)方法来通知操作完毕以让视图回到初始位置。注意，一
定要在UI线程调用mGestureRefreshLayout.setRefreshing(false)方法。
```java
mGestureRefreshLayout.setOnRefreshListener(new GestureRefreshLayout.OnRefreshListener() {
    @Override
    public void onRefresh() {
        // do something.
        mGestureRefreshLayout.setRefreshing(false);
        // after close refresh.
    }
});
```

GestureRefreshLayout支持手势刷新，也支持主动触发刷新。例如可以在选项菜单添加刷新按钮或者在一进入页面
自动执行刷新操作。这一特性很多情况是非常受用的，比如点击顶部刷新呢，或者用于电视，电视一般是没有触屏
的，这是主动刷新却是很实用的功能。
```java
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.refresh) {
        mGestureRefreshLayout.setRefreshing(true);
    }
    return super.onOptionsItemSelected(item);
}
```
同时如果你不想手势滑动触发刷新，只需要调用setEnable(false)方法就可以禁用。
```java
    mGestureRefreshLayout.setEnabled(false);
```

####高级用法
GestureRefreshLayout的强大之处是可以非常个性化得定制刷新效果。通过GestureChange手势状态改变监听器可以
定制任何你想要的效果，OnGestureStateChangeListener接口有下列3个状态回掉。
* onStartDrag(float startY) 开始滑动
* onDragging(float draggedDistance, float releaseDistance) 滑动中
* onFinishDrag(float endY) 滑动结束
可以根据不同状态下的不同参数值制定不同效果，如简单的刷新文字可以根据状态改变修改，其他动画可以根据
onDragging的两个参数比例值来完成旋转或者进度动画。
```java
mGestureRefreshLayout.setOnGestureChangeListener(new GestureRefreshLayout.OnGestureStateChangeListener() {
    @Override
    public void onStartDrag(float startY) {
        mRefreshText.setText("下拉刷新");
    }

    @Override
    public void onDragging(float draggedDistance, float releaseDistance) {
        //rotate angle: 360*draggedDistance/releaseDistance
        mProgressBar.setProgress((int) (draggedDistance/releaseDistance*100));
        Log.d(TAG, "onDragging: "+draggedDistance+","+releaseDistance);
        if (draggedDistance>releaseDistance){
            mRefreshText.setText("释放更新");
        }else {
            mRefreshText.setText("下拉刷新...");
        }
        // 超过定义的同步距离就意味着可以释放刷新了

    }

    @Override
    public void onFinishDrag(float endY) {
        mRefreshText.setText("正在更新...");
    }
});
```
![自定义动画效果演示](https://raw.githubusercontent.com/bvin/gesture-refresh-layout/dev/screen/gesture-refresh-advance.gif)

_为了保持结构简洁、用法简单，GestureRefreshLayout只提供基础手势滑动动画，其他任何表现刷新的动画和提示
都需自己实现。_


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

