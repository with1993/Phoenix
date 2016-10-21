package com.crazylin.facedetected;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

public class MainActivity extends Activity implements OnClickListener {
	private static final int PIC_CODE = 0X110;
	private Button getImage_btn, image_detect_btn, capture_btn;
	private View loadingView;
	private ImageView photoView;
	private TextView num_detected;
	private String mcurrentPhotoPath;
	private Bitmap mphotoImage;
	private Handler mhandler;
	private Paint mPaint;
	private TextView infoText;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        setContentView(R.layout.activity_main);
    	
		initView();
		initEvent();
		
		initHandler();
		mPaint = new Paint();
		
    }

	private void initHandler() {
		mhandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) {
					case RESULT_OK:
						loadingView.setVisibility(View.GONE);
						JSONObject json = (JSONObject) msg.obj;
						prepareResultBitmap(json);
						photoView.setImageBitmap(mphotoImage);
						break;
					case RESULT_CANCELED:
						loadingView.setVisibility(View.GONE);
						String errorMessage = (String) msg.obj;
						
						if(TextUtils.isEmpty(errorMessage)) {
							num_detected.setText("Error");
						}else {
							num_detected.setText(errorMessage);
						}
						break;
				}
			}

			
		};
	}

	private void prepareResultBitmap(JSONObject json) {
		Bitmap bitmap = Bitmap.createBitmap(mphotoImage.getWidth(), mphotoImage.getHeight(), mphotoImage.getConfig());
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(mphotoImage, 0, 0, null);
		try {
			JSONArray faces = json.getJSONArray("face");
			int faceCount = faces.length();
			num_detected.setText("find : " + faceCount);
			
			for(int i=0; i<faceCount; i++) {
				//单独的face对象
				JSONObject face = faces.getJSONObject(i);
				
				int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
				String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");
				//String race = face.getJSONObject("attribute").getJSONObject("race").getString("value");
				
				Bitmap infoBitmap = BuildBitmapInfo(age, gender);
				
				int infoWidth = infoBitmap.getWidth();
				int infoHeight = infoBitmap.getHeight();
				
				if((bitmap.getWidth() < photoView.getWidth())&&(bitmap.getHeight() < photoView.getHeight())) {
					float ratio = Math.max(bitmap.getWidth()*1.0f/photoView.getWidth(), bitmap.getHeight()*1.0f/photoView.getHeight());
					infoBitmap = Bitmap.createScaledBitmap(infoBitmap, (int)(infoWidth * ratio), (int)(infoHeight * ratio), false);
				}
				
				
				JSONObject position = face.getJSONObject("position");
				float x = (float) position.getJSONObject("center").getDouble("x");
				float y = (float) position.getJSONObject("center").getDouble("y");
				
				float w = (float) position.getDouble("width");
				float h = (float) position.getDouble("height");
				
				x = x/100*bitmap.getWidth();
				y = y/100*bitmap.getHeight();
				
				w = w/100*bitmap.getWidth();
				h = h/100*bitmap.getHeight();
				
				mPaint.setColor(0xffffffff);
				canvas.drawLine(x - w/2, y - h/2, x - w/2, y + h/2, mPaint);
				canvas.drawLine(x - w/2, y - h/2, x + w/2, y - h/2, mPaint);
				canvas.drawLine(x + w/2, y - h/2, x + w/2, y + h/2, mPaint);
				canvas.drawLine(x - w/2, y + h/2, x + w/2, y + h/2, mPaint);
				
				canvas.drawBitmap(infoBitmap, x - infoBitmap.getWidth()/2, y - h/2 - infoBitmap.getHeight(), null);
				
				mphotoImage = bitmap;
				
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private Bitmap BuildBitmapInfo(int age, String gender) {
System.out.println(gender + " age:" + age);
		infoText.setText(gender + " age:" + age );
		infoText.setDrawingCacheEnabled(true);
		Bitmap bitmap =  Bitmap.createBitmap(infoText.getDrawingCache());
		infoText.destroyDrawingCache();
		return bitmap;
	}

	private void initEvent() {
		getImage_btn.setOnClickListener(this);
		image_detect_btn.setOnClickListener(this);
		capture_btn.setOnClickListener(this);
	}

	private void initView() {
		getImage_btn = (Button) findViewById(R.id.get_image);
		image_detect_btn = (Button) findViewById(R.id.detect_image);
		capture_btn = (Button) findViewById(R.id.capture_image);
		loadingView = findViewById(R.id.loading);
		photoView = (ImageView) findViewById(R.id.photo_image);
		num_detected = (TextView) findViewById(R.id.num_detected);
		infoText = (TextView) findViewById(R.id.info_text);
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch(requestCode) {
		//获取图片的路径
			case PIC_CODE:
				if(null != intent) {
					Uri uri = intent.getData();
					Cursor cursor = getContentResolver().query(	uri, null, null, null, null);
					cursor.moveToFirst();
					
					int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
					mcurrentPhotoPath = cursor.getString(idx);
					cursor.close();
					
					resizePhoto();
					
					photoView.setImageBitmap(mphotoImage);
					num_detected.setText("");
				}
				break;
			case 2:
				Bitmap bmPhoto = (Bitmap) intent.getExtras().get("data");  
				mcurrentPhotoPath = "capturing";
				mphotoImage = bmPhoto;
				photoView.setImageBitmap(mphotoImage);
				num_detected.setText("");
				break;
		}
	}
	
	
	/**
	 * 用来压缩图片的方法
	 */
	private void resizePhoto() {
		BitmapFactory.Options options = new Options();
		options.inJustDecodeBounds = true;
		
		BitmapFactory.decodeFile(mcurrentPhotoPath, options);
		
		double ratio = Math.max(options.outWidth*1.0d/1024 , options.outHeight*1.0d/1024);
		
		options.inSampleSize = (int)Math.ceil(ratio);
		
		options.inJustDecodeBounds = false;
		
		mphotoImage = BitmapFactory.decodeFile(mcurrentPhotoPath, options);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.get_image:
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("image/*");
				startActivityForResult(intent, PIC_CODE);
				break;
				
			case R.id.capture_image:
			    Intent intent2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);  
			    startActivityForResult(intent2, 2);  
				break;
			case R.id.detect_image:
				loadingView.setVisibility(View.VISIBLE);
				if(mcurrentPhotoPath!=null && !mcurrentPhotoPath.trim().equals("")) {
					if(mcurrentPhotoPath.equals("capturing")) {
						
					}else {
						resizePhoto();
					}
				}else {
					mphotoImage = BitmapFactory.decodeResource(getResources(), R.drawable.brother2);
				}
				
				FaceDetect.detect(mphotoImage, new FaceDetect.Callback() {
					@Override
					public void success(JSONObject result) {
						Message msg = Message.obtain(mhandler);
						msg.what = RESULT_OK;
						msg.obj = result;
						msg.sendToTarget();
					}
					
					@Override
					public void error(FaceppParseException exception) {
						Message msg = Message.obtain(mhandler);
						msg.what = RESULT_CANCELED;
						msg.obj = exception.getErrorMessage();
						msg.sendToTarget();
					}
				});
				break;
		}
		
	}

}
