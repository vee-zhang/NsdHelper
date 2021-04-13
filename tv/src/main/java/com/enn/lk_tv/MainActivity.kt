package com.enn.lk_tv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.enn.nsdhelper.NsdHelper
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {


    private lateinit var tv: TextView
    private lateinit var tvResult: TextView

    private val handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv = findViewById(R.id.tv)
        tvResult = findViewById(R.id.tv_result)


        NsdHelper.addReceiverListener(object : NsdHelper.OnReceiveListener {
            override fun onReceive(bytes: ByteArray) {
                handler.post {
                    tvResult.text = String(bytes, Charset.forName("utf8"))
                }
            }
        })

        NsdHelper.regist(this, {
            handler.post {
                tv.text = "${it.serviceName}服务注册成功！"
            }
        }) { it, code ->
            handler.post {
                tv.text = "${it.serviceName}服务注册失败：$code"
            }
        }
    }


    override fun onDestroy() {
        NsdHelper.unregist()
        super.onDestroy()
    }
}