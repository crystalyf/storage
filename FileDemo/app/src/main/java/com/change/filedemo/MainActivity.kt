package com.change.filedemo

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.change.filedemo.util.FileSDCardUtil
import com.change.filedemo.util.HttpDownFileUtils
import com.change.filedemo.util.OnFileDownListener
import com.change.filedemo.util.PermissonsUtil
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    //在线图片
    //https://img1.baidu.com/it/u=1572085411,867992380&fm=26&fmt=auto
    private val imageUrl = "https://img1.baidu.com/it/u=1963877880,4176355358&fm=26&fmt=auto"
    private var context: Context? = null
    private var localPathList: MutableList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this@MainActivity
        PermissonsUtil.requestPermisson(this, PermissonsUtil.permissionList, null)
        initView()
    }


    private fun initView() {
        btn_save_image_to_public.setOnClickListener {
            saveFileToPublicDir(Environment.DIRECTORY_PICTURES)
        }
        btn_file_to_public_download.setOnClickListener {
            saveFileToPublicDir(Environment.DIRECTORY_DOWNLOADS)
        }
        btn_save_file_to_local.setOnClickListener {
            saveFileToToLocalDir(Environment.DIRECTORY_PICTURES)
        }
        btn_save_image_to_local.setOnClickListener {
            saveImageToLocalDir()
        }
        btn_get_local_path.setOnClickListener {
            getLocalPathList()
        }
    }

    /**
     * 存储文件至公共目录（共享存储目录）
     *
     * 包含了：
     *
     * 存储图片至公共目录【图片】（等等其他目录都行）
     * 存储文件至公共目录【下载】
     *
     */
    private fun saveFileToPublicDir(type: String) {
        HttpDownFileUtils.downFileFromServiceToPublicDir(
            imageUrl,
            this,
            type,
            object : OnFileDownListener {
                override fun onFileDownStatus(
                    status: Int,
                    `object`: Any?,
                    proGress: Int,
                    currentDownProGress: Long,
                    totalProGress: Long
                ) {
                    if (status == 1) {
                        Toast.makeText(this@MainActivity, "下载成功", Toast.LENGTH_SHORT).show()
                        if (`object` is File) {
                            val file = `object`
                        } else if (`object` is Uri) {
                            val uri = `object`
                        }
                    }
                }
            })
    }

    /**
     * 存储文件至私有存储目录
     *
     * 包含了：
     * 存储文件至私有目录【图片】（SDCard/Android/data/<你的应用包名>/files/Pictures）
     *
     */
    private fun saveFileToToLocalDir(type: String) {
        HttpDownFileUtils.downFileFromServiceToLocalDir(
            imageUrl,
            type,
            isNeedDeleteOldFile = false,
            isDownCacleDir = false,
            onFileDownListener = object : OnFileDownListener {
                override fun onFileDownStatus(
                    status: Int,
                    `object`: Any?,
                    proGress: Int,
                    currentDownProGress: Long,
                    totalProGress: Long
                ) {
                    if (status == 1) {
                        Toast.makeText(this@MainActivity, "下载成功", Toast.LENGTH_SHORT).show()
                        if (`object` is File) {
                            val file = `object`
                        } else if (`object` is Uri) {
                            val uri = `object`
                        }
                    }
                }
            })
    }

    /**
     * 存储图片至私有存储目录
     *
     * 包含了：
     * 存储图片至私有目录【图片】（SDCard/Android/data/<你的应用包名>/files/Pictures）
     *
     */
    private fun saveImageToLocalDir() {
        HttpDownFileUtils.downImageFromServiceToLocalDir(
            imageUrl,
            this@MainActivity,
            onFileDownListener = object : OnFileDownListener {
                override fun onFileDownStatus(
                    status: Int,
                    `object`: Any?,
                    proGress: Int,
                    currentDownProGress: Long,
                    totalProGress: Long
                ) {
                    if (status == 1) {
                        Toast.makeText(this@MainActivity, "下载成功", Toast.LENGTH_SHORT).show()
                        if (`object` is File) {
                            val file = `object`
                        } else if (`object` is Uri) {
                            val uri = `object`
                        }
                    }
                }
            })
    }

    /**
     * 获取私有外部存储的文件
     */
    private fun getLocalPathList() {
        var result = ""
        localPathList = FileSDCardUtil.getLocalFilePath()
        localPathList.let { it ->
            it?.forEach {
                result += it
                result += "\n"
            }
        }
        Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
    }

}