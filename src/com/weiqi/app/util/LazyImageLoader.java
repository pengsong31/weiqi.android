package com.weiqi.app.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.weiqi.app.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;




public class LazyImageLoader {

	private static LazyImageLoader instance;
	public static final String TAG = "LazyImageLoader";
    private byte[] lock = new byte[0];
	
	private boolean mAllowLoad = true;
	
	private boolean firstLoad = true;
	
	private int mStartLoadLimit = 0;
	
	private int mStopLoadLimit = 0;
	
	public void setLoadLimit(int startLoadLimit,int stopLoadLimit){
		if(startLoadLimit > stopLoadLimit){
			return;
		}
		mStartLoadLimit = startLoadLimit;
		mStopLoadLimit = stopLoadLimit;
	}
	
	public void restore(){
		mAllowLoad = true;
		firstLoad = true;
	}
		
	public void lock(){
		mAllowLoad = false;
		firstLoad = false;
	}
	
	public void unlock(){
		mAllowLoad = true;
		synchronized (lock) {
			lock.notifyAll();
		}
	}
	
	final Handler handler = new Handler();

	MemoryCache mMemoryCache = new MemoryCache();
	FileCache mFileCache;
	private Map<ImageView, Object> mImageViews = Collections
			.synchronizedMap(new WeakHashMap<ImageView, Object>());
	ExecutorService mExecutorService;

	public static LazyImageLoader getInstance(Context context) {
		if (instance == null) {
			instance = new LazyImageLoader(context);
		}
		return instance;
	}

	protected LazyImageLoader(Context context) {
		mFileCache = new FileCache(context);
		mExecutorService = Executors.newFixedThreadPool(5);
	}

	public void clearFileCache() {
		mFileCache.clear();
	}

	public void clearMemoryCache() {
		mMemoryCache.clear();
	}
	

	/**
	 * 从文件中绑定图片到imageview
	 * 
	 * @param file
	 * @param imageview
	 */
	public void displayImage(File file, ImageView imageview, int reqWidth,
			int reqHeight, int fallback) {
		
		if (file == null) {
			imageview.setImageResource(fallback);
			return;
		}

		reqWidth = reqWidth % 2 == 0 ? reqWidth : reqWidth + 1;
		reqHeight = reqHeight % 2 == 0 ? reqHeight : reqHeight + 1;

		mImageViews.put(imageview, file);
		Bitmap bitmap = mMemoryCache.get(file);
		if (bitmap != null) {
			imageview.setImageBitmap(bitmap);
		} else {
			queuePhoto(file, imageview, reqWidth, reqHeight, fallback);
			imageview.setImageResource(fallback);
		}
	}
	
	 

	public void displayImage(String url, ImageView imageview, int reqWidth,
			int reqHeight, int fallback) {
		try {
			displayImage(new URL(url), imageview, reqWidth, reqHeight, fallback);
		} catch (MalformedURLException e) {
			if (imageview != null) {
				imageview.setImageResource(fallback);
			}
			e.printStackTrace();
		}
	}
	public LazyImageLoader clearCache(URL url) {
		mMemoryCache.remove(url);
		mFileCache.clear();
		return instance;
	}
	
	public void displayImageOnScroll(int pos,final URL url,final ImageView imageview, int reqWidth,
			 int reqHeight,  int fallback){
		if (url == null) {
			imageview.setImageResource(fallback);
			return;
		}
		Bitmap bitmap = getImgFromLocal(url, reqWidth, reqHeight);
		if (bitmap != null) {
			imageview.setImageBitmap(bitmap);
		} else {
			mImageViews.put(imageview, url);
			imageview.setImageResource(fallback);
		    final int mPos = pos;
		    final int reqWidthFinal = reqWidth % 2 == 0 ? reqWidth : reqWidth + 1;
		    final int reqHeightFinal = reqHeight % 2 == 0 ? reqHeight : reqHeight + 1;
		    final int fallbackFinal = fallback;
			new Thread(new Runnable() {
				@Override
				public void run() {
					if(!mAllowLoad){
						synchronized (lock) {
							try {
								lock.wait();
							} catch (InterruptedException e1) {
								 
							}
						}
					}
					
					if(mAllowLoad && firstLoad){
						handler.post(new Runnable() {
							@Override
							public void run() {
								queuePhotoFromWeb(url, imageview, reqWidthFinal, reqHeightFinal, fallbackFinal);
							}
						});
					}
					
					if(mAllowLoad && mPos <= mStopLoadLimit && mPos >= mStartLoadLimit){
						handler.post(new Runnable() {
							@Override
							public void run() {
								queuePhotoFromWeb(url, imageview, reqWidthFinal, reqHeightFinal, fallbackFinal);
							}
						});
					}
				}
			}).start();
		}
	}

	private Bitmap getImgFromLocal(final URL url, int reqWidth, int reqHeight) {
		Bitmap bitmap = mMemoryCache.get(url);
		if(bitmap==null){
			File f = mFileCache.getFile(url);
			// from SD cache
			bitmap = decodeFile(f, reqWidth, reqHeight);
		}
		return bitmap;
	}
	
