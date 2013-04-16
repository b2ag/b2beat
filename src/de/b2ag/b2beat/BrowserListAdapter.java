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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class BrowserListAdapter extends ArrayAdapter<File> {
	private File currentDir;
	// private List<File> list;
	private Activity context;
	private SharedPreferences sharedPrefs;
	private LinkedHashSet<String> selectedSounds = new LinkedHashSet<String>();

	public BrowserListAdapter(Activity context, File currentDir ) {
		super(context, R.layout.browser_rowlayout, R.id.browser_row_label);
		this.context = context;

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.currentDir = currentDir; 
		selectedSounds = new LinkedHashSet<String>(sharedPrefs.getStringSet(
				"sounds", selectedSounds));
		reload();
	}
	
	public void changeDirector(File newDir) {
		currentDir = newDir;
		clear();
		reload();
	}
	
	public void reload()
	{
		File[] files = currentDir.listFiles();
		List<File> files_sorted = Arrays.asList(files);
		Collections.sort(files_sorted);
		addAll(files_sorted);		
	}

	static class ViewHolder {
		protected int position;
		protected TextView text;
		protected CheckBox checkbox;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		if (convertView == null) {
			LayoutInflater inflator = context.getLayoutInflater();
			view = inflator.inflate(R.layout.browser_rowlayout, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.position = position;
			viewHolder.text = (TextView) view
					.findViewById(R.id.browser_row_label);
			viewHolder.checkbox = (CheckBox) view
					.findViewById(R.id.browser_row_check);
			viewHolder.checkbox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View buttonView) {
					CompoundButton button = (CompoundButton) buttonView;
					File file = (File) buttonView.getTag();
					if (file != null) {
						if (button.isChecked()) {
							if (selectedSounds.size() < 30) {
								selectedSounds.add(file.getAbsolutePath());
							} else {
								new AlertDialog.Builder(context)
										.setIcon(
												android.R.drawable.ic_dialog_alert)
										.setMessage(
												"Die Datei kann nicht hinzugefügt werden. Maximale Anzahl erreicht.")
										.show();
								button.setChecked(false);
								return;
							}
						} else {
							selectedSounds.remove(file.getAbsolutePath());
						}
						sharedPrefs.edit()
								.putStringSet("sounds", selectedSounds)
								.commit();
					}
				}
			});
			view.setTag(viewHolder);
			viewHolder.checkbox.setTag(getItem(position));
		} else {
			view = convertView;
			((ViewHolder) view.getTag()).checkbox.setTag(getItem(position));
		}
		ViewHolder holder = (ViewHolder) view.getTag();
		holder.text.setText(getItem(position).getName());
		File file = getItem(position);
		if (file.isDirectory()) {
			holder.checkbox.setVisibility(View.INVISIBLE);
			holder.checkbox.setEnabled(false);
		} else {
			holder.checkbox.setVisibility(View.VISIBLE);
			holder.checkbox.setEnabled(true);
			holder.checkbox.setChecked(selectedSounds.contains(file
					.getAbsolutePath()));
		}
		return view;
	}
}
