package com.change.filedemo.util

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import com.permissionx.guolindev.PermissionX

/**
 * @author : gaoxiaoxiong
 * @date :2019/10/24 0024
 * @description:权限工具类
 */
object PermissonsUtil {
    /**
     * @date :2019/12/30 0030
     * @author : gaoxiaoxiong
     * @description:获取需要的权限
     */
    //APP 需要的所有权限
    var permissionList = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    //接口回调
    interface OnPermissonsListener {
        fun onSuccess()
        fun onFail()
    }

    /**
     * 请求权限
     */
    fun requestPermisson(
        activity: AppCompatActivity?,
        list: Array<String>,
        onPermissonsListener: OnPermissonsListener?
    ) {
        PermissionX.init(activity)
            .permissions(permissionList[0], permissionList[1])
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    onPermissonsListener?.onSuccess()
                } else {
                    onPermissonsListener?.onFail()
                }
            }
    }
}