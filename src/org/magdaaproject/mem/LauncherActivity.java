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

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
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

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
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
        
        //setup the buttons
        Button mButton = (Button) findViewById(R.id.launcher_ui_btn_settings);
        mButton.setOnClickListener(this);
        
        mButton = (Button) findViewById(R.id.launcher_ui_btn_start);
        mButton.setOnClickListener(this);
        
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
			break;
		case R.id.launcher_ui_btn_contact:
			// show the contact information stuff
			break;
		default:
			Log.w(sTag, "an unknown view fired an onClick event");
		}
		
	}
}
