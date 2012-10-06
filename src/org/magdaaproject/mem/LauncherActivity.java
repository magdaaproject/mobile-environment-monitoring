/*
 * Copyright (C) 2012 The MaGDAA Project
 *
 * This file is part of the MaGDAA MEM Software
 *
 * MaGDAA MEM Software is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.magdaaproject.mem;

import org.magdaaproject.utils.FileUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * 
 * The launcher activity for the MEM software, this is the default activity displayed
 * to the user when the application starts
 *
 */
public class LauncherActivity extends Activity implements OnClickListener {
	
	/*
	 * private class level constants
	 */
	private static final String sTag = "LauncherActivity";
	
	private static final int sNoExternalStorage = 0;

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        
        // populate the activity with the required text
        TextView mTextView = (TextView) findViewById(R.id.launcher_ui_lbl_about);
        
        CharSequence mStartText = getText(R.string.launcher_ui_lbl_about_start);
        
        // build the bullet steps
        SpannableString mStep01 = new SpannableString(getString(R.string.launcher_ui_lbl_step_01));
        mStep01.setSpan(new BulletSpan(Integer.parseInt(getText(R.string.ui_elem_bullet_span).toString())), 0, mStep01.length(), 0);
        
        SpannableString mStep02 = new SpannableString(getString(R.string.launcher_ui_lbl_step_02));
        mStep02.setSpan(new BulletSpan(Integer.parseInt(getText(R.string.ui_elem_bullet_span).toString())), 0, mStep02.length(), 0);
        
        SpannableString mStep03 = new SpannableString(getString(R.string.launcher_ui_lbl_step_03));
        mStep03.setSpan(new BulletSpan(Integer.parseInt(getText(R.string.ui_elem_bullet_span).toString())), 0, mStep03.length(), 0);
        
        SpannableString mStep04 = new SpannableString(getString(R.string.launcher_ui_lbl_step_04));
        mStep04.setSpan(new BulletSpan(Integer.parseInt(getText(R.string.ui_elem_bullet_span).toString())), 0, mStep04.length(), 0);
        
        CharSequence mFinishText = getText(R.string.launcher_ui_lbl_about_finish);
        
        // finalise the string and display it
        mTextView.setText(TextUtils.concat(mStartText, mStep01, mStep02, mStep03, mStep04, mFinishText));
        
        boolean mAllowStart = true;
        
        // check on external storage
        if(FileUtils.isExternalStorageAvailable() == false) {
        	mAllowStart = false;
        	showDialog(sNoExternalStorage);
        }
        
        //TODO check that Serval Mesh is installed
        //TODO check that Serval Mesh is running
        
        //setup the buttons
        Button mButton = (Button) findViewById(R.id.launcher_ui_btn_settings);
        mButton.setOnClickListener(this);
        
        mButton = (Button) findViewById(R.id.launcher_ui_btn_start);
        mButton.setOnClickListener(this);
        mButton.setEnabled(mAllowStart);
        
        mButton = (Button) findViewById(R.id.launcher_ui_btn_contact);
        mButton.setOnClickListener(this);
        
        // start the service for debugging
//        Intent mIntent = new Intent(this, org.magdaaproject.mem.services.CoreService.class);
//        startService(mIntent);
    }
    
    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
	@Override
	public void onClick(View view) {
		
		Intent mIntent;
		
		// determine which button was touched
		switch(view.getId()){
		case R.id.launcher_ui_btn_settings:
			mIntent = new Intent(this, org.magdaaproject.mem.SettingsActivity.class);
			startActivity(mIntent);
			break;
		case R.id.launcher_ui_btn_start:
			// show the readings activity
			mIntent = new Intent(this, org.magdaaproject.mem.ReadingsActivity.class);
			startActivity(mIntent);
			break;
		case R.id.launcher_ui_btn_contact:
			// show the contact information stuff
			contactUs();
			break;
		default:
			Log.w(sTag, "an unknown view fired an onClick event");
		}
		
	}
	
	/*
	 * callback method used to construct the required dialog
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {

		AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
		
		// determine which dialog to show
		switch(id) {
		case sNoExternalStorage:
			mBuilder.setMessage(R.string.launcher_ui_dialog_no_external_storage)
			.setCancelable(false)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			return mBuilder.create();
		default:
			return super.onCreateDialog(id);
		}
	}
	
	/*
	 * method to start the send an email process so that the user can contact us
	 */
	private void contactUs() {
		
		// send an email to us
		Intent mIntent = new Intent(Intent.ACTION_SEND);
		mIntent.setType("plain/text");
		mIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{getString(R.string.system_contact_email)});
		mIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.system_contact_email_subject));
		
		startActivity(Intent.createChooser(mIntent, getString(R.string.system_contact_email_chooser)));
	}
}
