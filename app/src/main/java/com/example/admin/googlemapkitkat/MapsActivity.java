package com.example.admin.googlemapkitkat;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

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
    private dataUtil d = new dataUtil();
    /** ソース&命令表示用テキストフィールド */
    private TextView textView1, textView2;
    // ここでxmlファイルを参照しようとするとエラー
//    private TextView textView1 = (TextView)findViewById(R.id.textView1);
//    private TextView textView2 = (TextView)findViewById(R.id.textView2);
    private boolean mStart = false;
    private boolean mStop = false;

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

        // START/STOPボタンを用意
        ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButton);
        // 起動時はボタンがオフ(START)の状態
        tb.setChecked(false);

        textView1 = (TextView)findViewById(R.id.textView1);
        textView2 = (TextView)findViewById(R.id.textView2);

        // 起動直後のソースと命令(Task)を表示
        // int main(void) {
        // main関数開始
        setSource();
        setTask();
        d.progCursor++;
        d.taskCursor++;

        // ToggleButtonのCheckが変更したタイミングで呼び出されるリスナー
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // startボタンが押されたとき
                if (isChecked) {
                    mStart = true;
                    mStop = false;
                } else {
                    mStop = true;
                    mStart = false;
                }
            }
        });
    }

    /**
     * 処理中のソースコードを表示する
     */
    private void setSource() {
        textView1.setText(d.getCode());
    }

    /**
     * 命令(初期化、代入など)を表示する
     */
    private void setTask() {
        textView2.setText(d.getTask().getText());
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

    /** カメラ調整用 */
    private boolean mSetUp = true;
    /** メモリの一辺の長さ */
    private final double side = 5.0 * Math.pow(10, -4);
    /** メモリの個数 */
    private final int numMemory = 6;

    @Override
    public void onLocationChanged(final Location location) {
        Log.d("debug", "onLocationChanged");

        // アプリを起動すると現在地に地図の中心を移動する
        if(mSetUp) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(18f)
                    .bearing(0).build();
            // 地図の中心を取得した緯度、経度に動かす
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mSetUp = false;
        }


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if(mStart) {
            // TODO: ソースと命令の表示
            // 移動や入力によって表示が変化する
            setSource();
            setTask();
        }

        // 現在地をマップの中心にさせるボタンを追加
        mMap.setMyLocationEnabled(true);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            // 引数はタップした個所の緯度経度
            public void onMapClick(LatLng goalLatLng){
                // マップをクリア
                mMap.clear();

                // タップした時の現在地の緯度経度
                final LatLng startLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                // TODO: 図形の描画をやってもらう
                LatLng sLL0, sLL1, gLL0, gLL1;
                LatLng[] latLngs = new LatLng[numMemory - 1];
                double x = Math.abs(goalLatLng.longitude - startLatLng.longitude);
                double y = Math.abs(goalLatLng.latitude - startLatLng.latitude);

                // 縦方向が長い場合
                if(y > x) {
                    sLL0 = new LatLng(startLatLng.latitude, startLatLng.longitude - side / 2.0);
                    sLL1 = new LatLng(startLatLng.latitude, startLatLng.longitude + side / 2.0);
                    gLL0 = new LatLng(goalLatLng.latitude, goalLatLng.longitude + side / 2.0);
                    gLL1 = new LatLng(goalLatLng.latitude, goalLatLng.longitude - side / 2.0);
                    for(int i = 0; i < latLngs.length; i++) {
                        latLngs[i] = new LatLng(startLatLng.latitude + side * (i + 1), startLatLng.longitude);
                    }

                }
                // 横方向に長い場合
                else {
                    sLL0 = new LatLng(startLatLng.latitude - side / 2.0, startLatLng.longitude);
                    sLL1 = new LatLng(startLatLng.latitude + side / 2.0, startLatLng.longitude);
                    gLL0 = new LatLng(goalLatLng.latitude + side / 2.0, goalLatLng.longitude);
                    gLL1 = new LatLng(goalLatLng.latitude - side / 2.0, goalLatLng.longitude);
                }

                // 長方形を描画
                PolygonOptions rectOptions = new PolygonOptions();
                rectOptions.add(sLL0, sLL1, gLL0, gLL1);
                rectOptions.strokeColor(Color.BLACK);
                rectOptions.strokeWidth(3);
                rectOptions.fillColor(0x700000ff);
                mMap.addPolygon(rectOptions);

                for(int i = 0; i < latLngs.length; i++) {
                    PolylineOptions lineOptions = new PolylineOptions();
                    lineOptions.add(new LatLng(latLngs[i].latitude, latLngs[i].longitude - side / 2.0));
                    lineOptions.add(new LatLng(latLngs[i].latitude, latLngs[i].longitude + side / 2.0));
                    lineOptions.width(3);
                    mMap.addPolyline(lineOptions);
                }


            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 連携処理を実施
        int itemId = item.getItemId();
        try {
            if (itemId == R.id.action_source) {
                // ソースコードを全て表示
//                Intent intent = new Intent(Intent.);
            }
        } catch (ActivityNotFoundException e) {
            // アプリがなかった時のエラー処理
            showToast(getString(R.string.lb_activity_not_found));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Do nothing
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Do nothing
    }

    /** エラーメッセージを表示 */
    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }
}