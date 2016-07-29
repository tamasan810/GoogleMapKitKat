package com.example.admin.googlemapkitkat;

/**
 * ユーザのアクションについてのクラス
 * 
 * @author admin
 * 
 */
public class task {
	final static String NOTARGET = "no";
	/** コマンド */
	private command command;
	/** アプリ上で出力する文字列 */
	private String text;
	/** 目標の変数の名前(moveなら移動先 , substitudeなら代入する対象) */
	private String target;
	/** プログラムを読み飛ばすか */
	private boolean isReading;

	public task(com.example.admin.googlemapkitkat.command command,
				String text, String target,
			boolean isReading) {
		this.command = command;
		this.text = text;
		this.target = target;
		this.isReading = isReading;
	}

	/**
	 * display用のコンストラクタ ターゲットはなし
	 * 
	 * @param command
	 * @param text
	 * @param isReading
	 */
	public task(com.example.admin.googlemapkitkat.command command,
				String text, boolean isReading) {
		this.command = command;
		this.text = text;
		this.target = NOTARGET;
		this.isReading = isReading;
	}

	public command getCommand() {
		return command;
	}

	public void setCommand(command command) {
		this.command = command;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public boolean isReading() {
		return isReading;
	}

	@Override
	public String toString() {
		return "task [command=" + command + ", text=" + text + ", target="
				+ target + ", isReading=" + isReading + "]";
	}

}