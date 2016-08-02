package com.example.admin.googlemapkitkat;

// 2016/8/2/11:02

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

// FragmentActivity is included in ActionBarActivity
// FragmentActivity, AppCompatActivity

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        LocationListener, ConnectionCallbacks, OnConnectionFailedListener {

    /** 緯度の差 */
    private static final double latDiff = 0.002861023f;
    /** 経度の差 */
    private static final double lonDiff = -3.0517578E-4;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int INTERVAL = 500;
    private static final int FASTESTINTERVAL = 16;
    private static final LocationRequest REQUEST = LocationRequest.create()
            // ミリ秒単位で位置情報の更新間隔を設定
            .setInterval(INTERVAL)
            // setintervalはあくまで目安であり、状況によって間隔が変わる
            .setFastestInterval(FASTESTINTERVAL)
            // 高精度だがバッテリーを食う
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    private FusedLocationProviderApi mFusedLocationProviderApi = LocationServices.FusedLocationApi;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private boolean mStart = false;
    private boolean mStop = false;

    /** ソース&命令表示用テキストフィールド */
    private TextView textView1, textView2;
    /** 現在地の緯度経度 */
    private LatLng nowLatLng;
    /** プログレスバー */
    private ProgressBar progressBar;
    /** 効果音*/
    private SoundPool soundPool;
    private int sound;

    dataUtil d = new dataUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("debug", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // 自動スリープをしない
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        // 現在地を初期化
        nowLatLng = new LatLng(0, 0);

        // テキストビューを用意
        textView1 = (TextView)findViewById(R.id.textView1);
        textView2 = (TextView)findViewById(R.id.textView2);

        // プログレスバーを用意
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        // 最大値はtaskListの長さ
        progressBar.setMax(d.taskList.length);

        // SoundPoolのインスタンス作成
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        // 効果音をロードしておく
        // 引数はContext、リソースID、優先度
        sound = soundPool.load(this,R.raw.mdecision, 1);

        // START/STOPボタンを用意
        ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButton);
        // 起動時はボタンがオフ(START)の状態
        tb.setChecked(false);

        // ToggleButtonのCheckが変更したタイミングで呼び出されるリスナー
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // startボタンが押されたとき
                if (isChecked) {
                    LatLng goalLocation = new LatLng(nowLatLng.latitude + latDiff,
                            nowLatLng.longitude + lonDiff);
                    d.setParamList(nowLatLng,goalLocation);
                    drawMemory();
                    mStart = true;
                    mStop = false;
                    soundPool.play(sound, 0.5f, 0.5f, 0, 0, 1);
                } else {
                    mStop = true;
                    mStart = false;
                }
            }
        });

        // NEXtボタンを用意
        Button button = (Button) findViewById(R.id.nextButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mStart) action();
            }
        });
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

    // 一定の間隔で呼ばれる
    @Override
    public void onLocationChanged(final Location location) {
        Log.d("debug", "onLocationChanged");
        // 現在地の緯度経度を更新
        nowLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        // アプリを起動すると現在地に地図の中心を移動する
        if(mSetUp) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(nowLatLng).zoom(18f)
                    .bearing(0).build();
            // 地図の中心を取得した緯度、経度に動かす
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mSetUp = false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

//        if(mStart) {
//            // onLocationChanged()内で呼んでいるため、レスポンスが遅れる
//            action();
//        }

        // 現在地をマップの中心にさせるボタンを追加
        mMap.setMyLocationEnabled(true);
    }


    /**
     * 行動メソッド
     * NEXtボタンを押すと呼ばれる
     */
    private void action() {
        Log.d("debug", "action");
        // TODO: commandごとにダイアログを表示させる条件が異なる、調整中。


        function(d);

        // タスクがinputであれば表示しない
        if(d.getTask().getCommand() != command.input) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

            // ダイアログの設定
            alertDialog.setTitle("暫定的なダイアログ");
            alertDialog.setMessage("ここに説明を追加");

            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // "OK"ボタンが押されたときの処理
                    // ダイアログを破棄
                    dialogInterface.dismiss();
                }
            });

            // "OK"以外でダイアログが閉じられないようにする
            alertDialog.setCancelable(false);

