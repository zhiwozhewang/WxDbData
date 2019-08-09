package com.example.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

const val test = ""

class MainActivity : AppCompatActivity(), View.OnClickListener {


    private lateinit var binding: com.example.myapplication.databinding.ActivityMainBinding

    //    internal lateinit var myBinder: GohnsonServiceOld.MyBinder
    var gohnsonServiceOld: GohnsonServiceOld? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        // 第一种将数据填充到xml文件中的方法(代码在下面这行)，我们直接实例化了一个MainViewModel赋值给BR资源中一个叫viewModel的变量
        // binding.setVariable(BR.viewModel, MainViewModel())
        // 以下是一些说明：
        // BR就是前文提到的DataBinding资源，像R文件一样自动生成，记录所有xml中data标签内的变量名称，有点像控件id的感觉
        // viewModel来自布局文件中data标签内的variable标签中的name

        // 第二种将数据填充到xml文件中的方法(代码在下面这行),viewModel这个变量名视你在xml中variable标签中的name而定
        binding.viewModel = MainViewModel()
        // 假如你的name为user,并且class名称也为User的话(name和class的名称不一定要相同)
        // 那么代码就是binding.user = User()

        // java 代码如下
        // binding.setViewModel(new MainViewModel())
        // binding.setUser(new User())
        num_text.text = "sdas"
        //
        //上下文，和.class文件的写法
        val serverIntent = Intent(MainActivity@ this, GohnsonServiceOld::class.java)
        val myConnection = MyConnection()
        bindService(serverIntent, myConnection, Context.BIND_AUTO_CREATE)

        click_button.setOnClickListener(this)

    }

    override fun onStart() {
        super.onStart()

    }

    override fun onDestroy() {
        super.onDestroy()
        // 在Activity销毁时记得解绑，以免内存泄漏
        binding.unbind()
    }

    override fun onClick(p0: View?) {
        gohnsonServiceOld!!.initBaseContnet()
    }

    internal inner class MyConnection : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            var myBinder = p1 as GohnsonServiceOld.MyBinder
            gohnsonServiceOld = myBinder.service
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
        }
    }
}

