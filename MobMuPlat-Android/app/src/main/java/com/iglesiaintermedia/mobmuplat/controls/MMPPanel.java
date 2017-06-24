package com.iglesiaintermedia.mobmuplat.controls;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.iglesiaintermedia.mobmuplat.MainActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;

import static java.lang.Math.abs;

public class MMPPanel extends MMPControl {
	
	//private String _imagePath;
	private boolean _shouldPassTouches;
	private boolean _showWarningText;
	private boolean _highlighted;
	private RectF _myRect;
	private Bitmap _imageBitmap;
	private int opaqueColor = 0xFFFFFFFF;
	private String _imagePath;

	private int _pixel_r = 0, _pixel_g = 0,  _pixel_b = 0;
	private float _pos_x, _pos_y;

	//topleft
	private float lat1 = 0;
	private float lon1 = 0;

	//bottom right
	private float lat2 = 0;
	private float lon2 = 0;

	public MMPPanel(Context context, float screenRatio) {
		super(context, screenRatio);
		_myRect = new RectF();
		_pos_x = 0;
		_pos_y = 0;
//
//		lat1 = (float) -7.735333;
//		lon1 = (float) 110.385055;
//
//		lat2 = (float) -7.715313;
//		lon2 = (float) 110.405207;




	}


	public double convertPdGPS(float val1, float val2) {
		double reverse = 1;
		if (val1 < 0) reverse = -1;
		return (abs(val1) + (val2/1e+06)) * reverse;
	}


	private void sendValues() {
		List<Object> args = new ArrayList<Object>();
		args.add(this.address);
		args.add(Float.valueOf(_pos_x));
		args.add(Float.valueOf(_pos_y));

		args.add(Float.valueOf(_pixel_r));
		args.add(Float.valueOf(_pixel_g));
		args.add(Float.valueOf(_pixel_b));
		this.controlDelegate.sendGUIMessageArray(args);
	}

	private void setValues(int r_value, int g_value, int b_value) {
		_pixel_r = r_value;
		_pixel_g = g_value;
		_pixel_b = b_value;
		invalidate();
	}

	public void setImagePath(String path) {//takes full path. widget may not be laid out.
		/*if(_imageBitmap!=null) _imageBitmap.recycle();
		_imageBitmap = BitmapFactory.decodeFile(path);
		invalidate();*/
		if (path!=null && !path.equals(_imagePath)) {
			_imagePath = path;
			maybeRefreshImage();
		}
	}
	
