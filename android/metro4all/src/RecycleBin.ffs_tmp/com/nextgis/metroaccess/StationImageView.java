/******************************************************************************
 * Project:  Metro Access
 * Purpose:  Routing in subway for disabled.
 * Authors:  Baryshnikov Dmitriy aka Bishop (polimax@mail.ru), Stanislav Petriakov
 ******************************************************************************
*   Copyright (C) 2013-2015 NextGIS
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

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nextgis.metroaccess.data.PortalItem;
import com.nextgis.metroaccess.data.StationItem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import static com.nextgis.metroaccess.Constants.BUNDLE_STATIONID_KEY;
import static com.nextgis.metroaccess.Constants.PARAM_PORTAL_DIRECTION;
import static com.nextgis.metroaccess.Constants.PARAM_ROOT_ACTIVITY;
import static com.nextgis.metroaccess.Constants.PARAM_SCHEME_PATH;
import static com.nextgis.metroaccess.Constants.PORTAL_MAP_RESULT;

public class StationImageView extends SherlockActivity {
	private WebView mWebView;
    private Bundle bundle;

	private String msPath;
    private boolean isCrossReference = false;
    private boolean mIsRootActivity, isForLegend;
    RecyclerView rvPortals;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.station_image_view);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, getResources().getConfiguration().orientation == Configuration
                .ORIENTATION_PORTRAIT ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL, false);
        rvPortals = ((RecyclerView) findViewById(R.id.rvPortals));
        rvPortals.setLayoutManager(mLayoutManager);

       	getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // load view
        mWebView = (WebView) findViewById(R.id.webView);
        // (*) this line make uses of the Zoom control
        mWebView.getSettings().setBuiltInZoomControls(true);
//        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);

        bundle = getIntent().getExtras();
        if(bundle!= null) {
            // PARAM_ROOT_ACTIVITY - determine is it called from StationMapActivity or StationExpandableListAdapter
            mIsRootActivity = bundle.getBoolean(PARAM_ROOT_ACTIVITY);
            isCrossReference = bundle.containsKey(PARAM_ROOT_ACTIVITY); // if PARAM_ROOT_ACTIVITY not contains, it called from another
            msPath = bundle.getString(PARAM_SCHEME_PATH);

            StationItem station = MainActivity.GetGraph().GetStation(bundle.getInt(BUNDLE_STATIONID_KEY, 0));
            String title = station == null ? getString(R.string.sFileNotFound) : String.format(getString(R.string.sSchema), station.GetName());
            setTitle(title);

            if (station != null && station.GetPortalsCount() > 0) {
                RVPortalAdapter adapter = new RVPortalAdapter(station.GetPortals(bundle.getBoolean(PARAM_PORTAL_DIRECTION, true)));
                rvPortals.setAdapter(adapter);
                rvPortals.setHasFixedSize(true);
                rvPortals.setItemAnimator(new DefaultItemAnimator());
                rvPortals.setVisibility(View.VISIBLE);
            }
        } else {
            isForLegend = true;
            setTitle(R.string.sLegend);

            Tracker t = ((Analytics) getApplication()).getTracker();
            t.setScreenName(Analytics.SCREEN_LAYOUT + " " + Analytics.LEGEND);
            t.send(new HitBuilders.AppViewBuilder().build());
        }

        mWebView.post(new Runnable() {
            @Override
            public void run() {
                if (!loadImage()) {
                    mWebView.setVisibility(View.GONE);
                    findViewById(R.id.tvLayoutError).setVisibility(View.VISIBLE);
                }
            }
        });
    }
	
	protected boolean loadImage(){
        Bitmap overlaidImage;

        if (isForLegend) {
            overlaidImage = BitmapFactory.decodeResource(getResources(), R.raw.schemes_legend);
            mWebView.clearCache(true);
        } else {
            File f = new File(msPath);
            String sParent = f.getParent();
            String sName = f.getName();
//            f = new File(f.getParent(), "/base/" + sName);

            if (!f.exists()) {
                return false;
            } else {
                Bitmap baseBitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
                overlaidImage = baseBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(overlaidImage);

                baseBitmap = BitmapFactory.decodeFile(sParent + "/titles/" + sName);    // TODO localization
                overlayBitmap(canvas, baseBitmap);

                if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferencesActivity.KEY_PREF_HAVE_LIMITS, false)) {
                    baseBitmap = BitmapFactory.decodeFile(sParent + "/interchanges/" + sName);
                    overlayBitmap(canvas, baseBitmap);
                }

                baseBitmap = BitmapFactory.decodeFile(sParent + "/info/" + sName);
                overlayBitmap(canvas, baseBitmap);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        overlaidImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] byteArray = baos.toByteArray();
        String imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

//        DisplayMetrics metrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        double deviceRatio = 1.0 * metrics.heightPixels / metrics.widthPixels;
        double deviceRatio = 1.0 * mWebView.getHeight() / mWebView.getWidth();
        double imageRatio = 1.0 * overlaidImage.getHeight() / overlaidImage.getWidth();
        String fix = deviceRatio > imageRatio ? "width=\"100%\" height=\"auto\"" : "width=\"auto\" height=\"100%\"";

        // background-color: rgba(255, 255, 255, 0.01); alpha channel is a fix for showing image on some webkit versions
        String sCmd = "<html><center><img style='background-color:rgba(255,255,255,0.01);position:absolute;margin:auto;top:0;left:0;right:0;bottom:0;" +
                "max-width:100%;max-height:100%;' " + fix + " src='data:image/png;base64," + imageBase64 + "'></center></html>";

        mWebView.loadData(sCmd, "text/html", "utf-8");

        return true;
	}

    private void overlayBitmap(Canvas canvas, Bitmap bitmap) {
        if (bitmap != null)
            canvas.drawBitmap(bitmap, 0, 0, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater infl = getSupportMenuInflater();
        infl.inflate(R.menu.menu_station_list, menu);
        menu.findItem(R.id.btn_legend).setEnabled(!isForLegend).setVisible(!isForLegend);
        menu.findItem(R.id.btn_map).setEnabled(!isForLegend && isCrossReference).setVisible(!isForLegend && isCrossReference);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT, Analytics.BACK, Analytics.SCREEN_LAYOUT);

                // app icon in action bar clicked; go home
                //Intent intent = new Intent(this, StationListView.class);
                //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                //startActivity(intent);
            	finish();
                return true;
            case R.id.btn_map:
                ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT, Analytics.BTN_MAP, Analytics.ACTION_BAR);

                if (mIsRootActivity) {
                    Intent intentMap = new Intent(this, StationMapActivity.class);
                    intentMap.putExtras(bundle);
                    intentMap.putExtra(PARAM_ROOT_ACTIVITY, false);
                    startActivityForResult(intentMap, PORTAL_MAP_RESULT);
                } else
                    finish();
                return true;
            case R.id.btn_legend:
                ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT, Analytics.LEGEND, Analytics.ACTION_BAR);
                onLegendClick();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        ((Analytics) getApplication()).addEvent(Analytics.SCREEN_LAYOUT, Analytics.BACK, Analytics.SCREEN_LAYOUT);

        super.onBackPressed();
    }

    public void onLegendClick() {
        Intent intentView = new Intent(this, StationImageView.class);
        startActivity(intentView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PORTAL_MAP_RESULT:
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK, data);
                    finish();
                }
                break;
            default:
                break;
        }
    }

    class RVPortalAdapter extends RecyclerView.Adapter<ViewHolder> implements ViewHolder.IViewHolderClick {
        private List<PortalItem> mPortals;

        public RVPortalAdapter (List<PortalItem> portals) {
            mPortals = portals;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            final View sView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.single_portal_item, viewGroup, false);
            return new ViewHolder(sView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int i) {
            viewHolder.setOnClickListener(this);
            viewHolder.setName(mPortals.get(i).GetReadableMeetCode());
        }

        @Override
        public int getItemCount() {
            return mPortals.size();
        }

        @Override
        public void onItemClick(View caller, int position) {
            Toast.makeText(getApplicationContext(), mPortals.get(position).GetReadableMeetCode(), Toast.LENGTH_SHORT).show();
        }
    }
}