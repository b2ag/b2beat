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

import de.b2ag.b2beat.MainActivity.MyGestureDetector;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class BrowserActivity extends Activity {

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (intent.getAction().equals("de.b2ag.b2beat.change_directory"))
		{
			BrowserFragment newBrowserFragment = new BrowserFragment();
			Bundle bundle = new Bundle();
			bundle.putString("directory", intent.getData().getPath());
			newBrowserFragment.setArguments(bundle);
			getFragmentManager()
			 .beginTransaction()
			 .replace(android.R.id.content, newBrowserFragment)
			 .addToBackStack(null)
			 .commit();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_main);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (!sharedPrefs.getString("sounds_browser_root_directory", "")
				.isEmpty()) {
			FragmentManager fragmentManager = getFragmentManager();
			BrowserFragment browserFragment = (BrowserFragment) fragmentManager
					.findFragmentByTag("browser");
			if (browserFragment == null) {
				browserFragment = new BrowserFragment();
			}
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			fragmentTransaction.add(android.R.id.content, browserFragment,
					"browser");
			fragmentTransaction.commit();
		}

	}

}
