package com.example.myapplication

import android.databinding.InverseMethod

class ViewInverseMethods {

    // 这个注解参数为反转的方法名，意味着一个这个注解需要两个方法才能完成
    @InverseMethod("sexToNum")
    fun numToSex(num: Int): String {
        return when (num) {
            0 -> "女"
            1 -> "男"
            else -> "未知性别"
        }
    }

     fun  sexToNum(sex: String): Int {
        return when (sex) {
            "女" -> 0
            "男" -> 1
            else -> 2
        }
    }
}