	public void setShouldPassTouches(boolean shouldPassTouches) {
		_shouldPassTouches = shouldPassTouches;
	}
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRect.set(0,0,right-left, bottom-top);
			maybeRefreshImage();
		}
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if (!this.isEnabled()) return false; //reject touch down if disabled.
		if(!_shouldPassTouches)getParent().requestDisallowInterceptTouchEvent(true);
		return true;
	}
	
	private void maybeRefreshImage() {
		if (_imagePath!=null && !_myRect.isEmpty()) {
			if(_imageBitmap!=null) _imageBitmap.recycle();
			//_imageBitmap = decodeSampledBitmapFromFile(_imagePath, (int)_myRect.width(), (int)_myRect.height());
			//invalidate();
			BitmapWorkerTask task = new BitmapWorkerTask(this, _imagePath, (int)_myRect.width(), (int)_myRect.height());
			task.execute();
		}
	}
	
	protected void onDraw(Canvas canvas) {
		
        	//this.paint.setStyle(Paint.Style.FILL);
		this.paint.setFilterBitmap(true);
        if (_highlighted) this.paint.setColor(this.highlightColor);
        else this.paint.setColor(this.color);
        canvas.drawRect(_myRect, this.paint);
        
        //set paint to opaque
        this.paint.setColor(opaqueColor);
        if(_imageBitmap!=null){
        	canvas.drawBitmap(_imageBitmap, null, _myRect, this.paint);
        }
        
		if(_showWarningText){
			this.paint.setColor(Color.BLACK);
			paint.setTextSize(20*this.screenRatio);
			canvas.drawText("image file not found", 10,20*this.screenRatio,this.paint);
		}

		canvas.drawCircle(_pos_x*canvas.getWidth(), _pos_y*canvas.getHeight(), (float)15, this.paint);
	}

	public float normalizeLatToBound(double lat) {
		double width = abs(lat1 - lat2);
		return (float) (abs(lat-lat1)/width);
	}

	public float normalizeLonToBound(double lon) {
		double width = abs(lon1 - lon2);
		return (float) (abs(lon-lon1)/width);
	}


	public void receiveList(List<Object> messageArray){ 
		super.receiveList(messageArray);
    	//image path
		if (messageArray.size()==2 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("image")){
			String path =  (String)messageArray.get(1);
			File extFile = new File(/*Environment.getExternalStorageDirectory()*/ MainActivity.getDocumentsFolderPath(), path);
            setImagePath(extFile.getAbsolutePath());
		}

		if (messageArray.size()==3 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("getpix")){
			float pos_x = (((Float)(messageArray.get(1))).floatValue());
			float pos_y = (((Float)(messageArray.get(2))).floatValue());

			_pos_x = pos_x;
			_pos_y = pos_y;

			if (_imageBitmap != null)
			{
				//clamp x and y
				if (pos_x > _imageBitmap.getWidth()) pos_x = _imageBitmap.getWidth();
				if (pos_y > _imageBitmap.getHeight()) pos_y = _imageBitmap.getHeight();

				int pixcolors = _imageBitmap.getPixel(((int)(pos_x*_imageBitmap.getWidth())), ((int)(pos_y*_imageBitmap.getHeight())));

				int redValue = Color.red(pixcolors);
				int blueValue = Color.blue(pixcolors);
				int greenValue = Color.green(pixcolors);


				_pixel_r = redValue;
				_pixel_g = greenValue;
				_pixel_b = blueValue;

				//setValues(redValue, greenValue, blueValue);
				sendValues();

			}

			invalidate();
		}

		if (messageArray.size()==9 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("setgpsbound")) {

			float val1= (((Float)(messageArray.get(1))).floatValue());
			float val2= (((Float)(messageArray.get(2))).floatValue());
			float val3= (((Float)(messageArray.get(3))).floatValue());
			float val4= (((Float)(messageArray.get(4))).floatValue());

			float val5= (((Float)(messageArray.get(5))).floatValue());
			float val6= (((Float)(messageArray.get(6))).floatValue());
			float val7= (((Float)(messageArray.get(7))).floatValue());
			float val8= (((Float)(messageArray.get(8))).floatValue());

			lat1 = (float) convertPdGPS(val3, val4);
			lon1 = (float) convertPdGPS(val1, val2);

			lat2 = (float) convertPdGPS(val7, val8);
			lon2 = (float) convertPdGPS(val5, val6);

			Log.d("pdspat", "lat1: "  + String.valueOf(lat1));
			Log.d("pdspat", "lon1: "  + String.valueOf(lon1));

			Log.d("pdspat", "lat2: "  + String.valueOf(lat2));
			Log.d("pdspat", "lon2: "  + String.valueOf(lon2));




		}


		if (messageArray.size()==1 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("updategpspos")){

			_pos_x = normalizeLonToBound((float)MainActivity.lon1);
			_pos_y = 1-normalizeLatToBound((float)MainActivity.lat1);

			//clamp
			if (_pos_x > 1) _pos_x = 1;
			if (_pos_x < 0) _pos_x = 0;

			if (_pos_y > 1) _pos_y = 1;
			if (_pos_y < 0) _pos_y = 0;

			//Log.d("pdspat", "X: "  + String.valueOf(_pos_x));
			//Log.d("pdspat", "Y: "  + String.valueOf(_pos_y));

			if (_imageBitmap != null)
			{
				//clamp x and y

				int pix_pos_x = ((int)(_pos_x*_imageBitmap.getWidth()))-1;
				int pix_pos_y = ((int)(_pos_y*_imageBitmap.getHeight()))-1;

				if (pix_pos_x > _imageBitmap.getWidth()) pix_pos_x = _imageBitmap.getWidth();
				if (pix_pos_x < 0) pix_pos_x = 0;

				if (pix_pos_y > _imageBitmap.getHeight()) pix_pos_y = _imageBitmap.getHeight();
				if (pix_pos_y < 0) pix_pos_y = 0;


				int pixcolors = _imageBitmap.getPixel(pix_pos_x, pix_pos_y);

				int redValue = Color.red(pixcolors);
				int blueValue = Color.blue(pixcolors);
				int greenValue = Color.green(pixcolors);
				_pixel_r = redValue;
				_pixel_g = greenValue;
				_pixel_b = blueValue;
			}

			sendValues();
			invalidate();
		}

		//highlight
	    if (messageArray.size()==2 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("highlight")){
	    	_highlighted = ((int)(((Float)(messageArray.get(1))).floatValue()) > 0);//ugly
	    	invalidate();
	    }
	}
	
	// Image unpacking, from http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	// caps size to 1024 in either direction - otherwise we can run out of ram too quickly
	static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			//final int halfHeight = height / 2;
			//final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			// AND keep downsizing until larger dim is <= 1024
			while (((height / (inSampleSize * 2)) > reqHeight && (width / (inSampleSize * 2)) > reqWidth) ) { //|| 
					//((height / inSampleSize) > 1024 || (width / inSampleSize) > 1024) ){
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}
	
	static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(path, options);

	    //checkBitmapFitsInMemory(options.outWidth, options.outHeight, 1);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeFile(path, options);
	}
	
	// Not used.
	public static boolean checkBitmapFitsInMemory(long bmpwidth,long bmpheight, int bmpdensity ){

	    long reqsize=bmpwidth*bmpheight*bmpdensity*4;
	    long allocNativeHeap = Debug.getNativeHeapAllocatedSize();
	    long maxMemory = Runtime.getRuntime().maxMemory();

	    final long heapPad=(long) Math.max(4*1024*1024,maxMemory*0.1);
	    boolean over = ((reqsize + allocNativeHeap + heapPad) >= maxMemory);
	    Log.i("IMAGE ALLOC", "heap "+allocNativeHeap+" req "+reqsize+" max "+maxMemory+" over? "+ over);
	    
	    /*{
	        return false;
	    }*/
	    return true;

	}
	
	class BitmapWorkerTask extends AsyncTask<Void, Void, Bitmap> {
	    private final WeakReference<MMPPanel> panelReference;
	    //private int data = 0;
	    private final String path;
	    private final int width, height;
	    
	    public BitmapWorkerTask(MMPPanel panel, String path, int width, int height) {
	        // Use a WeakReference to ensure the ImageView can be garbage collected
	    	panelReference = new WeakReference<MMPPanel>(panel);
	    	this.path = path;
	    	this.width = width;
	    	this.height = height;
	    	
	    }

	    // Decode image in background.
	    @Override
	    protected Bitmap doInBackground(Void... params) {
	        //data = params[0];
	        return decodeSampledBitmapFromFile(path, width, height);
	        //return decodeSampledBitmapFromResource(getResources(), data, 100, 100));
	    }

	    // Once complete, see if ImageView is still around and set bitmap.
	    @Override
	    protected void onPostExecute(Bitmap bitmap) {
	        if (panelReference != null && bitmap != null) {
	            final MMPPanel panel = panelReference.get();
	            if (panel != null) {
	                //imageView.setImageBitmap(bitmap);
	            	panel._imageBitmap = bitmap;
	            	panel.invalidate();
	            }
	        }
	    }
	}
}
