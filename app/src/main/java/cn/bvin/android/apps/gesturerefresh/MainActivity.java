package cn.bvin.android.apps.gesturerefresh;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import cn.bvin.android.lib.widget.refresh.GestureRefreshLayout;

public class MainActivity extends AppCompatActivity implements GestureRefreshLayout.OnRefreshListener {

    private GestureRefreshLayout mGestureRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGestureRefreshLayout = (GestureRefreshLayout) findViewById(R.id.gesture_refresh_layout);
        final TextView refreshText = (TextView) findViewById(R.id.refresh_text);
        mGestureRefreshLayout.setTranslateContent(true);//是否移动ContentView
        mGestureRefreshLayout.setOnGestureChangeListener(new GestureRefreshLayout.OnGestureStateChangeListener() {
            @Override
            public void onStartDrag(float startY) {
                refreshText.setText("下拉刷新");
            }

            @Override
            public void onDragging(float draggedDistance, float releaseDistance) {
                refreshText.setText("下拉刷新...");
            }

            @Override
            public void onFinishDrag(float endY) {
                refreshText.setText("释放更新");
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
