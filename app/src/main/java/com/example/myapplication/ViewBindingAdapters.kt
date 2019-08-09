package com.example.myapplication


import android.databinding.BindingAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide

object ViewBindingAdapters {

    // 包裹范围内 属于静态方法
    // 当为false时
    @BindingAdapter(value = ["app:imgUrl", "app:bgRes"], requireAll = false)
    fun setImgUrl(view: ImageView, url: String, res: Int) {
        Glide.with(view).load(url).into(view)
        view.setBackgroundResource(res)
    }


}

