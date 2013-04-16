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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import de.b2ag.b2beat.BrowserListAdapter.ViewHolder;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class BrowserFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}
		Bundle bundle=getArguments();
		File directory;
		if (bundle != null && bundle.containsKey("directory")) 
		{
			directory = new File(bundle.getString("directory"));
		}
		else
		{
			SharedPreferences sharedPrefs = PreferenceManager
					.getDefaultSharedPreferences(getActivity());
			directory = new File(sharedPrefs.getString(
					"sounds_browser_root_directory", ""));
		}
		
		View view = inflater.inflate(R.layout.browser_layout, container, false);
		ListView listView = (ListView) view.findViewById(R.id.browser_list);
		BrowserListAdapter adapter = new BrowserListAdapter(getActivity(),directory);
		listView.setAdapter(adapter);
		adapter.changeDirector(directory);

		// register directory clicks
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				File file = (File) adapter.getItemAtPosition(position);
				if (file != null) {
					if (file.isDirectory()) {
						// mAdapter.changeDirector(file);
						startActivity(new Intent(
								"de.b2ag.b2beat.change_directory")
								.setClass(getActivity(), BrowserActivity.class)
								.setData(Uri.parse(file.toURI().toString()))
								.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
					}
				}

			}

		});

		return view;
	}

}