	public void displayImageNoCache(URL url, ImageView imageview, int reqWidth,
			int reqHeight, int fallback) {
		
		
		if (url == null) {
			imageview.setImageResource(fallback);
			return;
		}

		reqWidth = reqWidth % 2 == 0 ? reqWidth : reqWidth + 1;
		reqHeight = reqHeight % 2 == 0 ? reqHeight : reqHeight + 1;
		 
		imageview.setImageResource(fallback);
		mImageViews.put(imageview, url);
		queuePhotoFromWeb(url, imageview, reqWidth, reqHeight, fallback);
			
		 
	}
	
	/**
	 * 从url中加载图
	 * 
	 * @param url
	 * @param imageview
	 */
	public void displayImage(URL url, ImageView imageview, int reqWidth,
			int reqHeight, int fallback) {
		
		
		if (url == null) {
			imageview.setImageResource(fallback);
			return;
		}

		reqWidth = reqWidth % 2 == 0 ? reqWidth : reqWidth + 1;
		reqHeight = reqHeight % 2 == 0 ? reqHeight : reqHeight + 1;
		
		Bitmap bitmap = getImgFromLocal(url, reqWidth, reqHeight);
		if (bitmap != null) {
			imageview.setImageBitmap(bitmap);
		} else {
			imageview.setImageResource(fallback);
			mImageViews.put(imageview, url);
			queuePhotoFromWeb(url, imageview, reqWidth, reqHeight, fallback);
			
		}
	}

	private void copyStream(InputStream is, OutputStream os) {
		final int buffer_size = 1024;
		try {
			byte[] bytes = new byte[buffer_size];
			for (;;) {
				int count = is.read(bytes, 0, buffer_size);
				if (count == -1) {
					break;
				}
				os.write(bytes, 0, count);
			}
		} catch (Exception ex) {
		}
	}

