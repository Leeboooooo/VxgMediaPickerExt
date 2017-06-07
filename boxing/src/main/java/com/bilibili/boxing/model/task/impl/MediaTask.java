package com.bilibili.boxing.model.task.impl;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.bilibili.boxing.model.BoxingManager;
import com.bilibili.boxing.model.callback.IMediaTaskCallback;
import com.bilibili.boxing.model.config.BoxingConfig;
import com.bilibili.boxing.model.entity.impl.MediaEntity;
import com.bilibili.boxing.model.entity.impl.VideoMedia;
import com.bilibili.boxing.model.task.IMediaTask;
import com.bilibili.boxing.utils.BoxingExecutor;
import com.bilibili.boxing.utils.BoxingLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by mobao.libo on 2017-06-06-0006.
 */

public class MediaTask implements IMediaTask<MediaEntity> {
    private static final String SELECTION_IMAGE_MIME_TYPE = MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?";
    private static final String SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF = MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?";
    private static final String SELECTION_ID = MediaStore.Images.Media.BUCKET_ID + "=? and (" + SELECTION_IMAGE_MIME_TYPE + " )";
    private static final String SELECTION_ID_WITHOUT_GIF = MediaStore.Images.Media.BUCKET_ID + "=? and (" + SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF + " )";
    private static final String[] SELECTION_ARGS_IMAGE_MIME_TYPE = {"image/jpeg", "image/png", "image/jpg", "image/gif"};
    private static final String[] SELECTION_ARGS_IMAGE_MIME_TYPE_WITHOUT_GIF = {"image/jpeg", "image/png", "image/jpg"};
    private IMediaTaskCallback<MediaEntity> callback;
    private BoxingConfig mPickerConfig;
    private Map<String, String> mThumbnailMap;
    private List<MediaEntity> results;
    private int imageCount = 0;
    private int videoCount = 0;

    public MediaTask(){
        this.mThumbnailMap = new ArrayMap<>();
        this.mPickerConfig = BoxingManager.getInstance().getBoxingConfig();
        results = new ArrayList<>();
    }

    @Override
    public void load(ContentResolver cr, int page, String id, IMediaTaskCallback<MediaEntity> callback) {
        this.callback = callback;
        buildThumbnail(cr);
        buildAlbumList(cr, id, page, callback);
        loadVideos(cr, page);
    }

    private void buildThumbnail(ContentResolver cr) {
        String[] projection = {MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA};
        queryThumbnails(cr, projection);
    }

    private void queryThumbnails(ContentResolver cr, String[] projection) {
        Cursor cur = null;
        try {
            cur = MediaStore.Images.Thumbnails.queryMiniThumbnails(cr, MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Thumbnails.MINI_KIND, projection);
            if (cur != null && cur.moveToFirst()) {
                do {
                    String imageId = cur.getString(cur.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID));
                    String imagePath = cur.getString(cur.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
                    mThumbnailMap.put(imageId, imagePath);
                } while (cur.moveToNext() && !cur.isLast());
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    private List<MediaEntity> buildAlbumList(ContentResolver cr, String bucketId, int page,
                                            @NonNull final IMediaTaskCallback<MediaEntity> callback) {
        List<MediaEntity> result = new ArrayList<>();
        String columns[] = getImageColumns();
        Cursor cursor = null;
        try {
            boolean isDefaultAlbum = TextUtils.isEmpty(bucketId);
            boolean isNeedPaging = mPickerConfig == null || mPickerConfig.isNeedPaging();
            boolean isNeedGif = mPickerConfig != null && mPickerConfig.isNeedGif();
            int totalCount = getTotalCount(cr, bucketId, columns, isDefaultAlbum, isNeedGif);

            String imageMimeType = isNeedGif ? SELECTION_IMAGE_MIME_TYPE : SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF;
            String[] args = isNeedGif ? SELECTION_ARGS_IMAGE_MIME_TYPE : SELECTION_ARGS_IMAGE_MIME_TYPE_WITHOUT_GIF;
            String order = isNeedPaging ? MediaStore.Images.Media.DATE_MODIFIED + " desc" + " LIMIT "
                    + page * IMediaTask.PAGE_LIMIT + " , " + IMediaTask.PAGE_LIMIT : MediaStore.Images.Media.DATE_MODIFIED + " desc";
            String selectionId = isNeedGif ? SELECTION_ID : SELECTION_ID_WITHOUT_GIF;
            cursor = query(cr, bucketId, columns, isDefaultAlbum, isNeedGif, imageMimeType, args, order, selectionId);
            addItem(totalCount, result, cursor, callback);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    private void loadVideos(ContentResolver cr, int page) {
        final List<MediaEntity> videoMedias = new ArrayList<>();
        final Cursor cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                getMediaColumns(), null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " desc" + " LIMIT " + page * IMediaTask.PAGE_LIMIT + " , " + IMediaTask.PAGE_LIMIT);
        try {
            int count = 0;
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getCount();
                do {
                    int i = 0;
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                    String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE));
                    String type = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE));
                    String size = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
                    String date = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN));
                    String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
                    String modifyDate = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED));
                    if (!size.equals("0") && size.compareToIgnoreCase("5120000")<0){
                        MediaEntity video = new MediaEntity.Builder(id, data)
                                .setTitle(title)
                                .setDuration(duration)
                                .setSize(size)
                                .setDataTaken(date)
                                .setModifyDate(modifyDate)
                                .setMimeType(type).build();
                        videoMedias.add(video);
                    }
                } while (!cursor.isLast() && cursor.moveToNext());