//            // ダイアログの表示
            alertDialog.show();

        }
        // TODO: ダイアログの表示の際にTextViewをハイライトできないか検討中
        setSource();
        setTask();

        progressBar.setProgress(d.taskCursor);

        // 次のタスクへ
        d.next();
    }

    public void drawMemory(){
        LatLng sLL0,sLL1,gLL0,gLL1,center;

        for(String key:d.paramMap.keySet()){
            param param = d.paramMap.get(key);
            center = param.getLocate();
            sLL0 = new LatLng(center.latitude-d.r,center.longitude-d.r);
            sLL1 = new LatLng(center.latitude+d.r,center.longitude-d.r);
            gLL0 = new LatLng(center.latitude+d.r,center.longitude+d.r);
            gLL1 = new LatLng(center.latitude-d.r,center.longitude+d.r);

            PolygonOptions rect = new PolygonOptions()
                    .add(sLL0,sLL1,gLL0,gLL1)
                    .strokeColor(Color.BLACK)
                    .strokeWidth(4)
                    ;
            //出現している
            if(param.isAppeared == true){
                //次の目的地なら
                if(key.equals(d.getTask().getTarget()))
                    rect.fillColor(0x60ff0000);
                else rect.fillColor(0x600000ff);
                MarkerOptions marker = new MarkerOptions()
                        .position(param.getLocate());
                if(param.getNumber() == param.NONE)
                    marker.title(key);
                else if(param.getNumber() == param.NOPARAM)
                    marker.title(key + "(関数)");
                else marker.snippet(Integer.toString(param.getNumber()));
                mMap.addMarker(marker);
            }
            //出現していない
            else{
                rect .fillColor(0x60ffffff);
            }
            mMap.addPolygon(rect);
        }
    }


    //このメソッドをアプリのメインクラスに実装
    //各処理において、現在のタスクを出力
    public void function(final dataUtil d) {
        Log.d("debug", "function");
        command command = d.getTask().getCommand();
        switch (command) {
            case display:
                //TODO 出力処理
                break;
            case initialize:
                d.initialize_c();
                break;
            case substitude:
                d.substitude_c();
                break;
            case get:
                d.get_c();
                break;
            case input:
                inputNum();
                break;
            case move:
                //TODO ここに移動の処理
                break;
            case output:
                //TODO ここにコンソール出力処理
                break;
            case add:
                d.add_c();
                break;
            case exit:
                //TODO ここにコンソール出力処理
                System.exit(0);
                break;
            default:
                break;
        }
    }

    /**
     * 入力受け付けメソッド
     */
    private void inputNum() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.dialog, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("入力受け付けダイアログ");
        alertDialog.setMessage(d.getTask().getText());
        alertDialog.setView(view);
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText editText = (EditText)view.findViewById(R.id.editText);
                String strNum = editText.getText().toString();
                d.input_c(strNum);
                dialogInterface.dismiss();
            }
        });

//        myAlertDialog = alertDialog.create();
//        myAlertDialog.show();
        alertDialog.show();
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

    /**
     * メニューボタンを表示するメソッド
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("debug", "onCreateOptionsMenu");
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 右上のメニューボタンから何かしらを選択した際に呼ばれるメソッド
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("debug", "onOptionsItemSelected");
        // 連携処理を実施
        int itemId = item.getItemId();
        if (itemId == R.id.action_source) {
            // SourceActivityを呼び出すIntentを生成
            Intent intent = new Intent(this, SourceActivity.class);
            // textというパラメータを設定
            intent.putExtra("text", d.getCode());
            // startActivityでソースコードを呼び出す
            startActivity(intent);
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