package com.exercise.AndroidCamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.exercise.AndroidCamera.FlashButton.FlashListener;
import com.exercise.AndroidCamera.R;
import com.exercise.AndroidCamera.FlashButton;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AndroidCamera extends Activity implements SurfaceHolder.Callback{

	Camera mCamera;
	private PictureCallback mPicture;
	
	CameraSurfaceView cameraSurfaceView;
	SurfaceHolder surfaceHolder;
	boolean previewing = false;
	private Button switchCamera,buttonTakePicture;
	private FlashButton btnFlash;
	private Context myContext;
	LayoutInflater controlInflater = null;
	private boolean cameraFront = false;
	
	 // We need the phone orientation to correctly draw the overlay:
    private int mOrientation;
    private int mOrientationCompensation;
    private OrientationEventListener mOrientationEventListener;

    // Let's keep track of the display rotation and orientation also:
    private int mDisplayRotation;
    private int mDisplayOrientation;
    
    private Camera.Face[] mFaces;
    // The surface view for the camera data
    
    // Draw rectangles and other fancy stuff:
    private FaceView mFaceView;
	private static  final int FOCUS_AREA_SIZE= 300;

	int cameraId = -1;
	TextView prompt;
	
	DrawingView drawingView;
	Face[] detectedFaces;
	
	final int RESULT_SAVEIMAGE = 0;
	
	private ScheduledExecutorService myScheduledExecutorService;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		myContext = this;
     //   setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        getWindow().setFormat(PixelFormat.UNKNOWN);
        cameraSurfaceView = (CameraSurfaceView)findViewById(R.id.camerapreview);
        surfaceHolder = cameraSurfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        drawingView = new DrawingView(this);
        LayoutParams layoutParamsDrawing 
        	= new LayoutParams(LayoutParams.FILL_PARENT, 
        			LayoutParams.FILL_PARENT);
        this.addContentView(drawingView, layoutParamsDrawing);
        
        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.control, null);
        LayoutParams layoutParamsControl 
        	= new LayoutParams(LayoutParams.FILL_PARENT, 
        			LayoutParams.FILL_PARENT);
        this.addContentView(viewControl, layoutParamsControl);
        
        mOrientationEventListener = new SimpleOrientationEventListener(this);
        mOrientationEventListener.enable();
        
        switchCamera = (Button) findViewById(R.id.btnChangeCamera);
		switchCamera.setOnClickListener(switchCameraListener);
        
        buttonTakePicture = (Button)findViewById(R.id.btntakepicture);
        buttonTakePicture.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				mCamera.takePicture(myShutterCallback, 
						myPictureCallback_RAW, mPicture);
			}});

        
        prompt = (TextView)findViewById(R.id.prompt);
        
		
		btnFlash =(FlashButton) findViewById(R.id.btnflash);
		btnFlash.setFlashListener(flashClick);
		//setCameraDisplayOrientation(this, cameraId, mCamera);
		
	}
    

	
	FlashListener flashClick = new FlashListener(){
	 	@Override
		public void onAutomatic() {
	 		final Camera.Parameters params = mCamera.getParameters();
	 		params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			 mCamera.setParameters(params);
		}
		@Override
		public void onOn() {
			final Camera.Parameters params = mCamera.getParameters();
			params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			mCamera.setParameters(params);
		}
		@Override
		public void onOff() {
			final Camera.Parameters params = mCamera.getParameters();
			params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			mCamera.setParameters(params);	
		}
	};
	
	OnClickListener switchCameraListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			//get the number of cameras
			int camerasNumber = Camera.getNumberOfCameras();
			if (camerasNumber > 1) {
				//release the old camera instance
				//switch camera, from the front and the back and vice versa
				
				releaseCamera();
				chooseCamera();
			} else {
				Toast toast = Toast.makeText(myContext, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
				toast.show();
			}
		}
	};

	
	public void chooseCamera() {
		//if the camera preview is the front
		if (cameraFront) {
			int cameraId = findBackFacingCamera();
			if (cameraId >= 0) {
				//open the backFacingCamera
				//set a picture callback
				//refresh the preview
			
				mCamera = Camera.open(cameraId);
				mPicture = getPictureCallback();
				//mCamera.startPreview();
				refreshCamera(mCamera,cameraId);
			}
		} else {
			int cameraId = findFrontFacingCamera();
			if (cameraId >= 0) {
				//open the backFacingCamera
				//set a picture callback
				//refresh the preview
				
				mCamera = Camera.open(cameraId);
				mPicture = getPictureCallback();
				//mCamera.startPreview();
				refreshCamera(mCamera,cameraId);
			}
		}
	}
	
	public void setCamera(Camera camera) {
		//method to set a camera instance
		mCamera = camera;
	}
	
	public void refreshCamera(Camera camera,int cameraId) {
		if(previewing){
			mCamera.stopFaceDetection();
			//mCamera.stopPreview();
			previewing = false;
		}
		 
		if (surfaceHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}
		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}
		// set preview size and make any resize, rotate or
		// reformatting changes here
		// start preview with new settings
