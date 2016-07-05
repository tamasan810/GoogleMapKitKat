package com.example.admin.googlemapkitkat;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        LocationListener, ConnectionCallbacks, OnConnectionFailedListener {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int INTERVAL = 500;
    private static final int FASTESTINTERVAL = 16;
    private static final LocationRequest REQUEST = LocationRequest.create()
            // ミリ秒単位で位置情報の更新間隔を設定
            .setInterval(INTERVAL)
            // setintervalはあくまで目安であり、状況によって間隔が変わる
            // 正確な更新間隔も設定
            .setFastestInterval(FASTESTINTERVAL)
            // 高精度だがバッテリーを食う
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    private FusedLocationProviderApi mFusedLocationProviderApi = LocationServices.FusedLocationApi;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    // 位置情報を格納
    private ArrayList<LatLng> mRunList = new ArrayList<LatLng>();
    private long mStartTimeMillis;
    private double mMeter = 0.0; // メートル
    private double mElapsedTime = 0.0; // ミリ秒
    private boolean mStart = false;
    private boolean mFirst = false;
    private boolean mStop = false;
    private boolean mSetUp = true;
    private Chronometer mChronometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("debug", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // GoogleApiClientを生成
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // GoogleMapインスタンス生成完了時に呼ばれる処理を登録する。
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        // 非同期にマップを取得
        mapFragment.getMapAsync(this);

        ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButton);
        tb.setChecked(false); // 起動時はボタンがオフの状態

        // ToggleのCheckが変更したタイミングで呼び出されるリスナー
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // トグルキーが変更された際に呼び出される
                if (isChecked) {
                    startChronometer();
                    mStart = true;
                    mFirst = true;
                    mStop = false;
                    mMeter = 0.0; // 移動距離をリセット
                } else {
                    stopChronometer();
                    mStop = true;
                    mStart = false;
                }
            }
        });
    }

    private void startChronometer() {
        Log.d("debug", "startChronometer");
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        // 電源オン時からの経過時間の値をベースにする
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
        mStartTimeMillis = System.currentTimeMillis();
    }

    private void stopChronometer() {
        Log.d("debug", "stopChronometer");
        mChronometer.stop();
        // ミリ秒
        mElapsedTime = SystemClock.elapsedRealtime() - mChronometer.getBase();
    }

    @Override
    protected void onResume() {
        Log.d("debug", "onResume");
        super.onResume();

        // GooglePlayサービスに接続
        // 接続したらonConnectedを呼び出す
        mGoogleApiClient.connect();
    }

    // getMapAsync()メソッドでマップが準備できると、onMapReady()メソッドが呼び出される
    // SupportMapFragmentクラスからオーバーライド
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("debug", "onMapReady");
        mMap = googleMap;

        // DangerousなPermissionはリクエストして許可をもらわないと使えない
        // アプリケーションが指定したパーミッションを持っているかを判断
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // ユーザーがPermissionを明示的に拒否したかどうかを返す
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // 一度拒否されたとき、Rationale(理論的根拠)を説明して、再度許可ダイアログを出すようにする
                new AlertDialog.Builder(this)
                        .setTitle("許可が必要です")
                        .setMessage("移動に合わせて地図を動かすためには、ACCESS_FINE_LOCATIONを" +
                                "許可してください")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // OK button pressed
                                requestAccessFineLocation();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showToast("GPS機能が使えないので、地図は動きません");
                            }
                        })
                        .show();
            } else {
                // まだ許可を求める前のとき、許可を求めるダイアログを表示する
                requestAccessFineLocation();
            }
        }
    }

    private void requestAccessFineLocation() {
        Log.d("debug", "requestAccessFineLocation");
        // 許可ダイアログを表示して、ユーザーに許可してもらう
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }

    // 許可ダイアログに対してユーザーが選択した結果を受け取る
    // ActivityCompatクラスからオーバーライド
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        Log.d("debug", "onRequestPermissionsResult");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // ユーザーが許可したとき
                // 許可が必要な機能を改めて実行する
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO
                } else {
                    // ユーザーが許可しなかった時
                    // 許可されなかったため機能が実行できないことを表示する
                    showToast("GPS機能が使えないので、地図は動きません");
                    // 以下を実行すると、java.lang.RuntimeExceptionになる
                    // mMap.setMyLocationEnable(true);
                }
                return;
            }
        }
    }

    //
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("debug", "onConnected");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // REQUESTオブジェクトを渡して、位置の更新をリクエスト
        // これでonLocationChanged()メソッドが呼び出されるようになる
        mFusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, REQUEST, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("debug", "onLocationChanged");
        // mStopがtrueならば、何もしない
        if (mStop) {
            return;
        }

        if(mSetUp) {
            // 調整
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(18f)
                    .bearing(0).build();
            // 地図の中心を取得した緯度、経度に動かす
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mSetUp = !mSetUp;
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        double lat = location.getLatitude(); // 経度
        double lon = location.getLongitude(); // 緯度

        TextView latText = (TextView) findViewById(R.id.latText);
        TextView lonText = (TextView) findViewById(R.id.lonText);
        latText.setText(String.valueOf(lat));
        lonText.setText(String.valueOf(lon));

        if (mStart) {
            // 移動線を描画
            drawTrace(latLng);
            // 走行距離を計算
            sumDistance();
        }

    }

    // 移動線を描画するメソッド
    private void drawTrace(LatLng latLng) {
        Log.d("debug", "drawTrace");
        // 位置情報をリストに追加
        mRunList.add(latLng);
        if(mRunList.size() > 2) {
            PolylineOptions polylineOptions = new PolylineOptions();
            for(LatLng polyLatLng : mRunList) {
                polylineOptions.add(polyLatLng);
            }
            polylineOptions.color(Color.BLUE);
            polylineOptions.width(3);
            polylineOptions.geodesic(false);
            mMap.addPolyline(polylineOptions);
        }
    }

    // 移動距離を計算するメソッド
    private void sumDistance() {
        Log.d("debug", "sumDistance");
        if(mRunList.size() < 2) {
            return;
        }
        mMeter = 0;
        // 結果を格納するための配列を生成
        float[] results = new float[3];
        int i = 1;

        while(i < mRunList.size()) {
            results[0] = 0;
            // 引数に指定した配列に計算結果を格納する
            // results[0]: 距離(メートル)
            // results[1]: 始点から終点までの方位角
            // results[2]: 終点から始点までの方位角
            Location.distanceBetween(mRunList.get(i - 1).latitude, mRunList.get(i - 1).longitude,
                    mRunList.get(i).latitude, mRunList.get(i).longitude, results);
            mMeter += results[0];
            i++;
        }
        TextView disText = (TextView) findViewById(R.id.disText);
        disText.setText(String.format("%2f" + " ,", mMeter));
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Do nothing
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Do nothing
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }
}