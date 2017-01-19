package cn.bvin.android.apps.gesturerefresh;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import cn.bvin.android.lib.widget.refresh.GestureRefreshLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final GestureRefreshLayout gestureRefreshLayout = (GestureRefreshLayout) findViewById(R.id.gesture_refresh_layout);
        final TextView refreshText = (TextView) findViewById(R.id.refresh_text);
        gestureRefreshLayout.setTranslateContent(true);//是否移动ContentView
        gestureRefreshLayout.setOnGestureChangeListener(new GestureRefreshLayout.OnGestureStateChangeListener() {
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
        gestureRefreshLayout.setOnRefreshListener(new GestureRefreshLayout.OnRefreshListener() {
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
                                gestureRefreshLayout.setRefreshing(false);
                            }
                        });
                    }
                }).start();

            }
        });
    }
}
