/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.temp.ServerStatus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    private String disk;

    public static class ServerViewHolder extends RecyclerView.ViewHolder {
        public TextView serverTextView;
        public TextView serverDetailTextView;
        public CircleImageView serverStatusImageView;

        public ServerViewHolder(View v) {
            super(v);
            serverTextView = (TextView) itemView.findViewById(R.id.serverTextView);
            serverDetailTextView = (TextView) itemView.findViewById(R.id.serverDetailTextView);
            serverStatusImageView = (CircleImageView) itemView.findViewById(R.id.serverStatusImageView);
        }
    }

    private static final String TAG = "MainActivity";
    public static final String SERVERS_CHILD = "ServerNames";
    private static final int REQUEST_INVITE = 1;
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private String mUsername;
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;

    private Button mSendButton;
    private RecyclerView mServerRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<ServerStats, ServerViewHolder> mFirebaseAdapter;
    private ProgressBar mProgressBar;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText mMessageEditText;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUsername = ANONYMOUS;

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mServerRecyclerView = (RecyclerView) findViewById(R.id.serverRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<ServerStats, ServerViewHolder>(
                        ServerStats.class,
                        R.layout.item_message,
                        ServerViewHolder.class,
                        mFirebaseDatabaseReference.child(SERVERS_CHILD)) {

            @Override
            protected void populateViewHolder(ServerViewHolder viewHolder, ServerStats serverStats, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                String name = serverStats.getServer();
                String powerState = serverStats.getPowerState();
                String vmhost = serverStats.getVmhost();
                int state = serverStats.getState();
                int diskPercent = serverStats.getDiskPercentage();
                String lastUpdate = serverStats.getLastUpdate();


                int image;
                if (state == 1) {
                    image = R.drawable.ic_online;
                } else {
                    image = R.drawable.ic_offline;
                }

                if (diskPercent >= 90) {
                    image = R.drawable.ic_error;
                }

                String strState = (state == 1) ? "Online": "Offline";

                String health = "Health: " + strState + ", " + powerState + ", " + vmhost + ", " + "Disk C: " + diskPercent + "%";
                viewHolder.serverTextView.setText(name);
                viewHolder.serverDetailTextView.setText(health);

                viewHolder.serverStatusImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                        image));

            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int serverStatsCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (serverStatsCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mServerRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mServerRecyclerView.setLayoutManager(mLinearLayoutManager);
        mServerRecyclerView.setAdapter(mFirebaseAdapter);

        // Initialize Firebase Measurement.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

}
