package com.bilibili.boxing.model.entity.impl;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.bilibili.boxing.model.entity.BaseMedia;
import com.bilibili.boxing.utils.BoxingExecutor;
import com.bilibili.boxing.utils.BoxingExifHelper;
import com.bilibili.boxing.utils.CompressTask;
import com.bilibili.boxing.utils.ImageCompressor;
import com.bilibili.boxing.utils.MediaUtils.*;


import java.io.File;
import java.util.Locale;

/**
 * Created by mobao.libo on 2017-06-06-0006.
 */

public class MediaEntity extends BaseMedia implements Parcelable{
    private static final long MB = 1024 * 1024;
    private static final long MAX_GIF_SIZE = 1024 * 1024L;
    private static final long MAX_IMAGE_SIZE = 1024 * 1024L;

    public String mTitle;
    public String mDuration;
    public String mDateTaken;
    private String mModifyData;
    public boolean mIsSelected;
    public String mThumbnailPath;
    public String mCompressPath;
    public int mHeight;
    public int mWidth;
    public MEDIA_TYPE mImageType;
    public String mMimeType;

    protected MediaEntity(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MediaEntity> CREATOR = new Creator<MediaEntity>() {
        @Override
        public MediaEntity createFromParcel(Parcel in) {
            return new MediaEntity(in);
        }

        @Override
        public MediaEntity[] newArray(int size) {
            return new MediaEntity[size];
        }
    };

    @Override
    public TYPE getType() {
        return TYPE.MEDIA;
    }

    public boolean compress(ImageCompressor imageCompressor) {
        return CompressTask.compress(imageCompressor, this, MAX_IMAGE_SIZE);
    }

    public boolean compress(ImageCompressor imageCompressor, int maxSize) {
        return CompressTask.compress(imageCompressor, this, maxSize);
    }

    public String getSizeByUnit() {
        double size = getSize();
        if (size == 0) {
            return "0K";
        }
        if (size >= MB) {
            double sizeInM = size / MB;
            return String.format(Locale.getDefault(), "%.1f", sizeInM) + "M";
        }
        double sizeInK = size / 1024;
        return String.format(Locale.getDefault(), "%.1f", sizeInK) + "K";
    }

    public String formatTimeWithMin(long duration) {
        if (duration <= 0) {
            return String.format(Locale.US, "%02d:%02d", 0, 0);
        }
        long totalSeconds = duration / 1000;

        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d", hours * 60 + minutes,
                    seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    public MEDIA_TYPE getImageTypeByMime(String mimeType) {
        if (!TextUtils.isEmpty(mimeType)) {
            if ("image/gif".equals(mimeType)) {
                return MEDIA_TYPE.GIF;
            } else if ("image/png".equals(mimeType)) {
                return MEDIA_TYPE.PNG;
            } else if ("image/jpg".equals(mimeType)){
                return MEDIA_TYPE.JPG;
            } else {
                return MEDIA_TYPE.VIDEO;
            }
        }
        return MEDIA_TYPE.PNG;
    }

    public MEDIA_TYPE getMediaType() {
        if (!TextUtils.isEmpty(mMimeType)) {
            if ("image/gif".equals(mMimeType)) {
                return MEDIA_TYPE.GIF;
            } else if ("image/png".equals(mMimeType)) {
                return MEDIA_TYPE.PNG;
            } else if ("image/jpeg".equals(mMimeType)){
                return MEDIA_TYPE.JPG;
            } else if ("video/mp4".equals(mMimeType)){
                return MEDIA_TYPE.VIDEO;
            }
        }
        return MEDIA_TYPE.PNG;
    }

    public boolean isGif() {
        return mImageType == MEDIA_TYPE.GIF;
    }

    public boolean isGifOverSize() {
        return isGif() && getSize() > MAX_GIF_SIZE;
    }

    public MediaEntity(Builder builder) {
        super(builder.mId, builder.mImagePath);
        this.mThumbnailPath = builder.mThumbnailPath;
        this.mSize = builder.mSize;
        this.mHeight = builder.mHeight;
        this.mIsSelected = builder.mIsSelected;
        this.mWidth = builder.mWidth;
        this.mMimeType = builder.mMimeType;
        this.mImageType = getImageTypeByMime(builder.mMimeType);
        this.mTitle = builder.mTitle;
        this.mDateTaken = builder.mDateTaken;
        this.mPath = builder.mPath;
        this.mDuration = builder.mDuration;
        this.mModifyData = builder.mModifyData;
    }

    public MediaEntity(@NonNull File file) {
        this.mId = String.valueOf(System.currentTimeMillis());
        this.mPath = file.getAbsolutePath();
        this.mSize = String.valueOf(file.length());
        this.mIsSelected = true;
    }

    /**
     * save image to MediaStore.
     */
    public void saveMediaStore(final ContentResolver cr) {
        BoxingExecutor.getInstance().runWorker(new Runnable() {
            @Override
            public void run() {
                if (cr != null && !TextUtils.isEmpty(getId())) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, getId());
                    values.put(MediaStore.Images.Media.MIME_TYPE, getMimeType());
                    values.put(MediaStore.Images.Media.DATA, getPath());
                    cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                }
            }
        });
    }

