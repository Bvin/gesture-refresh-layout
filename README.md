# GestureRefreshLayout
Android手势刷新布局

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

