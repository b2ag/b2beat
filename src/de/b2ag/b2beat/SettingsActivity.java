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

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {

	protected void onNewIntent(android.content.Intent intent) {
		if (intent.getData().toString().equals("reset_songlist")) {
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(getResources().getString(R.string.reset))
					.setMessage(getResources().getString(R.string.reset_confirm))
					.setPositiveButton(getResources().getString(android.R.string.yes),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									SharedPreferences sharedPrefs = PreferenceManager
											.getDefaultSharedPreferences(getBaseContext());
									sharedPrefs
											.edit()
											.putStringSet("sounds",
													new HashSet<String>())
											.commit();
									startActivity(new Intent("android.intent.action.MAIN").setClass(getBaseContext(),MainActivity.class).setData(Uri.parse("reload_songlist")).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
								}

							}).setNegativeButton(getResources().getString(android.R.string.no), null).show();
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			SettingsFragment settings = new SettingsFragment();
			settings.setArguments(getIntent().getExtras());
			getFragmentManager().beginTransaction()
					.add(android.R.id.content, settings).commit();
		}
	}
}
