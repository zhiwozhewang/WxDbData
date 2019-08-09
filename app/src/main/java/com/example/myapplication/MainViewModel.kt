package com.example.myapplication

import android.databinding.ObservableField
import android.databinding.ObservableInt
import android.util.Log

class MainViewModel {
    var name = "张三"
    //将变量age声明为可观察的Int对象。
    var age = ObservableInt(15)
    var isMan = true
    val text = ObservableField<String>("")
    val imgUrl = "http://image.uczzd.cn/14094444771339725257.png?id=0"
    // 因为涉及双向绑定，这里必须是可观察的数据类型
    val num = ObservableInt(1)
    fun log() {
        Log.d("MyTAG", "按钮被点击了一下")
    }
    fun oneYearLater() {
        val lastAage = age.get()
        age.set(lastAage + 1)
        Log.d("MyTAG", "年龄：$age")
    }
}