	// decodes image and scales it to reduce memory consumption
	private Bitmap decodeFile(File f,  int reqWidth,
			int reqHeight) {
		try {
			// decode image size
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, options);

			// Find the correct scale value. It should be the power of 2.
			int width_tmp = options.outWidth, height_tmp = options.outHeight;
			int scale = 1;
			while (true) {
				if (width_tmp / 2 < reqWidth || height_tmp / 2 < reqHeight) {
					break;
				}
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}

			// decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	private Bitmap getBitmapFromFile(File file, ImageView imageview,
			int reqWidth, int reqHeight) {
		if (file == null)
			return null;
		File f = mFileCache.getFile(file);

		// from SD cache
		Bitmap bitmap = decodeFile(f,  reqWidth, reqHeight);

		if (bitmap != null)
			return bitmap;
		else {
			bitmap = decodeFile(file,  reqWidth, reqHeight);
			if (bitmap == null)
				return null;
			try {
				FileOutputStream fos = new FileOutputStream(f);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
				fos.flush();
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return bitmap;

	}

	/**
	 * 总是从网络获取获取
	 * @param url
	 * @param imageview
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	private Bitmap getBitmapFromWeb(URL url, ImageView imageview, int reqWidth,
			int reqHeight) {
		if (url == null)
			return null;
		File f = mFileCache.getFile(url);
		// from SD cache
		Bitmap b = decodeFile(f, reqWidth, reqHeight);
		// from web
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			conn.setInstanceFollowRedirects(true);
			InputStream is = conn.getInputStream();
			OutputStream os = new FileOutputStream(f);
			copyStream(is, os);
			is.close();
			os.close();
			b = decodeFile(f, reqWidth, reqHeight);
			mMemoryCache.put(url, b);
			 
		} catch (Exception ex) {
			Log.e(TAG, ex.getStackTrace().toString());
		}
		
		return b;
	}

	private void queuePhoto(File file, ImageView imageview, int reqWidth,
			int reqHeight, int fallback) {
		LocalImageToLoad p = new LocalImageToLoad(file, imageview);
		mExecutorService.submit(new LocalImageLoader(p));
	}

	private void queuePhotoFromWeb(URL url, ImageView imageview, int reqWidth,
			int reqHeight, int fallback) {
		WebImageToLoad p = new WebImageToLoad(url, imageview, reqWidth,
				reqHeight, fallback);
		mExecutorService.submit(new WebImageLoader(p));
	}

	boolean imageViewReused(LocalImageToLoad imagetoload) {
		Object tag = mImageViews.get(imagetoload.imageview);
		if (tag == null || !tag.equals(imagetoload.file))
			return true;
		return false;
	}

	boolean imageViewReused(WebImageToLoad imagetoload) {
		Object tag = mImageViews.get(imagetoload.imageview);
		if (tag == null || !tag.equals(imagetoload.url))
			return true;
		return false;
	}

	/**
	 * 内存缓存
	 * 
	 * @author qibao.xieqb
	 * 
	 */
	public class MemoryCache {

		private Map<Object, SoftReference<Bitmap>> mCache = Collections
				.synchronizedMap(new HashMap<Object, SoftReference<Bitmap>>());

		public void clear() {
			mCache.clear();
		}

		public Bitmap get(Object tag) {
			if (!mCache.containsKey(tag))
				return null;
			SoftReference<Bitmap> ref = mCache.get(tag);
			return ref.get();
		}

		public void put(Object id, Bitmap bitmap) {
			mCache.put(id, new SoftReference<Bitmap>(bitmap));
			
		}
		
		public void remove(Object key){
			mCache.remove(key);
			
		}
	}

	/**
	 * 文件缓存
	 * 
	 * @author qibao.xieqb
	 * 
	 */
	private class FileCache {

		private File cacheDir;

		public FileCache(Context context) {
			/* Find the dir to save cached images. */
			
		    cacheDir = context.getCacheDir();
			
			if (cacheDir != null && !cacheDir.exists()) {
				cacheDir.mkdirs();
			}
		}
         
		public void clear() {
			if (cacheDir == null)
				return;
			File[] files = cacheDir.listFiles();
			if (files == null)
				return;
			for (File f : files) {
				f.delete();
			}
		}

		/**
		 * I identify images by hashcode. Not a perfect solution, good for the
		 * demo.
		 */
		public File getFile(Object tag) {
			if (cacheDir == null)
				return null;
			String filename = Integer.toHexString(tag.hashCode());
			File f = new File(cacheDir, filename);
			return f;
		}

	}

	// Used to display bitmap in the UI thread
	private class LocalBitmapDisplayer implements Runnable {

		Bitmap bitmap;
		LocalImageToLoad imagetoload;

		public LocalBitmapDisplayer(Bitmap b, LocalImageToLoad p) {
			bitmap = b;
			imagetoload = p;
		}

		@Override
		public void run() {
			if (imageViewReused(imagetoload))
				return;
			if (bitmap != null) {
				imagetoload.imageview.setImageBitmap(bitmap);
			} else {
				imagetoload.imageview
						.setImageResource(R.drawable.ic_launcher);
			}
		}
	}

	private class LocalImageLoader implements Runnable {

		LocalImageToLoad imagetoload;

		LocalImageLoader(LocalImageToLoad imagetoload) {
			this.imagetoload = imagetoload;
		}

		@Override
		public void run() {
			if (imageViewReused(imagetoload) || imagetoload.file == null)
				return;
			Bitmap bmp = getBitmapFromFile(imagetoload.file,
					imagetoload.imageview, imagetoload.reqWidth,
					imagetoload.reqHeight);
			mMemoryCache.put(imagetoload.file, bmp);
			if (imageViewReused(imagetoload))
				return;
			LocalBitmapDisplayer bd = new LocalBitmapDisplayer(bmp, imagetoload);
			Activity a = (Activity) imagetoload.imageview.getContext();
			a.runOnUiThread(bd);
		}
	}

	private class LocalImageToLoad {

		public File file;
		public ImageView imageview;
		public int reqWidth, reqHeight, fallback;

		public LocalImageToLoad(File file, ImageView imageview) {
			this.file = file;
			this.imageview = imageview;
		}
	}

	// Used to display bitmap in the UI thread
	private class WebBitmapDisplayer implements Runnable {

		Bitmap bitmap;
		WebImageToLoad imagetoload;

		public WebBitmapDisplayer(Bitmap b, WebImageToLoad p) {
			bitmap = b;
			imagetoload = p;
		}

		@Override
		public void run() {
			if (imageViewReused(imagetoload))
				return;
			if (bitmap != null) {
				imagetoload.imageview.setImageBitmap(bitmap);
			} else{
				imagetoload.imageview
						.setImageResource(imagetoload.fallback);
			}
		}
	}

	private class WebImageLoader implements Runnable {

		WebImageToLoad imagetoload;

		WebImageLoader(WebImageToLoad imagetoload) {
			this.imagetoload = imagetoload;
		}

		@Override
		public void run() {
			if (imageViewReused(imagetoload) || imagetoload.url == null)
				return;
			Bitmap bmp = getBitmapFromWeb(imagetoload.url,
					imagetoload.imageview, imagetoload.reqWidth,
					imagetoload.reqHeight);
			
			if (imageViewReused(imagetoload))
				return;
			WebBitmapDisplayer bd = new WebBitmapDisplayer(bmp, imagetoload);
			Activity a = (Activity) imagetoload.imageview.getContext();
			a.runOnUiThread(bd);
		}
	}

	// Task for the queue
	private class WebImageToLoad {

		public URL url;
		public ImageView imageview;
		public int reqWidth, reqHeight, fallback;

		public WebImageToLoad(URL url, ImageView imageview, int reqWidth,
				int reqHeight, int fallback) {
			this.url = url;
			this.imageview = imageview;
			this.reqWidth = reqWidth;
			this.reqHeight = reqHeight;
			this.fallback = fallback;

		}
	}

}
