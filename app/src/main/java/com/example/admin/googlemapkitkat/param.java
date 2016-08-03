package com.example.admin.googlemapkitkat;

import com.google.android.gms.maps.model.LatLng;

/**
 * 変数のデータ 名前、値、座標
 *
 * @author admin
 *
 */
public class param {
	final static int NOPARAM = Integer.MAX_VALUE;
	final static int NONE = Integer.MIN_VALUE;

	/** 変数名 */
	private String name;
	/** 実際の変数の値 */
	private int number;
	/** 変数のマップ上の座標 */
	private LatLng locate;
	/** ポインタの場合、参照先を入れる */
	private String pointer;
	/** 変数が出現したかどうか */
	public boolean isAppeared;


	/**
	 * 通常の変数用
	 *
	 * @param pn
	 * @param param
	 * @param ll
	 * @param ia
	 */
	param(String pn, int param, LatLng ll, boolean ia) {
		name = pn;
		number = param;
		locate = ll;
		isAppeared = ia;
		pointer = "";
	}
	param(String pn, int param, LatLng ll, boolean ia , String p) {
		name = pn;
		number = param;
		locate = ll;
		isAppeared = ia;
		pointer = p;
	}

	public String getName() {return name;}
	public void setName(String name) {	this.name = name;}

	public LatLng getLocate() {return locate;}
	public void setLocate(LatLng locate) {	this.locate = locate;}

	public int getNumber() {	return number;}
	public void setNumber(int number) {	this.number = number;}

	public String getPointer() {return pointer;	}
	public void setPointer(String pointer) {	this.pointer = pointer;}

	@Override
	public String toString() {
		return "param{" +
				"name='" + name + '\'' +
				", number=" + number +
				", locate=" + locate +
				", pointer='" + pointer + '\'' +
				", isAppeared=" + isAppeared +
				'}';
	}
}