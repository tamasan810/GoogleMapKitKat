package com.example.admin.googlemapkitkat;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Stack;

import static com.example.admin.googlemapkitkat.command.*;

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

	/** 変数のマップ<変数名 , 変数型> */
	HashMap<String, param> paramMap;

	/** ソースプログラム（出力用） */
	String source;

	/** 計算,出力用のスタック */
	Stack<Integer> stack;

	/** メモリの一辺/2 */
	double r;

	/** メモリの数 */
	int memNumber = 6;

	dataUtil() {
		setTaskList();
		stack = new Stack<Integer>();
		String[] progList = { "int main(void){", "int a = ?,b = ?;",
				"sum(&a,b);", "void sum(int *sum,int b){", "*sum = *sum + b;",
				"}", "printf(\"a = %d\",a);", "printf(\"b = %d\",b);",
				"return 0;", "}" };
		this.progList = progList;

		source = "void sum(int *sum,int b_){\n" + "\t*sum = *sum + b_;\n"
				+ "}\n\n" + "int main(void){\n" + "\tint a = ?,b = ?;\n"
				+ "\tprintf(\"a = %d\",a);\n" + "\tprintf(\"b = %d\",b);\n"
				+ "\treturn 0;\n" + "}";
	}

	/**
	 * 変数リストを用意
	 */
	public void setParamList(LatLng start,LatLng goal) {
		/** TODO 座標指定 */

		LatLng[]center = new LatLng[memNumber];

		double imargin = (goal.latitude - start.latitude)/memNumber;//メモリの緯度の間隔
		double kmargin = (goal.longitude - start.longitude)/memNumber;//メモリの経度の間隔

		this.r = imargin > kmargin ? imargin*0.5f : kmargin*0.5f;
		this.r = r < 0?-r:r;

		for(int i = 0;i < memNumber;i++){
			center[i] = new LatLng(start.latitude + imargin*i,start.longitude + kmargin*i );
		}


		paramMap = new HashMap<String, param>();
		paramMap.put("main", new param("main", param.NOPARAM, center[0],
				true));
		paramMap.put("a", new param("a", param.NONE, center[1], false));
		paramMap.put("b", new param("b", param.NONE, center[2], false));

		paramMap.put("sum", new param("sum", param.NOPARAM, center[3],
				false));
		paramMap.put("*sum", new param("*sum", param.NONE, center[4],
				false));
		paramMap.put("b_", new param("b_", param.NONE, center[5], false));
	}

	/**
	 * ユーザアクションのスクリプトを用意
	 */
	private void setTaskList() {
		taskList = new task[43];
		taskList[0] = new task(display, "main関数開始", true);
		// int a=?
		taskList[1] = new task(initialize, "aを初期化します", "a", false);
		taskList[2] = new task(input, "aに代入する値を入力", "a", false);
		taskList[3] = new task(move, "aに移動する", "a", false);
		taskList[4] = new task(substitude, "aに値を代入", "a", false);
		// ,b=?;
		taskList[5] = new task(initialize, "bを初期化する", "b", false);
		taskList[6] = new task(input, "bに代入する値を入力", "b", false);
		taskList[7] = new task(move, "bに移動する", "b", false);
		taskList[8] = new task(substitude, "bに値を代入", "b", true);
		// sum(&a,b)
		taskList[9] = new task(initialize, "sum関数呼び出し", "sum", true);
		taskList[10] = new task(move, "sum関数に移動", "sum", false);

		taskList[11] = new task(display, "引数を初期化します", false);
		// ポインタ*sum設定
		taskList[12] = new task(initialize, "*sumを初期化", "*sum", false);
		taskList[13] = new task(move, "aのアドレスを得るためaに移動", "a", false);
		taskList[14] = new task(display, "aのアドレスを得ました", false);
		// 引数bを初期化
		taskList[15] = new task(initialize, "b_を初期化", "b_", false);
		taskList[16] = new task(move, "b_に代入するためのbの値を取得するため、bに移動", "b", false);
		taskList[17] = new task(get, "bから値を取得", "b", false);
		taskList[18] = new task(substitude, "b_に値を代入", "b_", true);

		taskList[19] = new task(display, "演算をするために*sum,b_を取得します", false);
		taskList[20] = new task(display, "まず*sumの値を取得します", false);
		taskList[21] = new task(move, "*sumに移動", "*sum", false);
		taskList[22] = new task(move, "ポインタ*sumの参照するる変数aに移動", "a", false);
		taskList[23] = new task(get, "a(*sum)から値を取得", "a", false);

		taskList[24] = new task(display, "次にb_を取得します", false);
		taskList[25] = new task(move, "b_に移動", "b_", false);
		taskList[26] = new task(get, "b_の値を取得", "b_", false);

		taskList[27] = new task(add, "*sum + b_を実行", false);

		taskList[28] = new task(display, "*sumに計算結果を代入します", false);
		taskList[29] = new task(move, "*sumに移動", "*sum", false);
		taskList[30] = new task(move, "ポインタ*sumの参照する変数aに移動", "a", false);
		taskList[31] = new task(substitude, "a(*sum)に値を代入", "a", true);

		taskList[32] = new task(move, "sumの実行が終了したため、mainに移動", "main", true);
		taskList[33] = new task(display, "これかa,bら出力処理を行います", false);

		taskList[34] = new task(move, "aに移動", "a", false);
		taskList[35] = new task(get, "aの値を取得", "a", false);
		taskList[36] = new task(output, "aを出力", "a", true);

		taskList[37] = new task(move, "bに移動", "b", false);
		taskList[38] = new task(get, "bの値を取得", "b", false);
		taskList[39] = new task(output, "bを出力", "b", true);

		taskList[40] = new task(display, "最後にreturn 0をする", true);
		taskList[41] = new task(exit, "正常終了！", true);
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
		int num = stack.pop();
		paramMap.get(getTask().getTarget()).setNumber(num);
	}

	/**
	 * get命令 スタックに指定された変数の値を積む
	 */
	public void get_c() {
		param param = paramMap.get(getTask().getTarget());
		int num = param.getNumber();
		stack.push(num);
	}

	/**
	 * 入力された文字(数値でなければいけない)をスタックに積む (型は検証されていない)
	 */
	public void input_c(String num_s) {
		Log.d("debug", "input_c");
		int num = Integer.parseInt(num_s);
		stack.push(num);
		if(stack.isEmpty()) {
			Log.d("debug", String.valueOf(num) + "is pushed!");
		}
	}

	/**
	 * add命令 スタックに演算結果を積む
	 */
	public void add_c() {
		int a = stack.pop();
		int b = stack.pop();
		stack.push(a + b);
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
		result += Integer.toString(stack.pop());
		return result;
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
		LatLng correctLocate = getLocate();
		if(currentLocate.latitude <= correctLocate.latitude + r &&
				currentLocate.latitude >= correctLocate.latitude - r &&
				currentLocate.longitude <= correctLocate.longitude + r &&
				currentLocate.longitude >= correctLocate.longitude - r
				){
			return true;
		}
		return false;
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
	 *
	 * @return ソースコード
	 */
	public String getSource() {
		return source;
	}

}