//			mCameraPreview.setOnTouchListener(new View.OnTouchListener() {
//	        @Override
//	        public boolean onTouch(View v, MotionEvent event) {
//	            if (event.getAction() == MotionEvent.ACTION_DOWN) {
//	                focusOnTouch(event);
//	            }
//	            return true;
//	        }
//	    });
		setCamera(camera);
		try {
			// Camera.Parameters parameters = mCamera.getParameters();
		        
			mCamera.setPreviewDisplay(surfaceHolder);
		//  setCameraDisplayOrientation((Activity)this,cameraId,mCamera);
		//	 configureCamera(width, height);
		        setDisplayOrientation();
		   
			//parameters.set("orientation", "portrait");
			//parameters.setRotation(90);
//			mCamera.setDisplayOrientation(90);
			 
		//mCamera.setParameters(parameters);
			mCamera.startPreview();
			mCamera.startFaceDetection();
			previewing = true;
		} catch (Exception e) {
			Log.d("RefreshCam", "Error starting camera preview: " + e.getMessage());
		}
	}
    
    private int findFrontFacingCamera() {
		// Search for the front facing camera
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				cameraFront = true;
				cameraId=i;
				break;
			}
		}
		return cameraId;
	}
    
    
	private int findBackFacingCamera() {
		//Search for the back facing camera
		//get the number of cameras
		int numberOfCameras = Camera.getNumberOfCameras();
		//for every camera check
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				cameraFront = false;
				cameraId=i;
				break;
			}
		}
		return cameraId;
	}
    
	public void onResume() {
		super.onResume();
		if (!hasCamera(myContext)) {
			Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
			toast.show();
			finish();
		}
		if (mCamera == null) {
			//if the front facing camera does not exist
			if (findFrontFacingCamera() < 0) {
				Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
				switchCamera.setVisibility(View.GONE);
			}			
			  
			mCamera = Camera.open(findBackFacingCamera());
			mPicture = getPictureCallback();
			mOrientationEventListener.enable();
			refreshCamera(mCamera,cameraId);
			
			//cameraSurfaceView.refreshCamera(mCamera);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mOrientationEventListener.disable();
		//when on Pause, release camera in order to be used from other applications
		releaseCamera();
	}
	
	public static void addPicToGallery(Context context, String photoPath) {
	    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	    File f = new File(photoPath);
	    Uri contentUri = Uri.fromFile(f);
	    mediaScanIntent.setData(contentUri);
	    context.sendBroadcast(mediaScanIntent);
	  
	}

	private void releaseCamera() {
		// stop and release camera
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}

	private boolean hasCamera(Context context) {
		//check if the device has camera
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return true;
		} else {
			return false;
		}
	}

	
    public void touchFocus(final Rect tfocusRect){
    	
    	buttonTakePicture.setEnabled(false);
		
    	mCamera.stopFaceDetection();
		
    	//Convert from View's width and height to +/- 1000
		final Rect targetFocusRect = new Rect(
				tfocusRect.left * 2000/drawingView.getWidth() - 1000,
				tfocusRect.top * 2000/drawingView.getHeight() - 1000,
				tfocusRect.right * 2000/drawingView.getWidth() - 1000,
				tfocusRect.bottom * 2000/drawingView.getHeight() - 1000);
		
		final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
		Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
		focusList.add(focusArea);
		
		Parameters para = mCamera.getParameters();
		para.setFocusAreas(focusList);
		para.setMeteringAreas(focusList);
		mCamera.setParameters(para);
		
		mCamera.autoFocus(myAutoFocusCallback);
		
		drawingView.setHaveTouch(true, targetFocusRect);
  		drawingView.invalidate();
    }
    
    FaceDetectionListener faceDetectionListener
    = new FaceDetectionListener(){

		@Override
		public void onFaceDetection(Face[] faces, Camera tcamera) {
			
			if (faces.length == 0){
				prompt.setText(" No Face Detected! ");
				drawingView.setHaveFace(false);
			}else{
				prompt.setText(String.valueOf(faces.length) + " Face Detected :) ");
				drawingView.setHaveFace(true);
				detectedFaces = faces;
				
				//Set the FocusAreas using the first detected face
				List<Camera.Area> focusList = new ArrayList<Camera.Area>();
				Camera.Area firstFace = new Camera.Area(faces[0].rect, 1000);
				focusList.add(firstFace);
				
				Parameters para = mCamera.getParameters();
				
				if(para.getMaxNumFocusAreas()>0){
					para.setFocusAreas(focusList);
			    }
			    
			    if(para.getMaxNumMeteringAreas()>0){
					para.setMeteringAreas(focusList);
				}
			    
			    mCamera.setParameters(para);

				//Stop further Face Detection
				mCamera.stopFaceDetection();
				
				buttonTakePicture.setEnabled(false);
				
				/*
				 * Allways throw java.lang.RuntimeException: autoFocus failed 
				 * if I call autoFocus(myAutoFocusCallback) here!
				 * 
					camera.autoFocus(myAutoFocusCallback);
				*/
				
				//Delay call autoFocus(myAutoFocusCallback)
				myScheduledExecutorService = Executors.newScheduledThreadPool(1);
				myScheduledExecutorService.schedule(new Runnable(){
				      public void run() {
				    	  mCamera.autoFocus(myAutoFocusCallback);
				        }
				      }, 500, TimeUnit.MILLISECONDS);

			}
			
			drawingView.invalidate();
			
		}};
    
    AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback(){

		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			// TODO Auto-generated method stub
			if (arg0){
				buttonTakePicture.setEnabled(true);
				mCamera.cancelAutoFocus();						
			}
			
			float focusDistances[] = new float[3];
			arg1.getParameters().getFocusDistances(focusDistances);
			prompt.setText("Optimal Focus Distance(meters): " 
					+ focusDistances[Camera.Parameters.FOCUS_DISTANCE_OPTIMAL_INDEX]);

		}};
    
    ShutterCallback myShutterCallback = new ShutterCallback(){

		@Override
		public void onShutter() {
			// TODO Auto-generated method stub
			
		}};
		
	PictureCallback myPictureCallback_RAW = new PictureCallback(){

		@Override
		public void onPictureTaken(byte[] arg0, Camera arg1) {
			// TODO Auto-generated method stub
			
		}};
		
		private File getOutputMediaFile() {
			File mediaStorageDir = Environment.getExternalStoragePublicDirectory(
				            Environment.DIRECTORY_DCIM
				            + "/Camera");
			
			//if  folder does not exist
			if (!mediaStorageDir.exists()) {
				if (!mediaStorageDir.mkdirs()) {
					return null;
				}
			}
			Context context = getApplicationContext();
			
			//take the current timeStamp
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			File mediaFile;
			//and make a media file:
			mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
			//Toast.makeText(context,mediaStorageDir.getPath(), Toast.LENGTH_LONG).show();
			return mediaFile;
		}
		
		private PictureCallback getPictureCallback() {
			PictureCallback picture = new PictureCallback(){
		
				public void onPictureTaken(byte[] data, Camera camera) {
					//make a new picture file
					File pictureFile = getOutputMediaFile();
					
					if (pictureFile == null) {
						return;
					}
					try {
						
						Bitmap newImage = null;
			            Bitmap cameraBitmap;
			            if (data != null) {
			                cameraBitmap = BitmapFactory.decodeByteArray(data, 0, (data != null) ? data.length : 0);
			                // use matrix to reverse image data and keep it normal
		                    Matrix mtx = new Matrix();
		      
			                if (cameraFront) {
			                    //this will prevent mirror effect
			                    mtx.preScale(-1.0f, 1.0f);
			                }
			                // Setting post rotate to 90 because image will be possibly in landscape
			                
			                mtx.postRotate(90.f);
		                    // Rotating Bitmap , create real image that we want
		                    cameraBitmap = Bitmap.createBitmap(cameraBitmap, 0, 0, cameraBitmap.getWidth(), cameraBitmap.getHeight(), mtx, true);
		               
			                FileOutputStream fos = new FileOutputStream(pictureFile);
							cameraBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
							
							addPicToGallery(myContext,pictureFile.getPath());
							Toast toast = Toast.makeText(myContext, "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
							toast.show();
							fos.close();
			            }
						
					} catch (FileNotFoundException e) {
					} catch (IOException e) {
					}
					refreshCamera(mCamera,cameraId);
					//refresh camera to continue preview
					//mPreview.refreshCamera(mCamera,cameraId);
				}
			};
		
			return picture;
	}
		
		 private void setDisplayOrientation() {
		        // Now set the display orientation:
		        mDisplayRotation = Util.getDisplayRotation(AndroidCamera.this);
		        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, cameraId);

		        mCamera.setDisplayOrientation(mDisplayOrientation);
//		        Camera.Parameters parameters = mCamera.getParameters();
//////				
//////		     
//		        parameters.setRotation(270);
//////			 
//		       
//		        //parameters.setRotation(mDisplayOrientation);
//////				 
//				mCamera.setParameters(parameters);
//		        if (mFaceView != null) {
//		            mFaceView.setDisplayOrientation(mDisplayOrientation);
		        //}
		    }
		
		   private void configureCamera(int width, int height) {
		        Camera.Parameters parameters = mCamera.getParameters();
		        // Set the PreviewSize and AutoFocus:
		        setOptimalPreviewSize(parameters, width, height);
		     //   setAutoFocus(parameters);
		        // And set the parameters:
		        mCamera.setParameters(parameters);
		    }

		    private void setOptimalPreviewSize(Camera.Parameters cameraParameters, int width, int height) {
		        List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
		        float targetRatio = (float) width / height;
		        Camera.Size previewSize = Util.getOptimalPreviewSize(this, previewSizes, targetRatio);
		        cameraParameters.setPreviewSize(previewSize.width, previewSize.height);
		    }
		    
		
		public void setCameraDisplayOrientation(Activity activity , int icameraId , Camera camera1s)
	    {
	        CameraInfo cameraInfo = new CameraInfo();

	        Camera.getCameraInfo(icameraId, cameraInfo);

	        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

	        int degrees = 0; 

	        switch (rotation)
	        {
	        case Surface.ROTATION_0:
	            degrees = 0;
	            break;
	        case Surface.ROTATION_90:
	            degrees = 90;
	            break;
	        case Surface.ROTATION_180:
	            degrees = 180;
	            break;
	        case Surface.ROTATION_270:
	            degrees = 270;
	            break;

	        }

	        int result;
	       
			
	        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
	        {
	            // cameraType=CAMERATYPE.FRONT;

	            result = (cameraInfo.orientation + degrees) % 360;
	            result = (360 - result) % 360; // compensate the mirror

	        }
	        else
	        { // back-facing

	            result = (cameraInfo.orientation - degrees + 360) % 360;

	        }
	//         displayRotate=result;
	       mCamera.setDisplayOrientation(result);
	       Camera.Parameters parameters = mCamera.getParameters();
			
	        parameters.set("orientation", "portrait");
			parameters.setRotation(90);
			 
			mCamera.setParameters(parameters);

	    }
		

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		if(previewing){
			mCamera.stopFaceDetection();
			mCamera.stopPreview();
			previewing = false;
		}
		
		if (mCamera != null){
			try {
				
			
			
			 configureCamera(width, height);
		        setDisplayOrientation();
		        
				//Camera.Parameters parameters = mCamera.getParameters();
				mCamera.setPreviewDisplay(surfaceHolder);        
				//etCameraDisplayOrientation((Activity)this,cameraId,mCamera);
					 
				//mCamera.setParameters(parameters);
			
				mCamera.startPreview();

				prompt.setText(String.valueOf(
						"Max Face: " + mCamera.getParameters().getMaxNumDetectedFaces()));
				mCamera.startFaceDetection();
				previewing = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mCamera = Camera.open();
		 mCamera.startFaceDetection();
		 try {
	            mCamera.setPreviewDisplay(surfaceHolder);
	        } catch (Exception e) {
	            Log.e("sdf", "Could not preview the image.", e);
	        }
		mCamera.setFaceDetectionListener(faceDetectionListener);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		//mCamera.stopFaceDetection();
		//mCamera.stopPreview();
		//mCamera.release();
		//mCamera = null;
		//previewing = false;
		releaseCamera();
	}
	
	private class DrawingView extends View{
		
		boolean haveFace;
		Paint drawingPaint;
		
		boolean haveTouch;
		Rect touchArea;

		public DrawingView(Context context) {
			super(context);
			haveFace = false;
			drawingPaint = new Paint();
			drawingPaint.setColor(Color.GREEN);
			drawingPaint.setStyle(Paint.Style.STROKE); 
			drawingPaint.setStrokeWidth(2);
			
			haveTouch = false;
		}
		
		public void setHaveFace(boolean h){
			haveFace = h;
		}
		
		public void setHaveTouch(boolean t, Rect tArea){
			haveTouch = t;
			touchArea = tArea;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// TODO Auto-generated method stub
			 int vWidth = getWidth();
			 int vHeight = getHeight();
			
			if(haveFace){

				// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
				 // UI coordinates range from (0, 0) to (width, height).
				 
				
				for(int i=0; i<detectedFaces.length; i++){
					
					if(i == 0){
						drawingPaint.setColor(Color.GREEN);
					}else{
						drawingPaint.setColor(Color.RED);
					}
					
					int l = detectedFaces[i].rect.left;
					int t = detectedFaces[i].rect.top;
					int r = detectedFaces[i].rect.right;
					int b = detectedFaces[i].rect.bottom;
//					canvas.drawRect(
//							l, t, r, b,  
//							drawingPaint);
					int left	= (l+1000) * vWidth/2000;
					int top		= (t+1000) * vHeight/2000;
					int right	= (r+1000) * vWidth/2000;
					int bottom	= (b+1000) * vHeight/2000;
					canvas.drawRect(
							left, top, right, bottom,  
							drawingPaint);
				}
			}else{
				canvas.drawColor(Color.TRANSPARENT);
			}
			
			if(haveTouch){
				drawingPaint.setColor(Color.BLUE);
				int left	= (touchArea.left+1000) * vWidth/2000;
				int top		= (touchArea.top+1000) * vHeight/2000;
				int right	= (touchArea.right+1000) * vWidth/2000;
				int bottom	= (touchArea.bottom+1000) * vHeight/2000;
//				canvas.drawRect(
//						left, top, right, bottom,  
//						drawingPaint);
//				canvas.drawRect(
//						10, 10, 30, 30,  
//						drawingPaint);
								canvas.drawRect(
						touchArea.left, touchArea.top, touchArea.right, touchArea.bottom,  
						drawingPaint);
			}
		}
		
	}
	 private class SimpleOrientationEventListener extends OrientationEventListener {

	        public SimpleOrientationEventListener(Context context) {
	            super(context, SensorManager.SENSOR_DELAY_NORMAL);
	        }

	        @Override
	        public void onOrientationChanged(int orientation) {
	            // We keep the last known orientation. So if the user first orient
	            // the camera then point the camera to floor or sky, we still have
	            // the correct orientation.
	            if (orientation == ORIENTATION_UNKNOWN) return;
	            mOrientation = Util.roundOrientation(orientation, mOrientation);
	            // When the screen is unlocked, display rotation may change. Always
	            // calculate the up-to-date orientationCompensation.
	            int orientationCompensation = mOrientation
	                    + Util.getDisplayRotation(AndroidCamera.this);
	            if (mOrientationCompensation != orientationCompensation) {
	                mOrientationCompensation = orientationCompensation;
	                //mFaceView.setOrientation(mOrientationCompensation);
	            }
	        }
	 }
}