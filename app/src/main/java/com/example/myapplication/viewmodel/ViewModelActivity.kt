package com.example.myapplication.viewmodel

import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.myapplication.R
//import com.example.myapplication.databinding.ActivityViewModelBinding

//class ViewModelActivity : AppCompatActivity() {
//    private lateinit var tv: TextView
//    private lateinit var bt: Button
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_view_model)
//        tv = findViewById(R.id.tv_vm)
//        bt = findViewById(R.id.bt_vm)
//        val userViewModel = ViewModelProviders.of(this).get(UserViewModel::class.java)
//        // 监听ViewModel中user的变化，当它变化时，将TextView重新设置文字
//        userViewModel.user.observe(this, Observer {
//            tv.text = it?.name
//        })
//        // 为按钮设置点击事件，点击后设置user的值
//        bt.setOnClickListener {
//            val user = User("张三", 21, 1)
//            userViewModel.user.value = user
//            // Java代码
//            // userViewModel.user.setValue(user)
//        }
//    }
//}
class ViewModelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_model)
        val userViewModel = ViewModelProviders.of(this).get(UserViewModel::class.java)
        val binding = DataBindingUtil.setContentView<com.example.myapplication.databinding.ActivityViewModelBinding>(
            this,
            R.layout.activity_view_model
        )
        // Java代码
        // ActivityViewModelBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_view_model)

        binding.viewModel = userViewModel
        // java代码
        // binding.setViewModel(userViewModel)

        // 让xml内绑定的LiveData和Observer建立连接，也正是因为这段代码，让LiveData能感知Activity的生命周期
        binding.setLifecycleOwner(this)
    }
}

