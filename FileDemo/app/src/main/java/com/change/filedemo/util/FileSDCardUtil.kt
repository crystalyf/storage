package com.change.filedemo.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import com.change.filedemo.application.MyApplication
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.sql.Date
import java.text.SimpleDateFormat


/**
 * 沙盒外部公共目录 : 公共共享目录
 *
 * 沙盒目录： app的私有存储
 *
 */

object FileSDCardUtil {
    /**
     * @date: 2019/5/22 0022
     * @author: gaoxiaoxiong
     * @description: 外部存储图片保存的位置   （SDCard/Android/data/<你的应用包名>/files/Pictures）
     */
    val publicDiskImagePicDir: String
        get() = getPublicDiskFileDir(
            MyApplication.instance.applicationContext,
            Environment.DIRECTORY_PICTURES
        )

    /**
     * @date: 2019/5/22 0022
     * @author: gaoxiaoxiong
     * @description: 外部存储电影保存的位置
     */
    val publicDiskMoviesDir: String
        get() = getPublicDiskFileDir(
            MyApplication.instance.applicationContext,
            Environment.DIRECTORY_MOVIES
        )

    /**
     * @date :2019/12/16 0016
     * @author : gaoxiaoxiong
     * @description: 外部存储音乐保存的位置
     */
    val publicDiskMusicDir: String
        get() = getPublicDiskFileDir(
            MyApplication.instance.applicationContext,
            Environment.DIRECTORY_MUSIC
        )


    //----------------------------------------

    /**
     *  context.getCacheDir()  专门用于存放缓存数据。
    用户对app进行缓存清理的时候会清理缓存目录cache的数据，手机空间不足的时候系统也会对缓存目录内的数据进行清理。
    开发者仍要管理好缓存数据特别是内部存储的缓存，避免缓存数据过大。
     */


    /**
     * @date 创建时间:2018/12/20
     * @author GaoXiaoXiong
     * @Description: 创建保存图片的缓存目录
     */
    val publicDiskImagePicCacheDir: String
        get() = getPublicDiskCacheDir(
            MyApplication.instance.applicationContext,
            Environment.DIRECTORY_PICTURES
        )

    /**
     * @date: 2019/6/21 0021
     * @author: gaoxiaoxiong
     * @description:获取音乐的缓存目录
     */
    val publicDiskMusicCacheDir: String
        get() = getPublicDiskCacheDir(
            MyApplication.instance.applicationContext,
            Environment.DIRECTORY_MUSIC
        )

    /**
     * @date: 创建时间:2019/12/22
     * @author: gaoxiaoxiong
     * @descripion:获取视频的缓存目录
     */
    val publicDiskMoviesCacheDir: String
        get() = getPublicDiskCacheDir(
            MyApplication.instance.applicationContext,
            Environment.DIRECTORY_MOVIES
        )

