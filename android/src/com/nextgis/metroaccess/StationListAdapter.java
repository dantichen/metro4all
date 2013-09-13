/******************************************************************************
 * Project:  Metro Access
 * Purpose:  Routing in subway for disabled.
 * Author:   Baryshnikov Dmitriy (aka Bishop), polimax@mail.ru
 ******************************************************************************
*   Copyright (C) 2013 NextGIS
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.metroaccess;

import java.io.File;
import java.util.ArrayList;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StationListAdapter extends BaseAdapter {
	private Context mContext;
	private ArrayList <StationItem> mStationList;
	private ImageButton showSchemaButton;
	
	public StationListAdapter(Context c, ArrayList <StationItem> list) {
		mContext = c;
		mStationList = list;
	}

	public int getCount() {
		return mStationList.size();
	}

	public Object getItem(int arg0) {
		return mStationList.get(arg0);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		// get the selected entry
		final StationItem entry = mStationList.get(position);

		// reference to convertView
		View v = convertView;

		// inflate new layout if null
		if(v == null) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			v = inflater.inflate(R.layout.station_row_layout, null);
		}

		// load controls from layout resources
		TextView tvName = (TextView)v.findViewById(R.id.tvStationName);
		tvName.setText(entry.GetName());
		
		
		if(entry.GetType() < 2){
			int[] anDetails = {800,37,1,1,37,410,890,21};
			AddDetailes(v, anDetails);
		}
		else{
			AddEmpty(v);
		}
		
		ImageView ivIcon = (ImageView)v.findViewById(R.id.ivIcon);
		// set data to display
		//File path = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
	    File imgFile = new File(MainActivity.msRDataPath + "/icons", "" + entry.GetLine() + "" + entry.GetType() + ".png");		
		Log.d(MainActivity.TAG, imgFile.getPath());
		if(imgFile.exists()){

		    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
		    ivIcon.setImageBitmap(myBitmap);
		}	

		
		showSchemaButton = (ImageButton) v.findViewById(R.id.show_sheme);
		final File schemaFile = new File(MainActivity.msRDataPath + "/schemes", "" + entry.GetId() + ".png");		
		if(schemaFile.exists()){
			showSchemaButton.setOnClickListener(new OnClickListener() {
 
				public void onClick(View arg0) {
				    try {
				    	Log.d(MainActivity.TAG, schemaFile.getPath());
				    	Intent intent = new Intent(Intent.ACTION_VIEW);
				    	intent.setDataAndType(Uri.fromFile(schemaFile),"image/png");
				    	mContext.startActivity(intent);
				    } catch (ActivityNotFoundException e) {
				        Log.e(MainActivity.TAG, "Call failed", e);
				    }
				}
	 
			});
		}
		else{
			showSchemaButton.setVisibility(View.INVISIBLE);
		}

		return v;

	}

	protected void AddEmpty(View v ){
		LinearLayout lDetailes = (LinearLayout)v.findViewById(R.id.lDetailes);
		lDetailes.removeAllViews();
		TextView tvx = new TextView(lDetailes.getContext());
		tvx.setText(" ");
		tvx.setTextAppearance(mContext, R.attr.textAppearanceSmall);
		tvx.setGravity(Gravity.CENTER);
		lDetailes.addView(tvx);
	}
	
	protected void AddDetailes(View v, int[] val ){
		LinearLayout lDetailes = (LinearLayout)v.findViewById(R.id.lDetailes);
		lDetailes.removeAllViews();
		for(int i = 0; i < 8; i++){
			if(val[i] > 0){
				ImageView ivx = new ImageView(lDetailes.getContext());
				switch(i){
				case 0:
					ivx.setImageResource(R.drawable.ic_5);
					break;
				case 1:
					ivx.setImageResource(R.drawable.ic_6);
					break;
				case 2:
					ivx.setImageResource(R.drawable.ic_7);
					break;				
				case 3:
					ivx.setImageResource(R.drawable.ic_8);
					break;				
				case 4:
					ivx.setImageResource(R.drawable.ic_9);
					break;				
				case 5:
					ivx.setImageResource(R.drawable.ic_10);
					break;				
				case 6:
					ivx.setImageResource(R.drawable.ic_11);
					break;
				case 7:
					ivx.setImageResource(R.drawable.ic_12);
					break;	
				}
			
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				lp.setMargins(2, 2, 2, 2);	
				lp.gravity = Gravity.CENTER;
				ivx.setLayoutParams(lp);
			
				lDetailes.addView(ivx);
			
				TextView tvx = new TextView(lDetailes.getContext());
				tvx.setText("" + val[i]);
				tvx.setTextAppearance(lDetailes.getContext(), R.attr.textAppearanceSmall);
				tvx.setGravity(Gravity.CENTER);
				lDetailes.addView(tvx);
			}
		}
	}
	
	public static class StationItem implements Parcelable{
		private String sName;
		private int nType;// 1 - src, 2 - dest, 3 - cross from, 4 - cross to, 5 - transit
		private int nId;
		
		public StationItem(int nId, String sName, int nType) {
			this.sName = sName;
			this.nId = nId;
			this.nType = nType;
		}		
		
		public String GetName(){
			return sName;
		}
		
		public int GetId(){
			return nId;
		}
		
		public int GetType(){
			return nType;
		}
		
		public int GetLine(){
			return MainActivity.mmoStations.get(nId).getLine();
		}
		
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

		public void writeToParcel(Parcel out, int flags) {
			out.writeString(sName);
			out.writeInt(nId);
			out.writeInt(nType);
		}	
		
		public static final Parcelable.Creator<StationItem> CREATOR
        = new Parcelable.Creator<StationItem>() {
		    public StationItem createFromParcel(Parcel in) {
		        return new StationItem(in);
		    }
		
		    public StationItem[] newArray(int size) {
		        return new StationItem[size];
		    }
		};
		
		private StationItem(Parcel in) {
			sName = in.readString();
			nId = in.readInt();
			nType = in.readInt();
		}

	}
}