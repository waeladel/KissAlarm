/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.carlorodriguez.alarmon;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Activity for displaying information about a receive App Invite invitation.  This activity
 * displays as a Dialog over the MainActivity and does not cover the full screen.
 */
public class DeepLinkActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = DeepLinkActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deep_link_activity);

        // Button click listener
        findViewById(R.id.button_ok).setOnClickListener(this);
    }

    // [START deep_link_on_start]
    @Override
    protected void onStart() {
        super.onStart();

        // Check if the intent contains an AppInvite and then process the referral information.
        Intent intent = getIntent();
        /*if (AppInviteReferral.hasReferral(intent)) {
            processReferralIntent(intent);
        }*/
        processReferralIntent(intent);
    }
    // [END deep_link_on_start]

    // [START process_referral_intent]InvitationId
    private void processReferralIntent(Intent intent) {
        // Extract referral information from the intent
        //String invitationId = AppInviteReferral.get(intent);
        //String deepLink = AppInviteReferral.getDeepLink(intent);

        // Display referral information
        // [START_EXCLUDE]
        //Log.d(TAG, "Found Referral: deepLink =" + ":" + deepLink);
        /*((TextView) findViewById(R.id.deep_link_text))
                .setText(getString(R.string.deep_link_fmt, deepLink));
        ((TextView) findViewById(R.id.invitation_id_text))
                .setText(getString(R.string.invitation_id_fmt, invitationId));*/
        ((TextView) findViewById(R.id.deep_link_text))
                .setText(getString(R.string.deep_link_congratulation));
        ((TextView) findViewById(R.id.invitation_id_text))
                .setText(getString(R.string.deep_link_welcome_text));
        // [END_EXCLUDE]
    }
    // [END process_referral_intent]


    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.button_ok) {
            /*Intent startIntent = new Intent(this, ActivityAlarmClock.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(startIntent);*/
            finish();
        }
    }
}
