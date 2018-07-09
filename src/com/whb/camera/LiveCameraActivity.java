//                       .::::.
//                     .::::::::.
//                    :::::::::::
//                 ..:::::::::::'
//              '::::::::::::'
//                .::::::::::
//           '::::::::::::::..
//                ..::::::::::::.
//              ``::::::::::::::::
//               ::::``:::::::::'        .:::.
//              ::::'   ':::::'       .::::::::.
//            .::::'      ::::     .:::::::'::::.
//           .:::'       :::::  .:::::::::' ':::::.
//          .::'        :::::.:::::::::'      ':::::.
//         .::'         ::::::::::::::'         ``::::.
//     ...:::           ::::::::::::'              ``::.
//    ```` ':.          ':::::::::'                  ::::..
//                       '.:::::'                    ':'````..
//    女神保佑, 永无bug
package com.whb.camera;

import java.io.IOException;
import java.util.List;

import com.whb.camera.util.SystemUiHider;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.Matrix;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
@SuppressLint("NewApi")
public class LiveCameraActivity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener {
	private final String TAG = "CameraActivity";
	private final boolean DBG = false;
	private Context mContext = null;
	private Camera mCamera = null;
	private TextureView mPreviewSurface = null;
	private Button mVisableButton = null;
	private Button mTransparentButton = null;
	private Button mScaleSize = null;
	
	private final int MIN_RATE = 20;
	private final int MAX_RATE = 30;
	private final float ALPHA = 0.00f;
	
	private static final String SURFACE_ALPHA = "persist.surface.alpha";
	private static final String SURFACE_ALPHA_INIT = "persist.surface.alpha.init";
	
	private final int MSG_SET_SURFACE_ALPHA = 0x01;
	private final int MSG_HIDE_SURFACE = 0x02;
	private final int MSG_SHOW_SURFACE = 0x03;
	
	private final int SET_SURFACE_DELAY_TIME = 3000;
	
