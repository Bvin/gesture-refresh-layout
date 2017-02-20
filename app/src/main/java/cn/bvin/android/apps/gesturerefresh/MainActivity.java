package cn.bvin.android.apps.gesturerefresh;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import cn.bvin.android.lib.widget.refresh.GestureRefreshLayout;

public class MainActivity extends AppCompatActivity implements GestureRefreshLayout.OnRefreshListener {

    private static final String TAG = "MainActivity";

    private GestureRefreshLayout mGestureRefreshLayout;
    private ProgressBar mProgressBar;
    private TextView mRefreshText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGestureRefreshLayout = (GestureRefreshLayout) findViewById(R.id.gesture_refresh_layout);
        mRefreshText = (TextView) findViewById(R.id.refresh_text);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setMax(100);
        mGestureRefreshLayout.setTranslateContent(false);//是否移动ContentView
        //mGestureRefreshLayout.setDistanceToTriggerSync((int) (100*getResources().getDisplayMetrics().density));
        mGestureRefreshLayout.setOnLayoutTranslateCallback(new GestureRefreshLayout.OnLayoutTranslateCallback() {
            @Override
            public void onLayoutTranslate(int movementTop) {
                if (movementTop >= mProgressBar.getTop() && movementTop <= mProgressBar.getHeight()) {
                    //ViewCompat.setTranslationX(mProgressBar, t);
                    mProgressBar.layout(mProgressBar.getLeft(), movementTop, mProgressBar.getWidth(), movementTop + mProgressBar.getHeight());
                }
            }
        });
        mGestureRefreshLayout.setOnGestureChangeListener(new GestureRefreshLayout.OnGestureStateChangeListener() {
            @Override
            public void onStartDrag(float startY) {
                mRefreshText.setText("下拉刷新");
            }

            @Override
            public void onDragging(float draggedDistance, float releaseDistance) {
                //releaseDistance=RefreshView.h+64
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
        mGestureRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mGestureRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }).start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            mGestureRefreshLayout.setRefreshing(true);
            onRefresh();
        }
        return super.onOptionsItemSelected(item);
    }
}
