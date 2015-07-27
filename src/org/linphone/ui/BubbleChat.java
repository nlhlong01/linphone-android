package org.linphone.ui;
/*
BubbleChat.java
Copyright (C) 2012  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map.Entry;

import org.linphone.R;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatMessage.State;
import org.linphone.core.LinphoneContent;
import org.linphone.mediastream.Log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

/**
 * @author Sylvain Berfini
 */
@SuppressLint("SimpleDateFormat")
public class BubbleChat {
	private static final HashMap<String, Integer> emoticons = new HashMap<String, Integer>();
	
	private RelativeLayout view;
	private ImageView statusView;
	private LinphoneChatMessage nativeMessage;
	private LinphoneChatMessage.LinphoneChatMessageListener fileTransferListener;
	private Context mContext;
	private static final int SIZE_MAX = 512;
	
	@SuppressLint("InflateParams") 
	public BubbleChat(final Context context, LinphoneChatMessage message, LinphoneChatMessage.LinphoneChatMessageListener listener) {
		if (message == null) {
			return;
		}
		nativeMessage = message;
		fileTransferListener = listener;
		mContext = context;
		
		view = new RelativeLayout(context);
		LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	
    	if (message.isOutgoing()) {
    		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    		view.setBackgroundResource(R.drawable.chat_bubble_outgoing);
    	}
    	else {
    		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    		view.setBackgroundResource(R.drawable.chat_bubble_incoming);
    	}

    	layoutParams.setMargins(10, 0, 10, 0);
    	
    	view.setId(message.getStorageId());	
    	view.setLayoutParams(layoutParams);
    	
		LinearLayout layout;
    	if (context.getResources().getBoolean(R.bool.display_time_aside)) {
	    	if (message.isOutgoing()) {
	    		layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.chat_bubble_alt_outgoing, null);
	    	} else {
	    		layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.chat_bubble_alt_incoming, null);
	    	}
    	} else {
    		if (message.isOutgoing()) {
	    		layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.chat_bubble_outgoing, null);
	    	} else {
	    		layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.chat_bubble_incoming, null);
	    	}
    	}

    	String externalBodyUrl = message.getExternalBodyUrl();
    	LinphoneContent fileTransferContent = message.getFileTransferInformation();
    	if (externalBodyUrl != null || fileTransferContent != null) {
			Button download = (Button) layout.findViewById(R.id.download);
	    	ImageView imageView = (ImageView) layout.findViewById(R.id.image);
	    	
	    	String appData = message.getAppData();
    		if (appData == null) {
    	    	download.setVisibility(View.VISIBLE);
    	    	download.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						v.setEnabled(false);
						ProgressBar spinner = (ProgressBar) view.findViewById(R.id.spinner);
						spinner.setVisibility(View.VISIBLE);
						v.setVisibility(View.GONE);

						nativeMessage.setListener(fileTransferListener);
						nativeMessage.downloadFile();
					}
				});
    		} else {
    	    	imageView.setVisibility(View.VISIBLE);
    	    	loadBitmap(appData, imageView);
    		}
    	} else {
	    	TextView msgView = (TextView) layout.findViewById(R.id.message);
	    	if (msgView != null) {
	        	Spanned text = null;
	        	String msg = message.getText();
	        	if (msg != null) {
	    	    	if (context.getResources().getBoolean(R.bool.emoticons_in_messages)) {
	    	    		text = getSmiledText(context, getTextWithHttpLinks(msg));
	    	    	} else {
	    	    		text = getTextWithHttpLinks(msg);
	    	    	}
	    	    	msgView.setText(text);
	    	    	msgView.setMovementMethod(LinkMovementMethod.getInstance());
	        		msgView.setVisibility(View.VISIBLE);
	        	}
	    	}
    	}
    	
    	TextView timeView = (TextView) layout.findViewById(R.id.time);
    	timeView.setText(timestampToHumanDate(context, message.getTime()));
    	
    	LinphoneChatMessage.State status = message.getStatus();
    	statusView = (ImageView) layout.findViewById(R.id.status);
    	if (statusView != null) {
    		if (status == LinphoneChatMessage.State.Delivered) {
    			statusView.setImageResource(R.drawable.chat_message_delivered);
    		} else if (status == LinphoneChatMessage.State.NotDelivered) {
    			statusView.setImageResource(R.drawable.chat_message_not_delivered);
    		} else {
    			statusView.setImageResource(R.drawable.chat_message_inprogress);
    		}
    	}
    	
    	view.addView(layout);
	}
	
	public void updateStatusView() {
		if (statusView == null) {
			return;
		}
		
		if (nativeMessage.getStatus() == LinphoneChatMessage.State.Delivered) {
			statusView.setImageResource(R.drawable.chat_message_delivered);
		} else if (nativeMessage.getStatus() == LinphoneChatMessage.State.NotDelivered) {
			statusView.setImageResource(R.drawable.chat_message_not_delivered);
		} else {
			statusView.setImageResource(R.drawable.chat_message_inprogress);
		}
		
		view.invalidate();
	}
	
	public View getView() {
		return view;
	}
	
	private String timestampToHumanDate(Context context, long timestamp) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timestamp);
			
			SimpleDateFormat dateFormat;
			if (isToday(cal)) {
				dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.today_date_format));
			} else {
				dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.messages_date_format));
			}
			
			return dateFormat.format(cal.getTime());
		} catch (NumberFormatException nfe) {
			return String.valueOf(timestamp);
		}
	}
	
	private boolean isToday(Calendar cal) {
        return isSameDay(cal, Calendar.getInstance());
    }
	
	private boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            return false;
        }
        
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }
	
	public static Spannable getSmiledText(Context context, Spanned spanned) {
		SpannableStringBuilder builder = new SpannableStringBuilder(spanned);
		String text = spanned.toString();

		for (Entry<String, Integer> entry : emoticons.entrySet()) {
			String key = entry.getKey();
			int indexOf = text.indexOf(key);
			while (indexOf >= 0) {
				int end = indexOf + key.length();
				builder.setSpan(new ImageSpan(context, entry.getValue()), indexOf, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				indexOf = text.indexOf(key, end);
			}
		}
		
		return builder;
	}
	
	public static Spanned getTextWithHttpLinks(String text) {
		if (text.contains("<")) {
			text = text.replace("<", "&lt;");
		}
		if (text.contains(">")) {
			text = text.replace(">", "&gt;");
		}
		if (text.contains("http://")) {
			int indexHttp = text.indexOf("http://");
			int indexFinHttp = text.indexOf(" ", indexHttp) == -1 ? text.length() : text.indexOf(" ", indexHttp);
			String link = text.substring(indexHttp, indexFinHttp);
			String linkWithoutScheme = link.replace("http://", "");
			text = text.replaceFirst(link, "<a href=\"" + link + "\">" + linkWithoutScheme + "</a>");
		}
		if (text.contains("https://")) {
			int indexHttp = text.indexOf("https://");
			int indexFinHttp = text.indexOf(" ", indexHttp) == -1 ? text.length() : text.indexOf(" ", indexHttp);
			String link = text.substring(indexHttp, indexFinHttp);
			String linkWithoutScheme = link.replace("https://", "");
			text = text.replaceFirst(link, "<a href=\"" + link + "\">" + linkWithoutScheme + "</a>");
		}
		
		return Html.fromHtml(text);
	}
	
	public String getTextMessage() {
		return nativeMessage.getText();
	}

	public State getStatus() {
		return nativeMessage.getStatus();
	}
	
	public LinphoneChatMessage getNativeMessageObject() {
		return nativeMessage;
	}
	
	public int getId() {
		return nativeMessage.getStorageId();
	}
	
	public void loadBitmap(String path, ImageView imageView) {
		if (cancelPotentialWork(path, imageView)) {
			BitmapWorkerTask task = new BitmapWorkerTask(imageView);
			Bitmap defaultBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.chat_photo_default);
			final AsyncBitmap asyncBitmap = new AsyncBitmap(mContext.getResources(), defaultBitmap, task);
			imageView.setImageDrawable(asyncBitmap);
			task.execute(path);
		}
    }
	
	private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
	    private final WeakReference<ImageView> imageViewReference;
	    public String path;

	    public BitmapWorkerTask(ImageView imageView) {
	    	path = null;
	        // Use a WeakReference to ensure the ImageView can be garbage collected
	        imageViewReference = new WeakReference<ImageView>(imageView);
	    }

	    // Decode image in background.
	    @Override
	    protected Bitmap doInBackground(String... params) {
	    	path = params[0];
	    	Bitmap bm = null;
	    	
	    	if (path.startsWith("content")) {
	    		try {
	    			bm = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), Uri.parse(path));
				} catch (FileNotFoundException e) {
					Log.e(e);
				} catch (IOException e) {
					Log.e(e);
				}
	    	} else {
	    		bm = BitmapFactory.decodeFile(path);
	    		path = "file://" + path;
	    	}
	    	
	    	if (bm != null) {
	    		if (bm.getWidth() >= bm.getHeight() && bm.getWidth() > SIZE_MAX) {
					bm = ThumbnailUtils.extractThumbnail(bm, SIZE_MAX, (SIZE_MAX * bm.getHeight()) / bm.getWidth());
				} else if (bm.getHeight() >= bm.getWidth() && bm.getHeight() > SIZE_MAX) {
					bm = ThumbnailUtils.extractThumbnail(bm, (SIZE_MAX * bm.getWidth()) / bm.getHeight(), SIZE_MAX);
				}
	    	}
	    	return bm;
	    }

	    // Once complete, see if ImageView is still around and set bitmap.
	    @Override
	    protected void onPostExecute(Bitmap bitmap) {
	    	if (isCancelled()) {
	            bitmap = null;
	        }

	        if (imageViewReference != null && bitmap != null) {
	            final ImageView imageView = imageViewReference.get();
	            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
	            if (this == bitmapWorkerTask && imageView != null) {
	                imageView.setImageBitmap(bitmap);
	                imageView.setTag(path);
			    	imageView.setOnClickListener(new OnClickListener() {
		    			@Override
		    			public void onClick(View v) {
		    				Intent intent = new Intent(Intent.ACTION_VIEW);
		    				intent.setDataAndType(Uri.parse((String)v.getTag()), "image/*");
		    				mContext.startActivity(intent);
		    			}
		    		});
	            }
		    }
	    }
	}
	
	static class AsyncBitmap extends BitmapDrawable {
	    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

	    public AsyncBitmap(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
	        super(res, bitmap);
	        bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
	    }

	    public BitmapWorkerTask getBitmapWorkerTask() {
	        return bitmapWorkerTaskReference.get();
	    }
	}
	
	public static boolean cancelPotentialWork(String path, ImageView imageView) {
	    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

	    if (bitmapWorkerTask != null) {
	        final String bitmapData = bitmapWorkerTask.path;
	        // If bitmapData is not yet set or it differs from the new data
	        if (bitmapData == null || bitmapData != path) {
	            // Cancel previous task
	            bitmapWorkerTask.cancel(true);
	        } else {
	            // The same work is already in progress
	            return false;
	        }
	    }
	    // No task associated with the ImageView, or an existing task was cancelled
	    return true;
	}
	
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
	   if (imageView != null) {
	       final Drawable drawable = imageView.getDrawable();
	       if (drawable instanceof AsyncBitmap) {
	           final AsyncBitmap asyncDrawable = (AsyncBitmap) drawable;
	           return asyncDrawable.getBitmapWorkerTask();
	       }
	    }
	    return null;
	}
}
