/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2015 bboyfeiyu@gmail.com ( Mr.Simple )
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.feedback2345sdk.core;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.feedback2345sdk.R;
import com.feedback2345sdk.cache.BitmapCache;
import com.feedback2345sdk.cache.MemoryCache;
import com.feedback2345sdk.config.DisplayConfig;
import com.feedback2345sdk.config.ImageLoaderConfig;
import com.feedback2345sdk.policy.SerialPolicy;
import com.feedback2345sdk.request.BitmapRequest;


/**
 * 图片加载类,支持url和本地图片的uri形式加载.根据图片路径格式来判断是网络图片还是本地图片,如果是网络图片则交给SimpleNet框架来加载，
 * 如果是本地图片那么则交给mExecutorService从sd卡中加载
 * .加载之后直接更新UI，无需用户干预.如果用户设置了缓存策略,那么会将加载到的图片缓存起来.用户也可以设置加载策略，例如顺序加载{@see
 * SerialPolicy}和逆向加载{@see ReversePolicy}.
 * 
 * @author mrsimple
 */
public final class SimpleImageLoader {
    /**
     * SimpleImageLoader实例
     */
    private static SimpleImageLoader sInstance;

    /**
     * 网络请求队列
     */
    private RequestQueue mImageQueue;
    /**
     * 缓存
     */
    private volatile BitmapCache mCache = new MemoryCache();

    /**
     * 图片加载配置对象
     */
    private ImageLoaderConfig mConfig;

    private Handler mUIHandler = new Handler(Looper.myLooper());

    /**
     * 
     */
    private SimpleImageLoader() {
    }

    /**
     * 获取ImageLoader单例
     * 
     * @return
     */
    public static SimpleImageLoader getInstance() {
        if (sInstance == null) {
            synchronized (SimpleImageLoader.class) {
                if (sInstance == null) {
                    sInstance = new SimpleImageLoader();
                }
            }
        }
        return sInstance;
    }

    /**
     * @param config
     */
    public void init(ImageLoaderConfig config) {
        mConfig = config;
        mCache = mConfig.bitmapCache;
        checkConfig();
        mImageQueue = new RequestQueue(mConfig.threadCount);
        mImageQueue.start();
    }

    private void checkConfig() {
        if (mConfig == null) {
            throw new RuntimeException(
                    "The config of SimpleImageLoader is Null, please call the init(ImageLoaderConfig config) method to initialize");
        }

        if (mConfig.loadPolicy == null) {
            mConfig.loadPolicy = new SerialPolicy();
        }
        if (mCache == null) {
            mCache = new MemoryCache();
        }

    }

    public Handler getmUIHandler(){
        return mUIHandler;
    }

    public void displayImage(ImageView imageView, String uri) {
        displayImage(imageView, uri, null, null);
    }

    public void displayImage(ImageView imageView, String uri, DisplayConfig config) {
        displayImage(imageView, uri, config, null);
    }

    public void displayImage(ImageView imageView, String uri, ImageListener listener) {
        displayImage(imageView, uri, null, listener);
    }

    public void displayImage(final ImageView imageView, final String uri,
            final DisplayConfig config, final ImageListener listener) {
        BitmapRequest request = new BitmapRequest(imageView, uri, config, listener);
        // 加载的配置对象,如果没有设置则使用ImageLoader的配置
        request.displayConfig = request.displayConfig != null ? request.displayConfig
                : mConfig.displayConfig;
        Bitmap resultBitmap = mCache.get(request);
        if (resultBitmap != null){
            request.justCacheInMem = true;
            updateImageView(request, resultBitmap);
        } else {
            if (request.getImageView().getTag().equals(request.imageUri)){
                imageView.setImageResource(request.displayConfig.loadingResId > 0 ?
                        request.displayConfig.loadingResId : R.drawable.select_image_default_error);
            }
            // 添加对队列中
            mImageQueue.addRequest(request);
        }
    }

    public ImageLoaderConfig getConfig() {
        return mConfig;
    }

    public void stop() {
        mImageQueue.stop();
    }

    /**
     * 更新ImageView
     *
     * @param request
     * @param result
     */
    private void updateImageView(BitmapRequest request, Bitmap result) {
        final ImageView imageView = request.getImageView();
        final String uri = request.imageUri;
        if (result != null && imageView.getTag().equals(uri)) {
            imageView.setImageBitmap(result);
        }

        // 加载失败
        if (result == null && hasFaildPlaceholder(request.displayConfig)) {
            imageView.setImageResource(request.displayConfig.failedResId);
        }

        // 回调接口
        if (request.imageListener != null) {
            request.imageListener.onComplete(imageView, result, uri);
        }
    }


    private boolean hasFaildPlaceholder(DisplayConfig displayConfig) {
        return displayConfig != null && displayConfig.failedResId > 0;
    }

    /**
     * 图片加载Listener
     * 
     * @author mrsimple
     */
    public static interface ImageListener {
        public void onComplete(ImageView imageView, Bitmap bitmap, String uri);
    }
}
