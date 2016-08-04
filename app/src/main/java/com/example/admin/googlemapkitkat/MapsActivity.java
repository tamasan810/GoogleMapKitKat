package com.example.admin.googlemapkitkat;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
    /** START, NEXTボタン */
    private Button startButton, nextButton;
    /** ソース&命令表示用テキストビュー */
    private TextView textView1, textView2;
    /** 進行度用テキストビュー */
    private TextView barTV;
    /** 現在地の緯度経度 */
    private LatLng nowLatLng;
    /** プログレスバー */
    private ProgressBar progressBar;
    /** 効果音*/
    private SoundPool soundPool;
    private int[] sounds;
    private String filepath;

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

        // 効果音の用意
        setSound();

        // STARTボタンを用意
        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setup();
                soundPool.play(sounds[0], 0.5f, 0.5f, 0, 0, 1);
                mStart = false;
                startButton.setEnabled(false);
                nextButton.setEnabled(true);
            }
        });

        // NEXTボタンを用意
        nextButton = (Button) findViewById(R.id.nextButton);
        nextButton.setEnabled(false);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                action();
            }
        });

        selectLevel();
    }

    LatLng startLatLng;
    void setup(){
        AlertDialog.Builder alertDialogS = new AlertDialog.Builder(this);
        alertDialogS.setTitle("スタート地点取得");
        alertDialogS.setMessage("スタート地点を取得します。スタート地点に立ったらOKボタンを押してください。");
        alertDialogS.setCancelable(false);
        alertDialogS.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startLatLng = nowLatLng;
                dialogInterface.dismiss();
                setGoal();
            }
        });
        alertDialogS.show();
    }

    public void setGoal(){
        final Activity activity = this;
        AlertDialog.Builder alertDialogG = new AlertDialog.Builder(this);
        alertDialogG.setTitle("ゴール地点取得");
        alertDialogG.setMessage("ゴール地点を取得します。ゴール地点に立ったらOKボタンを押してください。");
        alertDialogG.setCancelable(false);
        alertDialogG.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                d.readScript(activity, filepath,startLatLng,nowLatLng);
                // テキストビューを用意
                textView1 = (TextView) findViewById(R.id.textView1);
                textView2 = (TextView) findViewById(R.id.textView2);
                barTV = (TextView) findViewById(R.id.barTextView);
                barTV.setText("0 /" + d.taskList.length);

                // プログレスバーを用意
                progressBar = (ProgressBar)findViewById(R.id.progressBar);
                // 最大値はtaskListの長さ
                progressBar.setMax(d.taskList.length);
                dialogInterface.dismiss();
                upDate();
            }
        });
        alertDialogG.show();
    }

    /**
     * 効果音を用意する
     */
    public void setSound() {
        // SoundPoolのインスタンス作成
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        // 効果音をロードしておく
        // 引数はContext、リソースID、優先度
        sounds = new int[3];
        sounds[0] = soundPool.load(this,R.raw.start, 1);
        sounds[1] = soundPool.load(this, R.raw.dialog, 1);
        sounds[2] = soundPool.load(this, R.raw.finish, 1);
    }

    public void selectLevel() {
        final String[] items = {"初級", "中級"};
        new AlertDialog.Builder(this)
                .setTitle("選択ダイアログ")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0) {
                            filepath = "prog3.txt";

                        }else{
                            filepath = "prog1.txt";
                        }
                    }
                })
                .show();
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
        if(mStart) return;

        // 現在地の緯度経度を更新
        nowLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        // アプリを起動すると現在地に地図の中心を移動する
        if(mSetUp) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(nowLatLng).zoom(17f).bearing(0).build();
            // 地図の中心を取得した緯度、経度に動かす
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mSetUp = false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // 現在地をマップの中心にさせるボタンを追加
        mMap.setMyLocationEnabled(true);
    }

    /**
     * 行動メソッド
     * NEXTボタンを押すと呼ばれる
     */
    private void action() {
        Log.d("debug", "action");
        if(function(d)){
            upDate();
            Log.d("debug","現在地" + nowLatLng.toString());
        }else{
            if(d.getTask().getCommand() == command.exit) {
                soundPool.play(sounds[2], 0.5f, 0.5f, 0, 0, 1);
                finish();
            }
        }
    }

    /**
     * 表示のアップデート
     */
    private void upDate() {
        setSource();
        setTask();
        drawMemory();
        progressBar.setProgress(d.taskCursor + 1);
        barTV.setText(d.taskCursor + 1 + "/" + d.taskList.length);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("処理の説明");
        alertDialog.setMessage(d.getNote());
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        soundPool.play(sounds[1], 0.5f, 0.5f, 0, 0, 1);
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

    public void drawMemory(){
        mMap.clear();
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
                    .strokeWidth(1);

            //出現している
            if(param.isAppeared == true){
                //次の目的地なら
                if(key.equals(d.getTask().getTarget()))
                    rect.fillColor(0x60ff0000);
                else
                    rect.fillColor(0x600000ff);
                MarkerOptions marker = new MarkerOptions()
                        .position(param.getLocate());

                if(param.type == paramType.function)
                    marker.title(key + "(関数)");
                else {
                    if(param.getValue().equals("none"))
                        marker.title(key)
                                .snippet(param.getValue());
                    else
                        marker.title(key);
                }
                mMap.addMarker(marker);
            }
            //出現していない
            else{
                rect .fillColor(0x60ffffff);
            }
            mMap.addPolygon(rect);
        }
    }

    // 終了(リスタート)時の処理
    public void finish() {
        d.init();
        startButton.setEnabled(true);
        nextButton.setEnabled(false);
        textView1.setText(R.string.source);
        textView2.setText("");
        barTV.setText("0 / 0");
        progressBar.setProgress(0);
        selectLevel();
    }

    //このメソッドをアプリのメインクラスに実装
    // 各処理において、現在のタスクを出力
    public boolean function(final dataUtil d) {
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
                d.get_c(this);
                break;
            case input:
                inputNum();
                break;
            case move:
                if(!d.isCorrectLocation(nowLatLng))
                    return false;
                break;
            case output:
                //TODO ここにコンソール出力処理
                AlertDialog.Builder alertDialog0 = new AlertDialog.Builder(this);
                alertDialog0.setTitle("output");
                alertDialog0.setMessage(d.getOutputStr());
                alertDialog0.setCancelable(false);
                alertDialog0.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                alertDialog0.show();
                break;
            case add:
                d.add_c(this);
                break;
            case exit:
                return false;
            default:
                break;
        }
        d.next();
        return true;
    }


    /**
     * 入力受け付けメソッド
     */
    private void inputNum() {
        // Context引き渡し用変数
        final Activity activity = this;
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.input_dialog, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("入力受け付けダイアログ");
        alertDialog.setMessage(d.getTask().getText());
        alertDialog.setView(view);
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText editText = (EditText)view.findViewById(R.id.editText);
                if(editText.getText().toString().equals(""))
                    d.input_c(activity,"0");
                else{
                    String strNum = editText.getText().toString();
                    d.input_c(activity, strNum);
                    dialogInterface.dismiss();
                }
            }
        });


        alertDialog.show();
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
        // ソース呼び出し
        int itemId = item.getItemId();
        if (itemId == R.id.action_source) {
            // SourceActivityを呼び出すIntentを生成
            Intent intent = new Intent(this, SourceActivity.class);
            // textというパラメータを設定
            Log.d("debug", d.getSource());
            intent.putExtra("text", d.getSource());
            // startActivityでソースコードを呼び出す
            startActivity(intent);
        }
        // 選択ダイアログからやり直し
        else if(itemId == R.id.restart) {
            finish();
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