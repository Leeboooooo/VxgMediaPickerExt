package com.bilibili.boxing.utils;

import android.text.TextUtils;

/**
 * Created by mobao.libo on 2017-06-07-0007.
 */

public class MediaUtils {
    public enum MEDIA_TYPE {
        PNG,JPG,GIF,VIDEO,MEDIA,PHOTO
    }

    public static MEDIA_TYPE getMediaTypeFormMimeType(String mimeType){
        if (!TextUtils.isEmpty(mimeType)) {
            if ("image/gif".equals(mimeType)) {
                return MEDIA_TYPE.GIF;
            } else if ("image/png".equals(mimeType)) {
                return MEDIA_TYPE.PNG;
            } else if ("image/jpg".equals(mimeType)){
                return MEDIA_TYPE.JPG;
            }else if ("image/jpeg".equals(mimeType)){
                return MEDIA_TYPE.JPG;
            } else {
                return MEDIA_TYPE.VIDEO;
            }
        }
        return MEDIA_TYPE.PNG;
    }
}
