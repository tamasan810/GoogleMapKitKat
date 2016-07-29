package com.example.admin.googlemapkitkat;
/**
 * ユーザにやらせる行動の列挙型
 * @author admin
 *get 		: 変数から値を取得し、push
 *input 	: 入力を受け付け、push
 *substitude:popし、変数に代入	okで次
 *move		:移動			
 *output	:出力			okで次
 *initialize:変数初期化（出現）	okで次
 *display	:画面表示のみ		okで次
 *add		:加算処理			2つpopし、1つpush
 *exit		:終了処理
 */
public enum command {
	display,initialize,substitude,get,input,move,output,add,exit
}