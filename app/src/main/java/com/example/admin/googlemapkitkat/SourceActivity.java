package com.example.admin.googlemapkitkat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class SourceActivity extends Activity {

    @Override
    protected void onCreate (Bundle savedinstanceState) {
        super.onCreate(savedinstanceState);
        setContentView(R.layout.activity_source);

        // パラメータを取得
        Intent intent = getIntent();
        if(intent != null) {
            TextView textView = (TextView)findViewById(R.id.sTextView);
            textView.setText(intent.getStringExtra("text"));
        }
    }
}