//                postMedias(callback, videoMedias, count);
                results.addAll(videoMedias);
                videoCount = count;
                sort();
            } else {
//                postMedias(callback, videoMedias, 0);
                results.addAll(videoMedias);
                videoCount = 0;
                sort();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void addItem(final int allCount,
                         final List<MediaEntity> result,
                         Cursor cursor,
                         @NonNull final IMediaTaskCallback<MediaEntity> callback) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String picPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                if (callback.needFilter(picPath)) {
                    BoxingLog.d("path:" + picPath + " has been filter");
                } else {
                    String id = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                    String size = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.SIZE));
                    String mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));
                    String modifyDate = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED));
                    int width = 0;
                    int height = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        width = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.WIDTH));
                        height = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT));
                    }
                    if (size.equals("0")) continue;
                    MediaEntity imageItem = new MediaEntity
                            .Builder(id, picPath)
                            .setThumbnailPath(mThumbnailMap.get(id))
                            .setSize(size).setMimeType(mimeType)
                            .setHeight(height)
                            .setWidth(width)
                            .setModifyDate(modifyDate)
                            .build();
                    if (!result.contains(imageItem)) {
                        result.add(imageItem);
                    }
                }
            } while (!cursor.isLast() && cursor.moveToNext());
            results.addAll(result);
            imageCount = allCount;
        } else {
            results.addAll(result);
            imageCount = 0;
        }
        clear();
    }

    private void sort(){
        if (results == null || results.size() < 1) postMedias(results,0,callback);
        int count = imageCount + videoCount;
        Comparator<MediaEntity> comparator = new Comparator<MediaEntity>() {
            @Override
            public int compare(MediaEntity o1, MediaEntity o2) {
                String preModifyDate = o1.getmModifyData();
                if (preModifyDate == null) return 0;
                String nextModifyDate = o2.getmModifyData();
                if (nextModifyDate == null) return 0;
                return nextModifyDate.compareToIgnoreCase(preModifyDate);
            }
        };
        Collections.sort(results,comparator);
        postMedias(results,count,this.callback);
    }

    private void postMedias(final List<MediaEntity> result,
                            final int count,
                            @NonNull final IMediaTaskCallback<MediaEntity> callback) {
        BoxingExecutor.getInstance().runUI(new Runnable() {
            @Override
            public void run() {
                callback.postMedia(result, count);
            }
        });
    }

    private Cursor query(ContentResolver cr, String bucketId, String[] columns, boolean isDefaultAlbum,
                         boolean isNeedGif, String imageMimeType, String[] args, String order, String selectionId) {
        Cursor resultCursor;
        if (isDefaultAlbum) {
            resultCursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, imageMimeType,
                    args, order);
        } else {
            if (isNeedGif) {
                resultCursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, selectionId,
                        new String[]{bucketId, args[0], args[1], args[2], args[3]}, order);
            } else {
                resultCursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, selectionId,
                        new String[]{bucketId, args[0], args[1], args[2]}, order);
            }
        }
        return resultCursor;
    }

    @NonNull
    private String[] getImageColumns() {
        String[] columns;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            columns = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT};
        } else {
            columns = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.MIME_TYPE};
        }
        return columns;
    }

    @NonNull
    private String[] getMediaColumns(){
        return new String[]{
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.Video.Media.DURATION
        };
    }

    private int getTotalCount(ContentResolver cr, String bucketId, String[] columns, boolean isDefaultAlbum, boolean isNeedGif) {
        Cursor allCursor = null;
        int result = 0;
        try {
            if (isDefaultAlbum) {
                allCursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
                        SELECTION_IMAGE_MIME_TYPE, SELECTION_ARGS_IMAGE_MIME_TYPE,
                        MediaStore.Images.Media.DATE_MODIFIED + " desc");
            } else {
                if (isNeedGif) {
                    allCursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, SELECTION_ID,
                            new String[]{bucketId, "image/jpeg", "image/png", "image/jpg", "image/gif"}, MediaStore.Images.Media.DATE_MODIFIED + " desc");
                } else {
                    allCursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, SELECTION_ID_WITHOUT_GIF,
                            new String[]{bucketId, "image/jpeg", "image/png", "image/jpg"}, MediaStore.Images.Media.DATE_MODIFIED + " desc");
                }
            }
            if (allCursor != null) {
                result = allCursor.getCount();
            }
        } finally {
            if (allCursor != null) {
                allCursor.close();
            }
        }
        return result;
    }

    private void clear() {
        if (mThumbnailMap != null) {
            mThumbnailMap.clear();
        }
    }
}
