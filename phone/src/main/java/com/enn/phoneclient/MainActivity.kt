package com.enn.phoneclient

import android.R.attr.port
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import java.net.DatagramPacket
import java.net.DatagramSocket


class MainActivity : AppCompatActivity() {

    companion object {
        val SERVICE_TYPE = "_lktv._udp."
    }


    private val TAG = "phone_client"

    private lateinit var nsdManager: NsdManager

    private lateinit var searchViewGroup: ViewGroup
    private lateinit var vs: ViewStub

    private lateinit var btnSearch: View
    private lateinit var editText: EditText
    private lateinit var btnSend: View

    private lateinit var tvContent: TextView

    private lateinit var rv: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
        nsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager)

    }

    /**
     * 页面初始化
     */
    private fun initView() {
        this.searchViewGroup = findViewById(R.id.ll_search)
        this.vs = findViewById(R.id.vs)
        this.btnSearch = findViewById(R.id.btn_search)
        rv = findViewById(R.id.rv)
        rv.adapter = adapter
    }


    /**
     * 查找服务
     */
    public fun searchService(v: View) {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }


    /**
     * 服务发现回调
     */
    private val nsdServiceInfoList = mutableListOf<NsdServiceInfo>()

    private val handler = Handler(Looper.getMainLooper()) {

        if (it.what == 2000) {
            adapter.update(nsdServiceInfoList)

        } else {

            searchViewGroup.visibility = View.GONE
            vs.inflate()
            btnSend = findViewById(R.id.btn_send)
            tvContent = findViewById(R.id.tv_content)
            editText = findViewById(R.id.et)


            // todo 发送消息

            btnSend.setOnClickListener {

                Thread {
                    val socket = DatagramSocket()
                    val byteArray = editText.text.toString().toByteArray()
                    val packet = DatagramPacket(
                        byteArray,
                        byteArray.size, targetServiceInfo.host, targetServiceInfo.port
                    )

                    socket.send(packet)
                    socket.send(packet)
                    socket.send(packet)
                    socket.close()
                }.start()

            }
        }
        true
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {

            if (service.serviceType == SERVICE_TYPE) {
                nsdServiceInfoList.add(service)

                Message.obtain().apply {
                    what = 2000
                    target = handler
                }.run {
                    sendToTarget()
                }

                // todo 刷新列表


            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    /**
     * 服务连接回调
     */
    private lateinit var targetServiceInfo: NsdServiceInfo
    private val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            targetServiceInfo = serviceInfo
            Message.obtain().apply {
                what = 1000
                target = handler
            }.run {
                sendToTarget()
            }
        }
    }

    private val adapter = NsdServiceInfoAdapter(object : OnClickListener {
        override fun onClick(service: NsdServiceInfo) {
            resolveService(service)
        }

    })

    /**
     * 确定服务
     */
    fun resolveService(service: NsdServiceInfo) {
        nsdManager.resolveService(
            service,
            resolveListener
        )
    }


    override fun onPause() {
//        nsdHelper?.tearDown()

        // todo 停止发现
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        //todo 发现服务
//        nsdHelper?.apply {
//            registerService(connection.localPort)
//            discoverServices()
//        }
    }

    override fun onDestroy() {
//        nsdHelper?.tearDown()
//        connection.tearDown()

        //todo 停止发现
        //todo 关闭 socket
        super.onDestroy()
    }


}

class VH(itemView: View, private val listener: OnClickListener) :
    RecyclerView.ViewHolder(itemView) {
    private lateinit var service: NsdServiceInfo

    init {
        itemView.setOnClickListener {
            listener.onClick(this.service)
        }
    }

    fun bind(service: NsdServiceInfo) {
        this.service = service
        (itemView as TextView).text = service.serviceName
    }
}

interface OnClickListener {
    fun onClick(service: NsdServiceInfo)
}

class NsdServiceInfoAdapter(private val listener: OnClickListener) : RecyclerView.Adapter<VH>() {
    private val list = mutableListOf<NsdServiceInfo>()

    fun update(list: List<NsdServiceInfo>) {
        this.list.clear()
        this.list.addAll(list)
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        return VH(v, this.listener)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size


}