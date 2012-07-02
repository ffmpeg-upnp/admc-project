package com.app.dlna.dmc.gui.customview;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.processor.model.YoutubeItem;
import com.app.dlna.dmc.utility.Cache;
import com.app.dlna.dmc.utility.Utility;

public class YoutubeItemArrayAdapter extends ArrayAdapter<YoutubeItem> {

	private LayoutInflater m_inflater;
	private static Bitmap BM_VIDEO = null;
	private static final int IMAGE_SIZE = 48;

	public YoutubeItemArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
		m_inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null || convertView instanceof ProgressBar) {
			convertView = m_inflater.inflate(R.layout.lvitem_youtube, null, false);
		}
		if (convertView.getTag() == null) {
			setViewHolder(convertView);
		}
		final YoutubeItem item = getItem(position);
		final ViewHolder holder = (ViewHolder) convertView.getTag();
		holder.name.setText(item.getTitle());
		holder.desc.setText(Utility.getTimeString(item.getDuration()));
		if (item.getAuthor() != null)
			holder.author.setText(item.getAuthor());
		HashMap<String, Bitmap> cache = Cache.getBitmapCache();
		String imageUrl = item.getThumbnail();
		holder.icon.setTag(imageUrl);
		if (cache.containsKey(imageUrl) && cache.get(imageUrl) != null) {
			holder.icon.setImageBitmap(cache.get(imageUrl));
		} else {
			holder.icon.setImageBitmap(BM_VIDEO);
			cache.put(imageUrl, BM_VIDEO);
			Utility.loadImageItemThumbnail(holder.icon, imageUrl, Cache.getBitmapCache(), IMAGE_SIZE);
		}
		return convertView;
	}

	public void setViewHolder(View view) {
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.name = (TextView) view.findViewById(R.id.name);
		viewHolder.desc = (TextView) view.findViewById(R.id.desc);
		viewHolder.icon = (ImageView) view.findViewById(R.id.icon);
		viewHolder.author = (TextView) view.findViewById(R.id.author);
		view.setTag(viewHolder);
	}

	private class ViewHolder {
		TextView desc;
		TextView name;
		ImageView icon;
		TextView author;
	}
}
