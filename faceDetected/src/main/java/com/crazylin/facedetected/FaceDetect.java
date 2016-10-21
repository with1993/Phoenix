package com.crazylin.facedetected;

import java.io.ByteArrayOutputStream;

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

public class FaceDetect {
	public interface Callback {
		void success(JSONObject result);
		
		void error(FaceppParseException exeception);
	}
	
	public static void detect(final Bitmap bm,final Callback callback) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					//向Face++服务器提交请求
System.out.println("向Face++服务器提交请求");
					HttpRequests requests = new HttpRequests(Constant.KEY, Constant.SECRET, true, true);
					Bitmap bmsmall = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight());
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bmsmall.compress(Bitmap.CompressFormat.JPEG, 50, stream);
					
					byte[] bytes = stream.toByteArray();
					PostParameters params = new PostParameters();
					params.setImg(bytes);
					JSONObject json = requests.detectionDetect(params);
					Log.e("Tag", json.toString());

					if (null != callback) {
						callback.success(json);
					}
				} catch (FaceppParseException e) {
					System.out.println(e.toString());
					e.printStackTrace();
					if(null != callback) {
						callback.error(e);
					}
				}
			}
		}).start();
	}
}
