package com.example.myapplication.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

// 假设我们要存放的UI数据就是User对象
// 先新建一个实体对象
data class User(val name: String, val age: Int, val sex: Int)

// 新建ViewModel类
// 新建ViewModel类
class UserViewModel : ViewModel() {
//    val user = User("张三", 21, 1)
    val user = MutableLiveData<User>()
}
