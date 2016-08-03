package com.example.admin.googlemapkitkat;

import com.google.android.gms.maps.model.LatLng;

/**
 * 変数のデータ 名前、値、座標
 *
 * @author admin
 *
 */
public class param {
	/** 変数名 */
	private String name;
	/** 変数のタイプ */
	paramType type;
	/** 実際の変数の値 */
	private String value;
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
	 * @param value
	 * @param ll
	 * @param ia
	 */
	param(String pn, paramType type,String value, LatLng ll, boolean ia) {
		name = pn;
		this.type = type;
		this.value = value;
		locate = ll;
		isAppeared = ia;
		pointer = "";
	}
	param(String pn,paramType type,String value, LatLng ll, boolean ia , String p) {
		name = pn;
		this.type = type;
		this.value = value;
		locate = ll;
		isAppeared = ia;
		pointer = p;
	}

	public String getName() {return name;}
	public void setName(String name) {	this.name = name;}

	public LatLng getLocate() {return locate;}
	public void setLocate(LatLng locate) {	this.locate = locate;}

	public String getValue() {	return value;}
	public void setValue(String value) {	this.value = value;}

	public String getPointer() {return pointer;	}
	public void setPointer(String pointer) {	this.pointer = pointer;}

	@Override
	public String toString() {
		return "param{" +
				"name='" + name + '\'' +
				"type="+ type + '\'' +
				", value=" + value +
				", locate=" + locate +
				", pointer='" + pointer + '\'' +
				", isAppeared=" + isAppeared +
				'}';
	}
}