	private Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			int action = msg.what;
			switch (action){
			case MSG_SET_SURFACE_ALPHA:
				Log.i(TAG, "received MSG_SET_SURFACE_ALPHA");
				if(mPreviewSurface != null) {
					Float alpha = (Float)msg.obj;
					mPreviewSurface.setAlpha(alpha.floatValue());
				}
				break;
			
			case MSG_SHOW_SURFACE:
				Log.i(TAG, "received MSG_SHOW_SURFACE");
				if(mPreviewSurface != null) {
					mPreviewSurface.setVisibility(View.VISIBLE);
				}
				break;
			
			case MSG_HIDE_SURFACE:
				Log.i(TAG, "received MSG_HIDE_SURFACE");
				if(mPreviewSurface != null) {
					mPreviewSurface.setVisibility(View.INVISIBLE);
				}
				break;
				
			default:
				break;
			};
		}
		
	};
	
	private void setSurfaceAlpha(Float alpha){
		Message msg = myHandler.obtainMessage(MSG_SET_SURFACE_ALPHA, alpha);
		myHandler.sendMessageDelayed(msg, SET_SURFACE_DELAY_TIME);
	}
	
	private void showSurface(){
		Message msg = myHandler.obtainMessage(MSG_SHOW_SURFACE);
		myHandler.sendMessageDelayed(msg, SET_SURFACE_DELAY_TIME);
	}
	
	private void hideSurface(){
		Message msg = myHandler.obtainMessage(MSG_HIDE_SURFACE);
		myHandler.sendMessageDelayed(msg, SET_SURFACE_DELAY_TIME);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		////mPreviewSurface = new TextureView(this);
		// mPreviewSurface.setVisibility(View.GONE);
		//mPreviewSurface.setVisibility(View.INVISIBLE);
		////mPreviewSurface.setSurfaceTextureListener(this);
		setContentView(R.layout.activity_live_camera);
		
		mPreviewSurface = (TextureView)findViewById(R.id.fullscreen_content);
        //mPreviewSurface.setAlpha(ALPHA);
        mPreviewSurface.setSurfaceTextureListener(this);
        
        mVisableButton = (Button)findViewById(R.id.visable);
        mVisableButton.setOnClickListener(this);
        
        mTransparentButton = (Button)findViewById(R.id.transparent);
        mTransparentButton.setOnClickListener(this);

        mScaleSize = (Button)findViewById(R.id.scalesize);
        mScaleSize.setOnClickListener(this);
        
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		//lp.alpha = ALPHA;
		//addContentView(mPreviewSurface, lp);
		
		//setContentView(mPreviewSurface);
	}
	
	private void initCamera() {
		Log.i(TAG, "initCamera(), mCamera: " + mCamera);
		if(mCamera == null) {
			Log.i(TAG, "no camera, init failed!");
			return ;
		}
		
		mCamera.setDisplayOrientation(90);
		
		Camera.Parameters param = mCamera.getParameters();
		
		List<Integer> listPreviewFmt = param.getSupportedPreviewFormats();
		for(Integer fmt : listPreviewFmt) {
			Log.i(TAG, "preview format " + fmt.intValue() + " is supported");
		}
		param.setPictureFormat(listPreviewFmt.get(listPreviewFmt.size()-1));
		
		Size previewSize = param.getPreviewSize();
		Log.i(TAG, "preview size:[" + previewSize.width + "," + previewSize.height + "]");
		param.setPreviewSize(previewSize.width, previewSize.height);
		
		List<int[]> fpsList = param.getSupportedPreviewFpsRange();
		for(int i=0; i<fpsList.size(); i++) {
			int[] fpsRange = fpsList.get(i);
			Log.i(TAG, "fpsRange length " + fpsRange.length);
			Log.i(TAG, "preview fps[" + i + "]: " + fpsRange[0] + " ~ " + fpsRange[1]);
		}
		param.setPreviewFpsRange(fpsList.get(fpsList.size()-1)[0], fpsList.get(fpsList.size()-1)[1]);
		
		List<String> listFocustMode = param.getSupportedFocusModes();
		for(String fcsmode : listFocustMode) {
			Log.i(TAG, "focus mode " + fcsmode + " is supported");
		}
		if(listFocustMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
			param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		}

		//setSurfaceAlpha(new Float(1.0f));
	    //hideSurface();
		//showSurface();
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1,
			int arg2) {
		// TODO Auto-generated method stub
		Toast.makeText(mContext, "SurfaceTexture Available", Toast.LENGTH_LONG).show();
		Log.i(TAG, "onSurfaceTextureAvailable(), width: " + arg1 + ", height:" + arg2);
		mCamera = Camera.open();
		
		initCamera();
		
		try{
			if(mCamera != null) {
				mCamera.setPreviewTexture(arg0);
				mCamera.startPreview();
			}
		} catch(IOException ioe) {
			Log.e(TAG, "start preview fail: " + ioe);
		}
	    
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
		// TODO Auto-generated method stub
		Toast.makeText(mContext, "SurfaceTexture Destroyed", Toast.LENGTH_LONG).show();
		Log.i(TAG, "onSurfaceTextureDestroyed(), SurfaceTexture: " + arg0);
		mCamera.stopPreview();
		mCamera.release();

		return false;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1,
			int arg2) {
		// TODO Auto-generated method stub
		Toast.makeText(mContext, "SurfaceTexture Size Changed", Toast.LENGTH_LONG).show();
		Log.i(TAG, "onSurfaceTextureSizeChanged(), width: " + arg1 + ", height:" + arg2);
		mCamera.getParameters().setPreviewSize(arg1, arg2);
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
		// TODO Auto-generated method stub
		if(DBG) {
		    Log.d(TAG, "onSurfaceTextureAvailable(), SurfaceTexture: " + arg0);
		}
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onClick(), View: " + arg0);
		if(arg0 != null) {
			switch(arg0.getId()) {
			case R.id.visable:
			    int visible = mPreviewSurface.getVisibility();

				Toast.makeText(mContext, "SurfaceTexture Visible? " + viewVisableToString(visible), Toast.LENGTH_SHORT).show();
			    if(visible == View.INVISIBLE) {
			    	mPreviewSurface.setVisibility(View.VISIBLE);
			    	mVisableButton.setText("Hide Preview Surface");
			    } else if(visible == View.VISIBLE) {
			    	mPreviewSurface.setVisibility(View.INVISIBLE);
			    	mVisableButton.setText("Show Preview Surface");
			    } else if(visible == View.GONE) {
			    	mPreviewSurface.setVisibility(View.VISIBLE);
			        mVisableButton.setText("Hide Preview Surface");
			    }
			    break;
			    
			case R.id.transparent:
				float transparent = mPreviewSurface.getAlpha();
				Toast.makeText(mContext, "SurfaceTexture transparent? " + viewTransparentToString(transparent), Toast.LENGTH_SHORT).show();
                if(transparent <= 0.0) {
                	transparent = 1.0f;
                } else {
                	transparent = 0.0f;
                }
			    mPreviewSurface.setAlpha(transparent);
			    mTransparentButton.setText("transparent:" + transparent);
			    break;
			    
			case R.id.scalesize:
				Matrix transform = new Matrix();
				if(mPreviewSurface != null) {
					float viewWidth = mPreviewSurface.getWidth();
					float viewHight = mPreviewSurface.getHeight();
					Toast.makeText(mContext, "preview size: " + viewWidth + "x" + viewHight, Toast.LENGTH_SHORT).show();
					transform.setScale(1.33f, 1.0f, 200.0f, 200.0f);
					mPreviewSurface.setTransform(transform);
				}
			    break;
			}
		}
	}
	
	private String viewVisableToString(int visible) {
		switch(visible) {
		case View.GONE:
			return "GONE";
			
		case View.VISIBLE:
			return "VISIBLE";
			
		case View.INVISIBLE:
			return "INVISIBLE";
			
		default:
			return "UNKNOWN";
		}
	}
    
	private String viewTransparentToString(float transparent) {
		return "transparent: " + transparent; 
	}
}