    /**
     * 作者：GaoXiaoXiong
     * 创建时间:2019/1/26
     * 注释描述:获取缓存目录
     *
     * @fileName 获取外部存储目录下缓存的 fileName的文件夹路径
     */
    private fun getPublicDiskCacheDir(context: Context, fileName: String): String {
        var cachePath: String? =
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) { //此目录下的是外部存储下的私有的fileName目录
                //SDCard/Android/data/你的应用包名/cache/fileName
                context.externalCacheDir!!.path + "/" + fileName
            } else {
                context.cacheDir.path + "/" + fileName
            }
        val file = File(cachePath)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath //SDCard/Android/data/你的应用包名/cache/fileName
    }

    /**
     * @date: 2019/8/2 0002
     * @author: gaoxiaoxiong
     * @description:获取外部存储目录下的 fileName的文件夹路径
     */
    private fun getPublicDiskFileDir(context: Context, fileName: String): String {
        var cachePath: String? =
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) { //此目录下的是外部存储下的私有fileName目录
                //私有存储空间的获取都需要使用Context，当然Activity中也可以省略
                context.getExternalFilesDir(fileName)!!.absolutePath //mnt/sdcard/Android/data/com.my.app/files/fileName
            } else {
                context.filesDir.path + "/" + fileName //data/data/com.my.app/files
            }
        val file = File(cachePath)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath //mnt/sdcard/Android/data/com.my.app/files/fileName
    }

    /**
     * @date :2020/3/17 0017
     * @author : gaoxiaoxiong
     * @description:获取公共目录，注意，只适合android9.0以下的
     */
    fun getPublicDiskFileDirAndroid9(fileDir: String?): String {
        var filePath: String? = null
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
            filePath = Environment.getExternalStoragePublicDirectory(fileDir).path
        }
        val file = File(filePath)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath
    }

    /**
     * @date: 创建时间:2020/1/13
     * @author: gaoxiaoxiong
     * @descripion:拷贝公共目录下的图片到沙盒cache目录
     */
    fun copyPublicDirPic2LocalCacheDir(
        context: Context,
        uriList: List<Uri>,
        onCopyPublicFile2PackageListener: OnCopyPublicFile2PackageListener?
    ) {
        var path = publicDiskImagePicCacheDir
        if (!path.endsWith("/")) {
            path = "$path/"
        }
        val resultPath = path
        Observable.just(uriList).subscribeOn(Schedulers.newThread()).map(
            Function<List<Uri?>, List<File>> { uriList ->
                val newFilesList: MutableList<File> = ArrayList()
                try {
                    for (i in uriList.indices) {
                        val format = SimpleDateFormat("yyyyMMddHHmmssSSS")
                        val date = Date(System.currentTimeMillis())
                        val filename = format.format(date)
                        val destFile = File(resultPath, "$filename.jpg")
                        if (!destFile.parentFile.exists()) {
                            destFile.parentFile.mkdirs()
                        }
                        if (!destFile.exists()) {
                            destFile.createNewFile()
                        }
                        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(
                            (uriList[i])!!, "r"
                        )
                        val inputStream = FileInputStream(
                            parcelFileDescriptor!!.fileDescriptor
                        )
                        val ostream = FileOutputStream(destFile)
                        val buffer = ByteArray(1024)
                        var byteCount = 0
                        while ((inputStream.read(buffer)
                                .also { byteCount = it }) != -1
                        ) {  // 循环从输入流读取 buffer字节
                            ostream.write(buffer, 0, byteCount) // 将读取的输入流写入到输出流
                        }
                        ostream.flush()
                        ostream.close()
                        inputStream.close()
                        newFilesList.add(destFile)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                newFilesList
            }).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<List<File?>?> {
                override fun onSubscribe(d: Disposable) {}
                override fun onError(e: Throwable) {}
                override fun onComplete() {}
                override fun onNext(files: List<File?>) {
                    onCopyPublicFile2PackageListener.let {
                        onCopyPublicFile2PackageListener?.onCopyPublicFile2Package(
                            files
                        )
                    }
                }
            })
    }

    /**
     * @param uri    需要拷贝的uri
     * @param toFile 输出的目标文件，
     * @date :2019/12/20 0020
     * @author : gaoxiaoxiong
     * @description:此方法将公共目录文件保存在沙盒目录下面,toFile最好是缓存目录下面的文件
     */
    fun copyPublicDir2PackageDir(
        context: Context,
        uri: Uri?,
        toFile: File,
        onCopyPublicFile2PackageListener: OnCopyPublicFile2PackageListener?
    ) {
        Observable.just(toFile).observeOn(Schedulers.newThread())
            .map { file ->
                try {
                    if (toFile.exists()) {
                        toFile.delete()
                    }
                    if (!toFile.parentFile.exists()) {
                        toFile.parentFile.mkdirs()
                    }
                    if (!toFile.exists()) {
                        toFile.createNewFile()
                    }
                    val parcelFileDescriptor = context.contentResolver.openFileDescriptor(
                        (uri)!!, "r"
                    )
                    val inputStream = FileInputStream(
                        parcelFileDescriptor!!.fileDescriptor
                    )
                    val ostream = FileOutputStream(file)
                    val buffer = ByteArray(1024)
                    var byteCount = 0
                    while ((inputStream.read(buffer)
                            .also { byteCount = it }) != -1
                    ) {  // 循环从输入流读取 buffer字节
                        ostream.write(buffer, 0, byteCount) // 将读取的输入流写入到输出流
                    }
                    ostream.flush()
                    ostream.close()
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                file
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<File?> {
                override fun onSubscribe(d: Disposable) {}

                override fun onError(e: Throwable) {}
                override fun onComplete() {}
                override fun onNext(files: File) {
                    onCopyPublicFile2PackageListener.let {
                        onCopyPublicFile2PackageListener?.onCopyPublicFile2Package(
                            files
                        )
                    }
                }
            })
    }

    /**
     * @date 创建时间:2020/4/1 0001
     * @auther gaoxiaoxiong
     * @Descriptiion 通过Uri 获取 filePath  fileName
     */
    fun getPathFromContentUri(contentUri: Uri?, context: Context): Array<String>? {
        if (contentUri == null) {
            return null
        }
        val filePath: String
        val fileName: String
        val filePathColumn =
            arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME)
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            contentUri, filePathColumn, null,
            null, null
        )
        cursor?.moveToFirst()
        filePath = cursor?.getString(cursor.getColumnIndex(filePathColumn[0])) ?: ""
        fileName = cursor?.getString(cursor.getColumnIndex(filePathColumn[1])) ?: ""
        cursor?.close()
        return arrayOf(filePath, fileName)
    }

    //-----------------------------

    private val outputDirectory: String by lazy {
        //沙盒外部私有存储的【图片】
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Environment.DIRECTORY_PICTURES
        } else {
            MyApplication.instance.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path
                ?: ""
        }
    }

    /**
     * 获取私有外部存储的文件
     */
    fun getLocalFilePath(): MutableList<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getMediaQPlus()
        } else {
            getMediaQMinus()
        }

    /**
     * 获取文件目录下，所有文件和目录的路径（AndroidQ（10.0） 以上版本）
     * 10.0以上返回的是映射出来的相对路径
     *
     * 已确认： 11.0好使
     */
    private fun getMediaQPlus(): MutableList<String> {
        val items = mutableListOf<String>()
        val contentResolver = MyApplication.instance.applicationContext.contentResolver

        //使用MediaStore查询外部存储里的Video文件, EXTERNAL_CONTENT_URI代表外部存储器，该值不变
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.RELATIVE_PATH,   //RELATIVE_PATH是相对路径不是绝对路径
                MediaStore.Video.Media.DATE_TAKEN,
            ),
            null,
            null,
            "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val path = cursor.getString(pathColumn)
                val date = cursor.getLong(dateColumn)
                val contentUri: Uri =
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                if (path == outputDirectory) {
                    items.add(contentUri.toString())
                }
            }
        }

        //使用MediaStore查询外部存储里的Images文件, EXTERNAL_CONTENT_URI代表外部存储器，该值不变
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.DATE_TAKEN,
            ),
            null,
            null,
            "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val path = cursor.getString(pathColumn)
                val date = cursor.getLong(dateColumn)
                //这个方法负责把id和contentUri连接成一个新的Uri，用于为路径加上ID部分
                val contentUri: Uri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                //android pix4a(11.0)的手机读出来的path 是Pictures/,带"/"
                if (TextUtils.equals(path, outputDirectory) || TextUtils.equals(
                        path,
                        "$outputDirectory/"
                    )
                ) {
                    //造入数据源供列表显示照片
                    items.add(contentUri.toString())
                }
            }
        }
        return items
    }

    /**
     * 获取文件目录下，所有文件和目录的路径（AndroidQ（10.0） 以下版本）
     * 10.0以下返回的是绝对路径
     *
     * 已确认： 8.0好使
     */
    private fun getMediaQMinus(): MutableList<String> {
        val items = mutableListOf<String>()
        //listFiles()是获取该目录下所有文件和目录的绝对路径
        File(outputDirectory).listFiles()?.forEach {
            //传绝对路径即可,造入数据源供列表显示照片
            items.add(it.absolutePath)
        }
        return items
    }
}