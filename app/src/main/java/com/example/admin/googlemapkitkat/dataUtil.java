package com.example.admin.googlemapkitkat;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Stack;

import static com.example.admin.googlemapkitkat.command.*;
import static com.example.admin.googlemapkitkat.paramType.*;

/**
 * 描画前にsetParamListを必ず実行
 */
public class dataUtil {
	final int none = Integer.MIN_VALUE;

	/** ユーザのアクションのリスト */
	task[] taskList;
	/** タスクリストを追うカーソル */
	int taskCursor = 0;

	/** Cのプログラムリスト（処理順のためソースプログラムと違う） */
	String[] progList;

	/** プログラムリストのカーソル */
	int progCursor = 0;

	/** 各タスクの説明文（カーソルはtaskCursor） */
	String[] notes ;

	/** 変数のマップ<変数名 , 変数型> */
	HashMap<String, param> paramMap;

	/** ソースプログラム（出力用） */
	String source = "";

	/** 計算,出力用のスタック */
	Stack<String> stack;

	/** メモリの一辺/2 */
	double r;

	/** メモリの数 */
	int memNumber;

	dataUtil() {
		stack = new Stack<String>();
	}

	public void readScript(Activity activity, String filepath, LatLng start, LatLng goal){
		try{
			InputStream is= activity.getResources().getAssets().open(filepath);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			setTaskList(br);
			setNotes(br);
			setParamList(br,start,goal);
			setSources(br);
		}catch(Exception e){
			Log.d("debug",e.getMessage());
		}
	}

	/**
	 * 変数リストを用意
	 */
	public void setParamList(BufferedReader br,LatLng start,LatLng goal) {
		paramMap = new HashMap<String,param>();
		String field;
		String[]fields;
		boolean ia;
		paramType type;
		try{
			br.readLine(); //&&を読み飛ばす
			memNumber = Integer.parseInt(br.readLine());
			LatLng[]center = new LatLng[memNumber];

//			double imargin = (goal.latitude - start.latitude)/memNumber;//メモリの緯度の間隔
//			double kmargin = (goal.longitude - start.longitude)/memNumber;//メモリの経度の間隔

			// 発表用
			double imargin = 0.002861023f/memNumber;
			double kmargin = 0;

			this.r = imargin > kmargin ? imargin*0.5f : kmargin*0.5f;
			this.r = r < 0?-r:r;

			for(int i = 0;i < memNumber;i++) {
				Log.d("debug",Integer.toString(i));
				center[i] = new LatLng(start.latitude + imargin * i, start.longitude + kmargin * i);
				field = br.readLine();
				fields = field.split(",");
				if (fields.length == 4) {
					type = toType(fields[1].trim());
					if (fields[3].trim().equals("t"))
						ia = true;
					else
						ia = false;
					paramMap.put(fields[0].trim(),new param(fields[0].trim(),type,fields[2].trim(),center[i],ia));
				}
				//ポインタの場合
				else {
					type = toType(fields[1].trim());
					if (fields[2].trim().equals("t"))
						ia = true;
					else
						ia = false;
					paramMap.put(fields[0].trim(),new param(fields[0].trim(),type,fields[2].trim(),center[i],ia,fields[4].trim()));
				}
			}
		}catch(Exception e){
			Log.d("debug",e.getMessage());
			System.exit(4);
		}
	}

	/**
	 * ユーザアクションのスクリプトを用意
	 */
	private void setTaskList(BufferedReader br) {
		String[]fields;
		String field;
		boolean read;
		int num;
		command command;
		try {
			field = br.readLine();
			fields = field.split(",");
			num = Integer.parseInt(fields[1]);
			br.readLine(); //&&を読み飛ばす

			taskList = new task[num];
			for(int i=0;i<num;i++){
				Log.d("debug",Integer.toString(i));
				field = br.readLine();
				fields = field.split(",");
				if(fields.length == 4){
					command = toCommand(fields[0].trim());
					if(fields[3].trim().equals("t"))
						read = true;
					else
						read = false;
					taskList[i] = new task(command,fields[1].trim(),fields[2].trim(),read);
				}
				else{
					command = toCommand(fields[0]);
					if(fields[2].trim().equals("t"))
						read = true;
					else
						read = false;
					taskList[i] = new task(command,fields[1].trim(),read);
				}
			}
		}catch(Exception e){
			Log.d("debug",e.getMessage());
			System.exit(2);
		}
	}

	public paramType toType(String str){
		if(str.equals("param"))
			return param;
		else if(str.equals("function"))
			return function;
		else if(str.equals("pointer"))
			return pointer;
		return null;
	}

	public command toCommand(String str){
		if(str.equals("get"))
			return get;
		else if(str.equals("input"))
			return input;
		else if(str.equals("substitude"))
			return substitude;
		else if(str.equals("move"))
			return move;
		else if(str.equals("output"))
			return output;
		else if(str.equals("initialize"))
			return initialize;
		else if(str.equals("display"))
			return display;
		else if(str.equals("add"))
			return add;
		else if(str.equals("exit"))
			return exit;
		return null;
	}

	private void setNotes(BufferedReader br){
		try {
			notes = new String[taskList.length];
			br.readLine(); //&&を読み飛ばす
			for(int i=0;i<notes.length;i++){
				notes[i] = br.readLine().trim();
			}
		}catch(Exception e){
			Log.d("debug",e.getMessage());
			System.exit(3);
		}
	}