    public static class Builder {
        private String mModifyData;
        private String mId;
        private String mTitle;
        private String mPath;
        private String mDuration;
        private String mSize;
        private String mDateTaken;
        private String mMimeType;
        private String mImagePath;
        private String mThumbnailPath;
        private boolean mIsSelected;
        private int mHeight;
        private int mWidth;

        public Builder(String id, String path) {
            this.mId = id;
            this.mPath = path;
        }

        public MediaEntity.Builder setTitle(String title) {
            this.mTitle = title;
            return this;
        }

        public MediaEntity.Builder setDuration(String duration) {
            this.mDuration = duration;
            return this;
        }

        public MediaEntity.Builder setSize(String size) {
            this.mSize = size;
            return this;
        }

        public MediaEntity.Builder setDataTaken(String dateTaken) {
            this.mDateTaken = dateTaken;
            return this;
        }

        public MediaEntity.Builder setMimeType(String type) {
            this.mMimeType = type;
            return this;
        }

        public MediaEntity.Builder setThumbnailPath(String thumbnailPath) {
            mThumbnailPath = thumbnailPath;
            return this;
        }

        public MediaEntity.Builder setSelected(boolean mIsSelected) {
            this.mIsSelected = mIsSelected;
            return this;
        }

        public MediaEntity.Builder setHeight(int mHeight) {
            this.mHeight = mHeight;
            return this;
        }

        public MediaEntity.Builder setWidth(int mWidth) {
            this.mWidth = mWidth;
            return this;
        }

        public MediaEntity.Builder setModifyDate(String date){
            this.mModifyData = date;
            return this;
        }

        public MediaEntity build() {
            return new MediaEntity(this);
        }
    }


    public MEDIA_TYPE getMediaKind(){
        if (!TextUtils.isEmpty(mMimeType)) {
            if ("image/gif".equals(mMimeType)
                    ||"image/png".equals(mMimeType)
                    ||"image/jpeg".equals(mMimeType)
                    ||"image/jpg".equals(mMimeType)) {
                return MEDIA_TYPE.PHOTO;
            }  else if ("video/mp4".equals(mMimeType)){
                return MEDIA_TYPE.VIDEO;
            }
        }
        return MEDIA_TYPE.PHOTO;
    }

    public void removeExif() {
        BoxingExifHelper.removeExif(getPath());
    }

    public String getModifyData() {
        return mModifyData;
    }


    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getDuration() {
        try {
            long duration = Long.parseLong(mDuration);
            return formatTimeWithMin(duration);
        } catch (NumberFormatException e) {
            return "0:00";
        }
    }

    public void setDuration(String mDuration) {
        this.mDuration = mDuration;
    }

    public String getDateTaken() {
        return mDateTaken;
    }

    public void setDateTaken(String mDateTaken) {
        this.mDateTaken = mDateTaken;
    }

    public void setModifyData(String mModifyData) {
        this.mModifyData = mModifyData;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean mIsSelected) {
        this.mIsSelected = mIsSelected;
    }

    public String getThumbnailPath() {
        return mThumbnailPath;
    }

    public void setThumbnailPath(String mThumbnailPath) {
        this.mThumbnailPath = mThumbnailPath;
    }

    public String getCompressPath() {
        return mCompressPath;
    }

    public void setCompressPath(String mCompressPath) {
        this.mCompressPath = mCompressPath;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int mHeight) {
        this.mHeight = mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int mWidth) {
        this.mWidth = mWidth;
    }

    public MEDIA_TYPE getImageType() {
        return mImageType;
    }

    public void setImageType(MEDIA_TYPE mImageType) {
        this.mImageType = mImageType;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mMimeType) {
        this.mMimeType = mMimeType;
    }
}
