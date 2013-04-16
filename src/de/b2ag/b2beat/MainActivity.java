/*
 * Copyright (C) 2013 Thomas Baag <b2beat@spam.b2ag.de>
 * 
 * This file is part of b2beat.
 * 
 * B2beat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * B2beat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package de.b2ag.b2beat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MainActivity extends Activity {
	private static final int BROWSER_RESULT = 1;

	private static final int SWIPE_MIN_DISTANCE = 200;
	private static final float SWIPE_MAX_RATIO_DIST_FACTOR = 3F;
	private static final float SWIPE_MAX_RATIO_VELO_FACTOR = 3F;
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;

	ArrayAdapter<String> m_adapter;

	static public class SamplerFragment extends ListFragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			return inflater.inflate(R.layout.sampler_layout, container);
		}
	}

	protected void setupPreferences() {

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (sharedPrefs.getString("sounds_browser_root_directory", "")
				.isEmpty()) {
			File soundsDirectory = getExternalFilesDir(null);
			if (soundsDirectory != null) {
				SharedPreferences.Editor settingsEditor = sharedPrefs.edit();
				settingsEditor.putString("sounds_browser_root_directory",
						soundsDirectory.getAbsolutePath());
				settingsEditor.commit();
				soundsDirectory.mkdirs();
			} else {
				Log.w("", "Couldn't find a suitable sounds library directory.");
			}
		}

	}

	protected void setupNativeLib() {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Set<String> fileset = sharedPrefs.getStringSet("sounds",
				new HashSet<String>());
		LinkedList<String> fileset_sorted = new LinkedList<String>(fileset);
		Collections.sort(fileset_sorted);
		String[] filelist = fileset_sorted.toArray(new String[fileset_sorted
				.size()]);
		TreeSet<String> newFilelist = new TreeSet<String>();
		m_adapter.clear();
		for (int i = 0; i < filelist.length; i++) {
			final String file = filelist[i];
			if (createUriAudioPlayer(i, "file://" + file)) {
				m_adapter.add(new File(file).getName().replaceFirst(
						"\\.[a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9]$", ""));
				newFilelist.add(file);
			}
		}
		sharedPrefs.edit().putStringSet("sounds", newFilelist).commit();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Remove title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Remove notification bar
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);

		setupPreferences();

		createEngine();

		FragmentManager fragmentManager = getFragmentManager();
		ListFragment listFragment = (ListFragment) fragmentManager
				.findFragmentById(R.id.sampler_fragment);

		m_adapter = new ArrayAdapter<String>(this, R.layout.sampler_rowlayout,
				R.id.label) {
			OnTouchListener touchListener = new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// gestureListener.onTouch(v, event);
					if (event.getActionMasked() == MotionEvent.ACTION_DOWN
							|| event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
						TextView textView = (TextView) v;
						final int position = (Integer) v.getTag();
						final float x = event.getX(event.getActionIndex());
						if (x < textView.getCompoundPaddingLeft()) {
							setPlayingAudioPlayer(position);
							return true;
						} else if (x > textView.getWidth()
								- textView.getCompoundPaddingRight()) {
							setStoppedAudioPlayer(position);
							return true;
						}
						// return true;
					}
					return false;
				}
			};

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				v.setTag(Integer.valueOf(position));
				v.setOnTouchListener(touchListener);
				return v;
			}
		};
		listFragment.setListAdapter(m_adapter);

		setupNativeLib();

		// try {
		// AssetManager assetManager = getAssets();
		// String[] filelist = assetManager.list("mysounds");

		// Gesture detection
		gestureDetector = new GestureDetector(this, new MyGestureDetector());
		gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() != MotionEvent.ACTION_CANCEL) {
					return gestureDetector.onTouchEvent(event);
				}
				return false;
			}
		};
		findViewById(android.R.id.list).setOnTouchListener(gestureListener);
		findViewById(android.R.id.empty).setOnTouchListener(gestureListener);
	}

	class MyGestureDetector extends SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {
				final float dy = Math.abs(e1.getY() - e2.getY());
				final float dx = e1.getX() - e2.getX();

				if (Math.abs(dx) < dy * SWIPE_MAX_RATIO_DIST_FACTOR)
					return false;
				if (Math.abs(velocityX) < Math.abs(velocityY) * SWIPE_MAX_RATIO_VELO_FACTOR)
					return false;
				if (e1.getPointerCount() > 1 || e1.getPointerCount() > 2)
					return false;
				if (dx > SWIPE_MIN_DISTANCE) {
					showPreferences();
					return true;
				} else if (-dx > SWIPE_MIN_DISTANCE) {
					showBrowser();
					return true;
				}
			} catch (Exception e) {
			}
			return false;
		}
	}

	protected void showBrowser() {
		Log.e("","AHHH??!?!");
		Intent intent = new Intent(getApplicationContext(),
				BrowserActivity.class);
		// intent.putExtra(activity.EXTRA_URL, link);
		startActivityForResult(intent, BROWSER_RESULT);
	}

	protected void showPreferences() {
		Intent intent = new Intent(getApplicationContext(),
				SettingsActivity.class);
		// intent.putExtra(activity.EXTRA_URL, link);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_settings: 
			showPreferences();
			return true;
		
		case R.id.action_browser: 
			showBrowser();
			return true;
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case (BROWSER_RESULT): {
			// if (resultCode == Activity.RESULT_OK) {
			shutdown(); // scheiss opensl es lib
			createEngine(); // scheiss opensl es lib
			setupNativeLib();
			// }
			break;
		}
		}
	}

	@Override
	protected void onNewIntent(android.content.Intent intent) {
		if (intent.getData().toString().equals("reload_songlist")) {
			shutdown(); // scheiss opensl es lib
			createEngine(); // scheiss opensl es lib
			setupNativeLib();
		}
	};

	/** Called when the activity is about to be destroyed. */
	@Override
	protected void onDestroy() {
		shutdown();
		super.onDestroy();
	}

	/** Native methods, implemented in jni folder */
	public static native void createEngine();

	public static native boolean createAssetAudioPlayer(
			AssetManager assetManager, int id, String filename);

	public static native boolean createUriAudioPlayer(int id, String uri);

	public static native void setPlayingAudioPlayer(int id);

	public static native void setStoppedAudioPlayer(int id);

	public static native void shutdown();

	/** Load jni .so on initialization */
	static {
		System.loadLibrary("b2beat");
	}

}
