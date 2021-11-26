package com.change.filedemo.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder


/**
 * @date: 2019/5/23 0023
 * @author: gaoxiaoxiong
 * @description:图片，视频下载工具
 */
object HttpDownFileUtils {
    private val TAG = HttpDownFileUtils::class.java.simpleName
    private var downFileUtils: HttpDownFileUtils? = null
    const val LOADING = 0 //加载中
    const val SUCCESS = 1
    const val FAIL = -1

    /**
     * @date :2020/1/16 0016
     * @author : gaoxiaoxiong
     * @description:下载到沙盒内部，路径自己指定目录
     * @param downPathUrl 下载的文件路径，需要包含后缀
     * @param localPath 下载到本地具体目录
     * @param isNeedDeleteOldFile 是否要删除老文件
     */
    fun downFileFromServiceToLocalDir(
        downPathUrl: String,
        localPath: String?,
        isNeedDeleteOldFile: Boolean,
        onFileDownListener: OnFileDownListener?
    ) {
        Observable.just(downPathUrl).subscribeOn(Schedulers.newThread()).map(
            Function { downPath ->
                var file: File? = null
                try {
                    val url = URL(downPath)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 30 * 1000
                    val `is` = conn.inputStream
                    val time = System.currentTimeMillis()
                    val code = conn.responseCode
                    val prefix = downPath.substring(downPath.lastIndexOf(".") + 1)
                    var fileName: String? = null
                    if (code == HttpURLConnection.HTTP_OK) {
                        fileName = conn.getHeaderField("Content-Disposition")
                        // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                        if (fileName == null || fileName.isEmpty()) {
                            // 通过截取URL来获取文件名
                            val downloadUrl = conn.url // 获得实际下载文件的URL
                            fileName = downloadUrl.file
                            fileName = fileName.substring(fileName.lastIndexOf("/") + 1)
                        } else {
                            fileName = URLDecoder.decode(
                                fileName.substring(
                                    fileName.indexOf("filename=") + 9
                                ), "UTF-8"
                            )
                            // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                            fileName = fileName.replace("\"".toRegex(), "")
                        }
                    }
                    if (isEmpty(fileName)) {
                        fileName = "$time.$prefix"
                    }
                    file = File(localPath, fileName)
                    if (isNeedDeleteOldFile && file.exists()) {
                        file.delete()
                    }
                    if (!file.parentFile.exists()) {
                        file.parentFile.mkdirs()
                    }
                    if (!file.exists()) {
                        file.createNewFile()
                    }
                    val fos = FileOutputStream(file)
                    val bis = BufferedInputStream(`is`)
                    val buffer = ByteArray(1024)
                    var len: Int
                    var total: Long = 0
                    val contentLength = conn.contentLength.toLong()
                    while ((bis.read(buffer).also { len = it }) != -1) {
                        fos.write(buffer, 0, len)
                        total += len.toLong()
                        onFileDownListener.let {
                            onFileDownListener?.onFileDownStatus(
                                0,
                                null,
                                (total * 100 / contentLength).toInt(),
                                total,
                                contentLength
                            )
                        }
                    }
                    fos.close()
                    bis.close()
                    `is`.close()
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                file
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<File?> {
                override fun onSubscribe(d: Disposable) {}
                override fun onError(e: Throwable) {}
                override fun onComplete() {}
                override fun onNext(file: File) {
                    if (file != null && onFileDownListener != null) {
                        onFileDownListener.onFileDownStatus(1, file, 0, 0, 0)
                    } else {
                        onFileDownListener?.onFileDownStatus(-1, null, 0, 0, 0)
                    }
                }
            })
    }

    /**
     * @date: 2019/5/23 0023
     * @author: gaoxiaoxiong
     * @description: 沙盒内部目录，是我们自己的APP目录下面
     * @param downPathUrl 下载的文件路径，需要包含后缀
     * @param isNeedDeleteOldFile 是否删除老项目
     * @param inserType  DIRECTORY_PICTURES  DIRECTORY_MOVIES  DIRECTORY_MUSIC
     * @param isDownCacleDir 是否下载到缓存目录 true 下载到缓存目录  false 不是下载到缓存目录
     *
     *
     * 已确认： 8.0，11.0好使
     */
    fun downFileFromServiceToLocalDir(
        downPathUrl: String,
        inserType: String,
        isNeedDeleteOldFile: Boolean,
        isDownCacleDir: Boolean,
        onFileDownListener: OnFileDownListener?
    ) {
        Observable.just(downPathUrl).subscribeOn(Schedulers.newThread())
            .map { downPath ->
                var file: File? = null
                try {
                    val url = URL(downPath)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 30 * 1000
                    val `is` = conn.inputStream
                    val time = System.currentTimeMillis()
                    val code = conn.responseCode
                    val prefix = downPath.substring(downPath.lastIndexOf(".") + 1)
                    var fileName: String? = null
                    if (code == HttpURLConnection.HTTP_OK) {
                        fileName = conn.getHeaderField("Content-Disposition")
                        // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                        if (fileName == null || fileName.isEmpty()) {
                            // 通过截取URL来获取文件名
                            val downloadUrl = conn.url // 获得实际下载文件的URL
                            fileName = downloadUrl.file
                            fileName = fileName.substring(fileName.lastIndexOf("/") + 1)
                        } else {
                            fileName = URLDecoder.decode(
                                fileName.substring(
                                    fileName.indexOf("filename=") + 9
                                ), "UTF-8"
                            )
                            // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                            fileName = fileName.replace("\"".toRegex(), "")
                        }
                    }
                    if (isEmpty(fileName)) {
                        fileName = "$time.$prefix"
                    }
                    if (!isDownCacleDir) {
                        when (inserType) {
                            Environment.DIRECTORY_PICTURES -> { //图片
                                file = File(
                                    FileSDCardUtil.publicDiskImagePicDir,
                                    fileName ?: ""
                                )
                            }
                            Environment.DIRECTORY_MOVIES -> { //视频
                                file = File(
                                    FileSDCardUtil.publicDiskMoviesDir,
                                    fileName ?: ""
                                )
                            }
                            Environment.DIRECTORY_MUSIC -> { //音乐
                                file = File(
                                    FileSDCardUtil.publicDiskMusicDir,
                                    fileName ?: ""
                                )
                            }
                        }
                    } else {
                        when (inserType) {
                            Environment.DIRECTORY_PICTURES -> { //图片
                                file = File(
                                    FileSDCardUtil.publicDiskImagePicCacheDir,
                                    fileName ?: ""
                                )
                            }
                            Environment.DIRECTORY_MOVIES -> { //视频
                                file = File(
                                    FileSDCardUtil.publicDiskMoviesCacheDir,
                                    fileName ?: ""
                                )
                            }
                            Environment.DIRECTORY_MUSIC -> { //音乐
                                file = File(
                                    FileSDCardUtil.publicDiskMusicCacheDir,
                                    fileName ?: ""
                                )
                            }
                        }
                    }
                    if (isNeedDeleteOldFile && file!!.exists()) {
                        file.delete()
                    }
                    if (file?.parentFile?.exists() == false) {
                        file.parentFile.mkdirs()
                    }
                    if (file?.exists() == false) {
                        file.createNewFile()
                    }
                    val fos = FileOutputStream(file)
                    val bis = BufferedInputStream(`is`)
                    val buffer = ByteArray(1024)
                    var len: Int
                    var total: Long = 0
                    val contentLength = conn.contentLength.toLong()
                    while ((bis.read(buffer).also { len = it }) != -1) {
                        fos.write(buffer, 0, len)
                        total += len.toLong()
                        onFileDownListener.let {
                            onFileDownListener?.onFileDownStatus(
                                0,
                                null,
                                (total * 100 / contentLength).toInt(),
                                total,
                                contentLength
                            )
                        }

                    }
                    fos.close()
                    bis.close()
                    `is`.close()
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
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
                override fun onNext(file: File) {
                    if (file != null && onFileDownListener != null) {
                        onFileDownListener.onFileDownStatus(1, file, 0, 0, 0)
                    } else {
                        onFileDownListener?.onFileDownStatus(-1, null, 0, 0, 0)
                    }
                }
            })
    }

