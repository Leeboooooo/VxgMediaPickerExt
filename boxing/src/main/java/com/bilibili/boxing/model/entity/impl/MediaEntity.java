package com.bilibili.boxing.model.entity.impl;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.bilibili.boxing.model.entity.BaseMedia;

import java.util.Locale;

/**
 * Created by mobao.libo on 2017-06-06-0006.
 */

public class MediaEntity extends BaseMedia {
    private static final long MB = 1024 * 1024;

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

    public enum MEDIA_TYPE {
        PNG, JPG, GIF,VIDEO
    }

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

        public MediaEntity.Builder setIsSelected(boolean mIsSelected) {
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

    public String getmModifyData() {
        return mModifyData;
    }
}
