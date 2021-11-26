package com.change.filedemo.util

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


/** Created by Fenrir-xingjunchao on 11/23/21. **/
object ImageUtil {

    /**
     *  通过http URL 获取图片流 转为字节数组
     */
     fun getImageByteArrayFromNet(url: String): ByteArray? {
        val urlConet = URL(url)
        val con: HttpURLConnection = urlConet.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        con.connectTimeout = 4 * 1000
        val inStream: InputStream = con.inputStream //通过输入流获取图片数据
        val outStream = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        var len = 0
        while (inStream.read(buffer).also { len = it } != -1) {
            outStream.write(buffer, 0, len)
        }
        inStream.close()
        return outStream.toByteArray()
    }

}