	public void setSources(BufferedReader br){
		try{
			br.readLine(); //&&を読み飛ばす
			int num = Integer.parseInt(br.readLine());
			progList = new String[num];
			for(int i=0;i<num;i++){
				progList[i] = br.readLine();
			}

			br.readLine();//&&を読み飛ばす
			num = Integer.parseInt(br.readLine());
			for(int i=0;i<num;i++){
				source += br.readLine() + "\n";
			}
		}catch(Exception e) {
			Log.d("debug",e.getMessage());
			System.exit(5);
		}
	}

	public void init(){
		taskCursor = 0;
		progCursor = 0;
	}

	/**
	 * 次の命令へ(条件が合えば次のコードへ)
	 */
	public boolean next() {
		if (getTask().isReading())
			progCursor++;
		taskCursor++;
		if (getTask().getCommand() == exit)
			return false;
		return true;
	}

	/**
	 * substitude命令 : スタックから値を得て、指定された変数に代入
	 */
	public void substitude_c() {
		String value = stack.pop();
		paramMap.get(getTask().getTarget()).setValue(value);
		popStack();
	}

	/**
	 * get命令 スタックに指定された変数の値を積む
	 */
	public void get_c(Activity activity) {
		String str = "";
		param param = paramMap.get(getTask().getTarget());
		if(param.type == paramType.pointer)
			str = param.getPointer() +  "のアドレス";
		else
			str = param.getValue();
		stack.push(str);
		pushStask(activity, str);
	}

	/**
	 * 入力された文字(数値でなければいけない)をスタックに積む (型は検証されていない)
	 */
	public void input_c(Activity activity , String num_s) {
		stack.push(num_s);
		pushStask(activity ,num_s);
	}

	/**
	 * add命令 スタックに演算結果を積む
	 */
	public void add_c(Activity activity) {
		int a = Integer.parseInt(stack.pop());
		int b = Integer.parseInt(stack.pop());
		stack.push(Integer.toString(a + b));
		popStack();
		popStack();
		pushStask(activity, Integer.toString(a + b));
	}

	/**
	 * initialize命令 : 変数の出現(isAppeared)をtrueにする
	 */
	public void initialize_c() {
		paramMap.get(getTask().getTarget()).isAppeared = true;
	}

	/**
	 * 出力する（output命令）文字列を返す
	 *
	 * @return　出力用の文字列
	 */
	public String getOutputStr() {
		String result = "";
		result += getTask().getTarget() + " = ";
		result += stack.pop();
		popStack();
		return result;
	}

	FrameLayout frameLayout0, frameLayout1;
	TextView stackText0, stackText1;

	/**
	 * スタックに値をpush
	 */
	public void pushStask(Activity activity, String str) {
		if(stackText0 == null) {
			frameLayout0 = (FrameLayout) activity.findViewById(R.id.frame0) ;
			stackText0 = new TextView(activity);
			stackText0.setText(str);
			stackText0.setTextSize(18f);
			stackText0.setTextColor(Color.parseColor("#0000FF"));
			stackText0.setGravity(Gravity.CENTER);
//			stackText0.setBackgroundColor(Color.argb(220, 255, 255, 255));
			stackText0.setBackground(activity.getResources().getDrawable(R.drawable.frame_style));

			frameLayout0.addView(stackText0);
		} else {
			frameLayout1 = (FrameLayout) activity.findViewById(R.id.frame1);
			stackText1 = new TextView(activity);
			stackText1.setText(str);
			stackText1.setTextSize(18f);
			stackText1.setTextColor(Color.parseColor("#0000FF"));
			stackText1.setGravity(Gravity.CENTER);
//			stackText1.setBackgroundColor(Color.argb(220, 255, 255, 255));
			stackText1.setBackground(activity.getResources().getDrawable(R.drawable.frame_style));
			frameLayout1.addView(stackText1);
		}
	}

	/**
	 * スタックから値をpop
	 */
	public void popStack() {
		if(stackText1 != null) {
			frameLayout1.removeAllViews();
			stackText1 = null;
		} else {
			frameLayout0.removeAllViews();
			stackText0 = null;
		}
	}

	/**
	 * 現在向かうべき場所を返す
	 *
	 * @return
	 */
	public LatLng getLocate() {
		return paramMap.get(getTask().getTarget()).getLocate();
	}

	public boolean isCorrectLocation(LatLng currentLocate){
		return true;
//		LatLng correctLocate = getLocate();
//		Log.d("debug",getTask().getTarget() + ": correct;" + correctLocate.toString());
//		Log.d("debug","r:" + Double.toString(r));
//		if(currentLocate.latitude <= correctLocate.latitude + this.r &&
//				currentLocate.latitude >= correctLocate.latitude - this.r &&
//				currentLocate.longitude <= correctLocate.longitude + this.r &&
//				currentLocate.longitude >= correctLocate.longitude - this.r
//				){
//			return true;
//		}
//		return false;
	}

	/**
	 * 現在のタスク取得
	 *
	 * @return　現在のタスク
	 */
	public task getTask() {
		return taskList[taskCursor];
	}

	/**
	 * 現在のコード取得
	 *
	 * @return　現在のコード
	 */
	public String getCode() {
		return progList[progCursor];
	}

	/**
	 * 現在の説明文取得
	 * @return
	 */
	public String getNote() {return notes[taskCursor];	}
	/**
	 *
	 * @return ソースコード
	 */
	public String getSource() {
		return source;
	}
}