package com.enn.nsdhelper


import android.app.Activity
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.HandlerThread
import java.net.DatagramPacket
import java.net.DatagramSocket

object NsdHelper {

    const val SERVICE_NAME = "LKTV2"
    const val SERVICE_TYPE = "_lktv._udp."
    const val BYTE_SIZE = 2 * 1024


    private lateinit var nsdManager: NsdManager

    private var socket: DatagramSocket? = null

    private val handlerThread = HandlerThread("receiverThread")

    private val callbackList = mutableListOf<OnReceiveListener>()


    /**
     * 添加监听
     */
    fun addReceiverListener(listener: OnReceiveListener) {
        this.callbackList.add(listener)
    }

    /**
     * 移除监听
     */
    fun removeReceiverListener(listener: OnReceiveListener) {
        this.callbackList.remove(listener)
    }


    /**
     * 清除监听
     */
    fun clearReceiverListener() {
        this.callbackList.clear()
    }


    /**
     * 注册服务信息
     */
    fun regist(
        activity: Activity,
        onSuccess: ((serviceInfo: NsdServiceInfo) -> Unit)? = null,
        onFaile: ((serviceInfo: NsdServiceInfo, errorCode: Int) -> Unit)? = null
    ) {
        this.socket = DatagramSocket(0)
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = socket?.localPort ?: -1
        }
        nsdManager = activity.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.RegistrationListener {

                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    onSuccess?.invoke(serviceInfo)
                    receive()
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    onFaile?.invoke(serviceInfo, errorCode)
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                }
            })
    }

    /**
     * 接收消息
     */
    private fun receive() {
        handlerThread.start()
        val bytes = ByteArray(BYTE_SIZE)
        val receiverPacket = DatagramPacket(bytes, bytes.size)
        while (!handlerThread.isInterrupted) {
            socket?.receive(receiverPacket)
            for (listener in callbackList) {
                listener.onReceive(bytes)
            }
        }
        socket?.close()
    }

    /**
     * 取消注册
     */
    fun unregist(
        onSuccess: ((serviceInfo: NsdServiceInfo) -> Unit)? = null,
        onFaile: ((serviceInfo: NsdServiceInfo, errorCode: Int) -> Unit)? = null
    ) {
        try {
            handlerThread.quit()
            nsdManager.unregisterService(object : NsdManager.RegistrationListener {

                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {

                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {

                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    onSuccess?.invoke(serviceInfo)
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    onFaile?.invoke(serviceInfo, errorCode)
                }
            })
        } catch (e: Exception) {
        }
    }

    interface OnReceiveListener {
        fun onReceive(bytes: ByteArray)
    }
}