    /**
     * @date: 2021/11/25
     * @author: xingjunchao
     * @description: 沙盒内部目录，是我们自己的APP目录下面
     * @param downPathUrl online图片url
     *
     *  图片命名成带".png"之类的后缀才能在私有存储路径下直接打开查看图片，成为图片文件
     *
     *  已确认： 8.0, 11.0好使
     */
    fun downImageFromServiceToLocalDir(
        downPathUrl: String,
        context: Context,
        onFileDownListener: OnFileDownListener?
    ) {
        Observable.just(downPathUrl).subscribeOn(Schedulers.newThread())
            .map { downPath ->
                var file: File? = null
                try {
                    val url = URL(downPath)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 30 * 1000
                    val `is` = conn.inputStream
                    val time = System.currentTimeMillis()
                    val code = conn.responseCode
                    val prefix = downPath.substring(downPath.lastIndexOf(".") + 1)
                    var fileName: String? = null
                    if (code == HttpURLConnection.HTTP_OK) {
                        fileName = conn.getHeaderField("Content-Disposition")
                        // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                        if (fileName == null || fileName.isEmpty()) {
                            // 通过截取URL来获取文件名
                            val downloadUrl = conn.url // 获得实际下载文件的URL
                            fileName = downloadUrl.file
                            fileName = fileName.substring(fileName.lastIndexOf("/") + 1)
                        } else {
                            fileName = URLDecoder.decode(
                                fileName.substring(
                                    fileName.indexOf("filename=") + 9
                                ), "UTF-8"
                            )
                            // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                            fileName = fileName.replace("\"".toRegex(), "")
                        }
                    }
                    if (isEmpty(fileName)) {
                        fileName = "$time.$prefix"
                    }
                    /**
                     * 图片命名成带".png"之类的后缀才能在私有存储路径下直接打开查看图片
                     */
                    fileName = "$fileName.png"
                    file = File(
                        FileSDCardUtil.publicDiskImagePicDir,
                        fileName ?: ""
                    )
                    if (file?.parentFile?.exists() == false) {
                        file.parentFile.mkdirs()
                    }
                    if (!file.exists()) {
                        file.createNewFile()
                    }
                    val fos = FileOutputStream(file)
                    val bis = BufferedInputStream(`is`)
                    val buffer = ByteArray(1024)
                    var len: Int
                    var total: Long = 0
                    val contentLength = conn.contentLength.toLong()
                    while ((bis.read(buffer).also { len = it }) != -1) {
                        fos.write(buffer, 0, len)
                        total += len.toLong()
                        onFileDownListener.let {
                            onFileDownListener?.onFileDownStatus(
                                0,
                                null,
                                (total * 100 / contentLength).toInt(),
                                total,
                                contentLength
                            )
                        }

                    }
                    fos.close()
                    bis.close()
                    `is`.close()
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
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
                override fun onNext(uri: File) {
                    if (uri != null && onFileDownListener != null) {
                        onFileDownListener.onFileDownStatus(1, uri, 0, 0, 0)
                    } else {
                        onFileDownListener?.onFileDownStatus(-1, null, 0, 0, 0)
                    }
                }
            })
    }

    /**
     * ok
     * 如果是要存放到沙盒外部目录，就需要使用此方法
     * @date: 创建时间:2019/12/11
     * @author: gaoxiaoxiong
     * @descripion: 保存图片，视频，音乐到公共地区，此操作需要在线程，不是我们自己的APP目录下面的
     * @param downPathUrl 下载文件的路径，需要包含后缀  (ps：灵活理解运用，没后缀也行)
     * @param inserType 存储类型，可选参数 DIRECTORY_PICTURES  ,DIRECTORY_MOVIES  ,DIRECTORY_MUSIC ，DIRECTORY_DOWNLOADS
     */
    fun downFileFromServiceToPublicDir(
        downPathUrl: String,
        context: Context,
        inserType: String,
        onFileDownListener: OnFileDownListener?
    ) {
        if (inserType == Environment.DIRECTORY_DOWNLOADS) {
            /**
             *  兼容Android Q和以下版本
             */
            if (Build.VERSION.SDK_INT >= 29) {
                //android 10, 返回的是uri
                downUnKnowFileFromService(
                    downPathUrl,
                    context,
                    inserType,
                    onFileDownListener
                )
            } else {
                //android 10 以下 , 返回的是file
                downUnKnowFileFromService(downPathUrl, onFileDownListener)
            }
        } else {
            //下载到沙盒外部公共目录
            downMusicVideoPicFromService(downPathUrl, context, inserType, onFileDownListener)
        }
    }

    /**
     * @date :2020/3/17 0017
     * @author : gaoxiaoxiong
     * @description:下载文件到DIRECTORY_DOWNLOADS，适用于android<=9
     *
     * 已确认: 8.0好用
     */
    private fun downUnKnowFileFromService(
        downPathUrl: String,
        onFileDownListener: OnFileDownListener?
    ) {
        Observable.just(downPathUrl).subscribeOn(Schedulers.newThread())
            .map {
                var file: File? = null
                val url = URL(downPathUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 30 * 1000
                val `is` = conn.inputStream
                val time = System.currentTimeMillis()
                val code = conn.responseCode
                val prefix = downPathUrl.substring(downPathUrl.lastIndexOf(".") + 1)
                var fileName: String? = null
                if (code == HttpURLConnection.HTTP_OK) {
                    fileName = conn.getHeaderField("Content-Disposition")
                    // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                    if (fileName == null || fileName.isEmpty()) {
                        // 通过截取URL来获取文件名
                        val downloadUrl = conn.url // 获得实际下载文件的URL
                        fileName = downloadUrl.file
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1)
                    } else {
                        fileName = URLDecoder.decode(
                            fileName.substring(
                                fileName.indexOf("filename=") + 9
                            ), "UTF-8"
                        )
                        // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                        fileName = fileName.replace("\"".toRegex(), "")
                    }
                }
                if (isEmpty(fileName)) {
                    fileName = "$time.$prefix"
                }
                file = File(
                    FileSDCardUtil.getPublicDiskFileDirAndroid9(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )
                if (file.parentFile?.exists() == false) {
                    file.parentFile.mkdirs()
                }
                if (file.exists()) {
                    file.createNewFile()
                }
                val fos = FileOutputStream(file)
                val bis = BufferedInputStream(`is`)
                val buffer = ByteArray(1024)
                var len: Int
                var total: Long = 0
                val contentLength = conn.contentLength.toLong()
                while ((bis.read(buffer).also { len = it }) != -1) {
                    fos.write(buffer, 0, len)
                    total += len.toLong()
                    onFileDownListener.let {
                        onFileDownListener?.onFileDownStatus(
                            LOADING,
                            null,
                            (total * 100 / contentLength).toInt(),
                            total,
                            contentLength
                        )
                    }
                }
                file
            }.observeOn(AndroidSchedulers.mainThread()).subscribe(object : Observer<File?> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(file: File) {
                    if (file != null && onFileDownListener != null) {
                        onFileDownListener.onFileDownStatus(SUCCESS, file, 0, 0, 0)
                    } else {
                        onFileDownListener?.onFileDownStatus(FAIL, null, 0, 0, 0)
                    }
                }

                override fun onError(e: Throwable) {}
                override fun onComplete() {}
            })
    }

    /**
     * 如果是要存放到沙盒外部目录，就需要使用此方法
     * @date: 创建时间:2019/12/11
     * @author: gaoxiaoxiong
     * @descripion: 下载的文件到 DIRECTORY_DOWNLOADS，只有10以上才有 MediaStore.Downloads
     * @param downPathUrl 下载文件的路径，需要包含后缀
     * @param inserType 存储类型 DIRECTORY_DOWNLOADS
     *
     * 已确认： 11.0好用
     */
    private fun downUnKnowFileFromService(
        downPathUrl: String,
        context: Context,
        inserType: String,
        onFileDownListener: OnFileDownListener?
    ) {
        if ((inserType == Environment.DIRECTORY_DOWNLOADS)) {
            Observable.just(downPathUrl).subscribeOn(Schedulers.newThread())
                .map {
                    var uri: Uri? = null
                    try {
                        val url = URL(downPathUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 30 * 1000
                        val `is` = conn.inputStream
                        val time = System.currentTimeMillis()
                        val code = conn.responseCode
                        val prefix = downPathUrl.substring(downPathUrl.lastIndexOf(".") + 1)
                        var fileName: String? = null
                        if (code == HttpURLConnection.HTTP_OK) {
                            fileName = conn.getHeaderField("Content-Disposition")
                            // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                            if (fileName == null || fileName.isEmpty()) {
                                // 通过截取URL来获取文件名
                                val downloadUrl = conn.url // 获得实际下载文件的URL
                                fileName = downloadUrl.file
                                fileName = fileName.substring(fileName.lastIndexOf("/") + 1)
                            } else {
                                fileName = URLDecoder.decode(
                                    fileName.substring(
                                        fileName.indexOf("filename=") + 9
                                    ), "UTF-8"
                                )
                                // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                                fileName = fileName.replace("\"".toRegex(), "")
                            }
                        }
                        if (isEmpty(fileName)) {
                            fileName = "$time.$prefix"
                        }
                        val contentValues = ContentValues()
                        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        contentValues.put(MediaStore.Downloads.MIME_TYPE, getMIMEType(fileName))
                        contentValues.put(
                            MediaStore.Downloads.DATE_TAKEN,
                            System.currentTimeMillis()
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            //在外面其实已经判断版本了，这个方法整体只有在大于Q的时候才会走，加判断是为了取消代码爆红
                            uri = context.contentResolver.insert(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                contentValues
                            )
                        }
                        val inputStream = BufferedInputStream(`is`)
                        val os = context.contentResolver.openOutputStream(
                            (uri)!!
                        )
                        if (os != null) {
                            val buffer = ByteArray(1024)
                            var len: Int
                            var total: Long = 0
                            val contentLength = conn.contentLength.toLong()
                            while ((inputStream.read(buffer).also { len = it }) != -1) {
                                os.write(buffer, 0, len)
                                total += len.toLong()
                                onFileDownListener.let {
                                    onFileDownListener?.onFileDownStatus(
                                        LOADING,
                                        null,
                                        (total * 100 / contentLength).toInt(),
                                        total,
                                        contentLength
                                    )
                                }
                            }
                        }
                        os!!.flush()
                        inputStream.close()
                        `is`.close()
                        os.close()
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    uri
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Uri?> {
                    override fun onSubscribe(d: Disposable) {}
                    override fun onNext(uri: Uri) {
                        if (uri != null && onFileDownListener != null) {
                            onFileDownListener.onFileDownStatus(SUCCESS, uri, 0, 0, 0)
                        } else {
                            onFileDownListener?.onFileDownStatus(FAIL, null, 0, 0, 0)
                        }
                    }

                    override fun onError(e: Throwable) {}
                    override fun onComplete() {}
                })
        }
    }

    /**
     * 如果是要存放到沙盒外部目录，就需要使用此方法
     * @date: 创建时间:2019/12/11
     * @author: gaoxiaoxiong
     * @descripion: 保存图片，视频，音乐到公共地区，此操作需要在线程，不是我们自己的APP目录下面的
     * @param downPathUrl 下载文件的路径，需要包含后缀
     * @param inserType 存储类型，可选参数 DIRECTORY_PICTURES  ,DIRECTORY_MOVIES  ,DIRECTORY_MUSIC
     *
     * 已确认： 8.0, 11.0好用
     *
     * 10.0上下版本系统都是用Uri的方式写入媒体库
     */
    private fun downMusicVideoPicFromService(
        downPathUrl: String,
        context: Context,
        inserType: String,
        onFileDownListener: OnFileDownListener?
    ) {
        Observable.just(downPathUrl).subscribeOn(Schedulers.newThread())
            .map {
                var uri: Uri? = null
                try {
                    val url = URL(downPathUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 30 * 1000
                    val `is` = conn.inputStream
                    val time = System.currentTimeMillis()
                    val code = conn.responseCode
                    val prefix = downPathUrl.substring(downPathUrl.lastIndexOf(".") + 1)
                    var fileName: String? = null
                    if (code == HttpURLConnection.HTTP_OK) {
                        fileName = conn.getHeaderField("Content-Disposition")
                        // 通过Content-Disposition获取文件名，这点跟服务器有关，需要灵活变通
                        if (fileName == null || fileName.isEmpty()) {
                            // 通过截取URL来获取文件名
                            val downloadUrl = conn.url // 获得实际下载文件的URL
                            fileName = downloadUrl.file
                            fileName = fileName.substring(fileName.lastIndexOf("/") + 1)
                        } else {
                            fileName = URLDecoder.decode(
                                fileName.substring(
                                    fileName.indexOf("filename=") + 9
                                ), "UTF-8"
                            )
                            // 有些文件名会被包含在""里面，所以要去掉，不然无法读取文件后缀
                            fileName = fileName.replace("\"".toRegex(), "")
                        }
                    }
                    if (isEmpty(fileName)) {
                        fileName = "$time.$prefix"
                    }
                    //设置保存参数到ContentValues中
                    val contentValues = ContentValues()
                    when (inserType) {
                        Environment.DIRECTORY_PICTURES -> {
                            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                            contentValues.put(
                                MediaStore.Images.Media.MIME_TYPE,
                                getMIMEType(fileName)
                            )
                            contentValues.put(
                                MediaStore.Images.Media.DATE_TAKEN,
                                System.currentTimeMillis()
                            )
                            //只是往 MediaStore 里面插入一条新的记录，MediaStore 会返回给我们一个空的 Content Uri
                            //接下来问题就转化为往这个 Content Uri 里面写入
                            uri = context.contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )
                        }
                        Environment.DIRECTORY_MOVIES -> {
                            contentValues.put(
                                MediaStore.Video.Media.MIME_TYPE,
                                getMIMEType(fileName)
                            )
                            contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                            contentValues.put(
                                MediaStore.Video.Media.DATE_TAKEN,
                                System.currentTimeMillis()
                            )

                            /**
                             *    执行insert操作
                             *
                             *    我们通用方式是，先把视频通过【直接路径】的方式保存到SD卡，然后通过MediaColumns.DATA把文件路径注册到MediaStore中。这样相册就能看到这个视频了
                             *
                             *    使用先在MediaStore中注册获取一个Uri，会默认生成一个路径的，把Uri用输出流的方式打开，然后写入需要共享的视频。我们把这种方式叫Uri共享视频的方式。
                             *    通过这种方式才能共享视频，因为在Android10的分区存储模型中，是禁止通过直接路径访问SD卡的，否则报FileNotFoundException错，即便是共享媒体目录也不行
                             *    也就是说，在Android10及以上的系统只能通过Uri的方式共享媒体库中的文件
                             */

                            //只是往 MediaStore 里面插入一条新的记录，MediaStore 会返回给我们一个空的 Content Uri
                            //接下来问题就转化为往这个 Content Uri 里面写入
                            uri = context.contentResolver.insert(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )
                        }
                        Environment.DIRECTORY_MUSIC -> {
                            contentValues.put(
                                MediaStore.Audio.Media.MIME_TYPE,
                                getMIMEType(fileName)
                            )
                            contentValues.put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                            if (Build.VERSION.SDK_INT >= 29) {
                                //android 10
                                contentValues.put(
                                    MediaStore.Audio.Media.DATE_TAKEN,
                                    System.currentTimeMillis()
                                )
                            }
                            //只是往 MediaStore 里面插入一条新的记录，MediaStore 会返回给我们一个空的 Content Uri
                            //接下来问题就转化为往这个 Content Uri 里面写入
                            uri = context.contentResolver.insert(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )
                        }
                    }
                    /**
                     *   保存共享媒体，必须使用先在MediaStore创建表示视频保存信息的Uri，然后通过Uri写入视频数据的方式。
                     */
                    //若生成了null的uri，则表示该文件添加成功，接着使用流将内容写入该uri中即可
                    val inputStream = BufferedInputStream(`is`)
                    val os = context.contentResolver.openOutputStream(
                        (uri)!!
                    )
                    if (os != null) {
                        val buffer = ByteArray(1024)
                        var len: Int
                        var total: Long = 0
                        val contentLeng = conn.contentLength.toLong()
                        while ((inputStream.read(buffer).also { len = it }) != -1) {
                            os.write(buffer, 0, len)
                            total += len.toLong()
                            onFileDownListener.let {
                                onFileDownListener?.onFileDownStatus(
                                    LOADING,
                                    null,
                                    (total * 100 / contentLeng).toInt(),
                                    total,
                                    contentLeng
                                )
                            }
                        }
                    }

                    //oppo手机不会出现在照片里面，但是会出现在图集里面
                    if ((inserType == Environment.DIRECTORY_PICTURES)) { //如果是图片
                        //扫描到相册
                        val filePathArray: Array<String>? =
                            FileSDCardUtil.getPathFromContentUri(uri, context)
                        MediaScannerConnection.scanFile(
                            context, arrayOf(
                                filePathArray?.get(0)
                            ), arrayOf("image/jpeg")
                        ) { path, uri ->
                            Log.e(
                                TAG,
                                "PATH:$path"
                            )
                        }   //completeListenerCallback执行的逻辑
                    }
                    os!!.flush()
                    inputStream.close()
                    `is`.close()
                    os.close()
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                uri
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Uri?> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(uri: Uri) {
                    if (uri != null && onFileDownListener != null) {
                        onFileDownListener.onFileDownStatus(SUCCESS, uri, 0, 0, 0)
                    } else {
                        onFileDownListener?.onFileDownStatus(FAIL, null, 0, 0, 0)
                    }
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "错误信息:" + e.message)
                }

                override fun onComplete() {}
            })
    }

    /**
     * @date :2020/3/17 0017
     * @author : gaoxiaoxiong
     * @description:根据文件后缀名获得对应的MIME类型
     * @param fileName 文件名，需要包含后缀.xml类似这样的
     */
    private fun getMIMEType(fileName: String?): String {
        //临时写死了图片的type，
        return "image/JPEG"
    }

    /**
     * todo： 暂时不用此方法
     * @date :2020/3/17 0017
     * @author : gaoxiaoxiong
     * @description:获取文件的mimetype类型
     */
    val fileMiMeType: Array<Array<String>>
        get() = arrayOf(
            arrayOf(".3gp", "video/3gpp"),
            arrayOf(".apk", "application/vnd.android.package-archive"),
            arrayOf(".asf", "video/x-ms-asf"),
            arrayOf(".avi", "video/x-msvideo"),
            arrayOf(".bin", "application/octet-stream"),
            arrayOf(".bmp", "image/bmp"),
            arrayOf(".c", "text/plain"),
            arrayOf(".class", "application/octet-stream"),
            arrayOf(".conf", "text/plain"),
            arrayOf(".cpp", "text/plain"),
            arrayOf(".doc", "application/msword"),
            arrayOf(
                ".docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ),
            arrayOf(".xls", "application/vnd.ms-excel"),
            arrayOf(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            arrayOf(".exe", "application/octet-stream"),
            arrayOf(".gif", "image/gif"),
            arrayOf(".gtar", "application/x-gtar"),
            arrayOf(".gz", "application/x-gzip"),
            arrayOf(".h", "text/plain"),
            arrayOf(".htm", "text/html"),
            arrayOf(".html", "text/html"),
            arrayOf(".jar", "application/java-archive"),
            arrayOf(".java", "text/plain"),
            arrayOf(".jpeg", "image/jpeg"),
            arrayOf(".jpg", "image/jpeg"),
            arrayOf(".js", "application/x-javascript"),
            arrayOf(".log", "text/plain"),
            arrayOf(".m3u", "audio/x-mpegurl"),
            arrayOf(".m4a", "audio/mp4a-latm"),
            arrayOf(".m4b", "audio/mp4a-latm"),
            arrayOf(".m4p", "audio/mp4a-latm"),
            arrayOf(".m4u", "video/vnd.mpegurl"),
            arrayOf(".m4v", "video/x-m4v"),
            arrayOf(".mov", "video/quicktime"),
            arrayOf(".mp2", "audio/x-mpeg"),
            arrayOf(".mp3", "audio/x-mpeg"),
            arrayOf(".mp4", "video/mp4"),
            arrayOf(".mpc", "application/vnd.mpohun.certificate"),
            arrayOf(".mpe", "video/mpeg"),
            arrayOf(".mpeg", "video/mpeg"),
            arrayOf(".mpg", "video/mpeg"),
            arrayOf(".mpg4", "video/mp4"),
            arrayOf(".mpga", "audio/mpeg"),
            arrayOf(".msg", "application/vnd.ms-outlook"),
            arrayOf(".ogg", "audio/ogg"),
            arrayOf(".pdf", "application/pdf"),
            arrayOf(".png", "image/png"),
            arrayOf(".pps", "application/vnd.ms-powerpoint"),
            arrayOf(".ppt", "application/vnd.ms-powerpoint"),
            arrayOf(
                ".pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            ),
            arrayOf(".prop", "text/plain"),
            arrayOf(".rc", "text/plain"),
            arrayOf(".rmvb", "audio/x-pn-realaudio"),
            arrayOf(".rtf", "application/rtf"),
            arrayOf(".sh", "text/plain"),
            arrayOf(".tar", "application/x-tar"),
            arrayOf(".tgz", "application/x-compressed"),
            arrayOf(".txt", "text/plain"),
            arrayOf(".wav", "audio/x-wav"),
            arrayOf(".wma", "audio/x-ms-wma"),
            arrayOf(".wmv", "audio/x-ms-wmv"),
            arrayOf(".wps", "application/vnd.ms-works"),
            arrayOf(".xml", "text/plain"),
            arrayOf(".z", "application/x-compress"),
            arrayOf(".zip", "application/x-zip-compressed"),
            arrayOf("", "*/*")
        )

    /**
     * Return whether the string is null or 0-length.
     *
     * @param s The string.
     * @return `true`: yes<br></br> `false`: no
     * true表示为空  false表示不是空
     */
    private fun isEmpty(s: CharSequence?): Boolean {
        return s == null || s.isEmpty()
    }

}