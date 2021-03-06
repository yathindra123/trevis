package com.trevis.trevis.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.trevis.trevis.R;


import android.app.ProgressDialog;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.trevis.trevis.RequestType;
import com.trevis.trevis.modal.Friend;
import com.trevis.trevis.modal.FriendRequest;
import com.trevis.trevis.modal.Friends;
import com.trevis.trevis.modal.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mProfileName, mProfileStatus, mProfileFriendsCount;
    private Button mProfileSendReqBtn, mDeclineBtn;

    private DatabaseReference mUsersDatabase;

    private ProgressDialog mProgressDialog;

    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mNotificationDatabase;

    private DatabaseReference mRootRef;

    private FirebaseUser mCurrent_user;

    private String mCurrent_state;
    private String device_token;
    List<FriendRequest> requestList;
    FriendRequest friendRequest;

    String selected_uid;

    RequestQueue queue;
    public static final String KEY_FCM_SENDER_ID = "sender_id";
    public static final String KEY_FCM_TEXT = "text";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //Get profile user details using extra
        final String profile_name = getIntent().getStringExtra("tappedUserName");
        final String profile_status = getIntent().getStringExtra("tappedUserStatus");
        final String profile_uid = getIntent().getStringExtra("tappedUserUID");
        final String profile_token = getIntent().getStringExtra("tappedUserDevToken");
        final String profile_image = getIntent().getStringExtra("tappedUserImage");

        selected_uid = profile_uid;

        mCurrent_user = FirebaseAuth.getInstance().getCurrentUser();

        mProfileImage = (ImageView) findViewById(R.id.profile_image);
        mProfileName = (TextView) findViewById(R.id.profile_name);
        mProfileStatus = (TextView) findViewById(R.id.profile_status);
        mProfileFriendsCount = (TextView) findViewById(R.id.profile_totalFriends);
        mProfileSendReqBtn = (Button) findViewById(R.id.profile_send_req_btn);
        mDeclineBtn = (Button) findViewById(R.id.profile_decline_btn);

        //Default state
        mCurrent_state = "not_friends";

        mDeclineBtn.setVisibility(View.INVISIBLE);
        mDeclineBtn.setEnabled(false);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User Data");
        mProgressDialog.setMessage("Please wait while we load the user data.");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        //Set items on profile
        mProfileName.setText(profile_name);
        mProfileStatus.setText(profile_status);
        Picasso.get().load(profile_image).placeholder(R.drawable.default_avatar).into(mProfileImage);


        // If own account
        if(mCurrent_user.getUid().equals(profile_uid)){
            //Remove decline button
            mDeclineBtn.setEnabled(false);
            mDeclineBtn.setVisibility(View.INVISIBLE);
            //Remove req send button
            mProfileSendReqBtn.setEnabled(false);
            mProfileSendReqBtn.setVisibility(View.INVISIBLE);
        }

        //Send Friend Requests Database by current user's UID

        //Check whether selected user id is in the friend requests set

        //is a friend?
        isFriend(mCurrent_user.getUid(),selected_uid);

        //Req send by me
        getReqType("from", mCurrent_user.getUid(), selected_uid);



        addListeners();
    }

    boolean checkedInTo = false;
    public void calledWhenFrmStateFalse(){

        getReqType("from", selected_uid , mCurrent_user.getUid());
        checkedInTo = true;

        mProgressDialog.dismiss();
    }

    public void loadUI(){

        if(isAFriend == false){
            isFriend(mCurrent_user.getUid(),selected_uid);
        }

        if (friendRequest.getFrom().equals(mCurrent_user.getUid())){
            //I have sent
            //Cancel request

            mCurrent_state = "req_sent";
            mProfileSendReqBtn.setText("Cancel Friend Request");

            //Hide decline btn
            mDeclineBtn.setVisibility(View.INVISIBLE);
            mDeclineBtn.setEnabled(false);
        }
        else if (friendRequest.getTo().equals(mCurrent_user.getUid())){
            //I got a request
            //Should confirm or decline
            mCurrent_state = "req_received";
            mProfileSendReqBtn.setText("Accept Friend Request");

            //Display decline btn
            mDeclineBtn.setVisibility(View.VISIBLE);
            mDeclineBtn.setEnabled(true);
        }
        else if (isAFriend){
            /*
            * Following not working as expected*/
            mCurrent_state = "friends";
            mProfileSendReqBtn.setText("Unfriend this Person");

            mDeclineBtn.setVisibility(View.INVISIBLE);
            mDeclineBtn.setEnabled(false);
        }

//                mFriendReqDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//
//                        //Check whether selected user id is in the friend requests set
//                        if(dataSnapshot.hasChild(user_id)){
//
//                            //Get req type
//                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();
//
//                            if(req_type.equals("received")){
//                                mCurrent_state = "req_received";
//                                mProfileSendReqBtn.setText("Accept Friend Request");
//
//                                //Display decline btn
//                                mDeclineBtn.setVisibility(View.VISIBLE);
//                                mDeclineBtn.setEnabled(true);
//                            }
//                            else if(req_type.equals("sent")) {
//                                mCurrent_state = "req_sent";
//                                mProfileSendReqBtn.setText("Cancel Friend Request");
//
//                                //Hide decline btn
//                                mDeclineBtn.setVisibility(View.INVISIBLE);
//                                mDeclineBtn.setEnabled(false);
//                            }
//
//                            mProgressDialog.dismiss();
//
//                        } else {
//
//                            mFriendDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
//                                @Override
//                                public void onDataChange(DataSnapshot dataSnapshot) {
//
//                                    if(dataSnapshot.hasChild(user_id)){
//
//                                        mCurrent_state = "friends";
//                                        mProfileSendReqBtn.setText("Unfriend this Person");
//
//                                        mDeclineBtn.setVisibility(View.INVISIBLE);
//                                        mDeclineBtn.setEnabled(false);
//
//                                    }
//                                    mProgressDialog.dismiss();
//                                }
//
//                                @Override
//                                public void onCancelled(DatabaseError databaseError) {
//                                    mProgressDialog.dismiss();
//                                }
//                            });
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//
//                    }
//                });
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
//

        mProgressDialog.dismiss();

//        //Should change below code
//        mUsersDatabase.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//
//                //Set properties of selected user
//
//                String display_name = dataSnapshot.child("name").getValue().toString();
//                String status = dataSnapshot.child("status").getValue().toString();
//                String image = dataSnapshot.child("image").getValue().toString();
//                device_token = dataSnapshot.child("device_token").getValue().toString();
//
//                mProfileName.setText(display_name);
//                mProfileStatus.setText(status);
//
//                Picasso.get().load(image).placeholder(R.drawable.default_avatar).into(mProfileImage);
//
//                // If own account
//                if(mCurrent_user.getUid().equals(user_id)){
//                    //Remove decline button
//                    mDeclineBtn.setEnabled(false);
//                    mDeclineBtn.setVisibility(View.INVISIBLE);
//                    //Remove req send button
//                    mProfileSendReqBtn.setEnabled(false);
//                    mProfileSendReqBtn.setVisibility(View.INVISIBLE);
//                }
//
//
//                //Send Friend Requests Database by current user's UID
//                mFriendReqDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//
//                        //Check whether selected user id is in the friend requests set
//                        if(dataSnapshot.hasChild(user_id)){
//
//                            //Get req type
//                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();
//
//                            if(req_type.equals("received")){
//                                mCurrent_state = "req_received";
//                                mProfileSendReqBtn.setText("Accept Friend Request");
//
//                                //Display decline btn
//                                mDeclineBtn.setVisibility(View.VISIBLE);
//                                mDeclineBtn.setEnabled(true);
//                            }
//                            else if(req_type.equals("sent")) {
//                                mCurrent_state = "req_sent";
//                                mProfileSendReqBtn.setText("Cancel Friend Request");
//
//                                //Hide decline btn
//                                mDeclineBtn.setVisibility(View.INVISIBLE);
//                                mDeclineBtn.setEnabled(false);
//                            }
//
//                            mProgressDialog.dismiss();
//
//                        } else {
//
//                            mFriendDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
//                                @Override
//                                public void onDataChange(DataSnapshot dataSnapshot) {
//
//                                    if(dataSnapshot.hasChild(user_id)){
//
//                                        mCurrent_state = "friends";
//                                        mProfileSendReqBtn.setText("Unfriend this Person");
//
//                                        mDeclineBtn.setVisibility(View.INVISIBLE);
//                                        mDeclineBtn.setEnabled(false);
//
//                                    }
//                                    mProgressDialog.dismiss();
//                                }
//
//                                @Override
//                                public void onCancelled(DatabaseError databaseError) {
//                                    mProgressDialog.dismiss();
//                                }
//                            });
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//
//                    }
//                });
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
//
//        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                //Disable After clicking on that
//                mProfileSendReqBtn.setEnabled(false);
//
//                // If not friends
//                if(mCurrent_state.equals("not_friends")){
//
//                    //push() will create a push id, means a random id
//                    DatabaseReference newNotificationref = mRootRef.child("notifications").child(user_id).push();
//                    String newNotificationId = newNotificationref.getKey();
//
//                    //HashMap for Notification data
//                    HashMap<String, String> notificationData = new HashMap<>();
//                    notificationData.put("from", mCurrent_user.getUid());
//                    notificationData.put("type", "request");
//
////                    mUsersDatabase.child(user_id).addValueEventListener(new ValueEventListener() {
////                        @Override
////                        public void onDataChange(DataSnapshot dataSnapshot) {
////                            String device_token = dataSnapshot.child("device_token").getValue().toString();
////                            Log.d("Token ekaaa",device_token);
////                        }
////
////                        @Override
////                        public void onCancelled(DatabaseError databaseError) {
////
////                        }
////                    });
//
//
//
//
//
//                    mNotificationDatabase.child(user_id).push().setValue(notificationData).addOnCompleteListener(new OnCompleteListener<Void>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Void> task) {
//
//                            sendNotification();
//                        }
//                    });
//                    //Map for requests
//                    Map requestMap = new HashMap();
//                    // Adding values by dividing forward slashes
//                    requestMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id + "/request_type", "sent");
//                    requestMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid() + "/request_type", "received");
//                    requestMap.put("notifications/" + user_id + "/" + newNotificationId, notificationData);
//
//                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//                            if(databaseError != null){
//                                Toast.makeText(ProfileActivity.this, "There was some error in sending request", Toast.LENGTH_SHORT).show();
//                            }
//                            else {
//                                mCurrent_state = "req_sent";
//                                mProfileSendReqBtn.setText("Cancel Friend Request");
//                            }
//
//                            mProfileSendReqBtn.setEnabled(true);
//                        }
//                    });
//
//                }
//
//
//                //Cancel requests state
//                if(mCurrent_state.equals("req_sent")){
//
//                    mFriendReqDatabase.child(mCurrent_user.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//
//                            // Remove values in requests
//                            mFriendReqDatabase.child(user_id).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
//                                @Override
//                                public void onSuccess(Void aVoid) {
//
//                                    mProfileSendReqBtn.setEnabled(true);
//                                    mCurrent_state = "not_friends";
//                                    mProfileSendReqBtn.setText("Send Friend Request");
//
//                                    mDeclineBtn.setVisibility(View.INVISIBLE);
//                                    mDeclineBtn.setEnabled(false);
//
//
//                                }
//                            });
//
//                        }
//                    });
//
//                }
//
//
//                //Request Received state
//                if(mCurrent_state.equals("req_received")){
//
//                    //Get date
//                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());
//
//                    Map friendsMap = new HashMap();
//                    friendsMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id + "/date", currentDate);
//                    friendsMap.put("Friends/" + user_id + "/"  + mCurrent_user.getUid() + "/date", currentDate);
//
//                    friendsMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id, null);
//                    friendsMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid(), null);
//
//
//                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//
//                            if(databaseError == null){
//
//                                mProfileSendReqBtn.setEnabled(true);
//                                mCurrent_state = "friends";
//                                mProfileSendReqBtn.setText("Unfriend this Person");
//
//                                mDeclineBtn.setVisibility(View.INVISIBLE);
//                                mDeclineBtn.setEnabled(false);
//
//                            } else {
//
//                                String error = databaseError.getMessage();
//                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    });
//
//                }
//
//
//                //Unfriend state
//                if(mCurrent_state.equals("friends")){
//
//                    Map unfriendMap = new HashMap();
//                    unfriendMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id, null);
//                    unfriendMap.put("Friends/" + user_id + "/" + mCurrent_user.getUid(), null);
//
//                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//
//                            if(databaseError == null){
//
//                                mCurrent_state = "not_friends";
//                                mProfileSendReqBtn.setText("Send Friend Request");
//
//                                mDeclineBtn.setVisibility(View.INVISIBLE);
//                                mDeclineBtn.setEnabled(false);
//
//                            } else {
//
//                                String error = databaseError.getMessage();
//
//                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
//
//
//                            }
//
//                            mProfileSendReqBtn.setEnabled(true);
//
//                        }
//                    });
//
//                }
//
//
//            }
//        });
//
//        //Decline Button listener
//        mDeclineBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //create a map
//                Map friendsDeclineMap = new HashMap();
//                friendsDeclineMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id, null);
//                friendsDeclineMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid(), null);
//
//                mRootRef.updateChildren(friendsDeclineMap, new DatabaseReference.CompletionListener() {
//                    @Override
//                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//                        if(databaseError == null){
//                            mProfileSendReqBtn.setEnabled(true);
//                            mCurrent_state = "not_friends";
//                            mProfileSendReqBtn.setText("Send Friend Request");
//                            mDeclineBtn.setVisibility(View.INVISIBLE);
//                            mDeclineBtn.setEnabled(false);
//                        }
//                        else {
//
//                            String error = databaseError.getMessage();
//                            Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
//            }
//        });
    }




    //new
    public void addListeners(){
        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Disable After clicking on that
                mProfileSendReqBtn.setEnabled(false);

                // If not friends
                if(mCurrent_state.equals("not_friends")){

                    //push() will create a push id, means a random id
//                    DatabaseReference newNotificationref = mRootRef.child("notifications").child(user_id).push();
//                    String newNotificationId = newNotificationref.getKey();
//
//                    //HashMap for Notification data
//                    HashMap<String, String> notificationData = new HashMap<>();
//                    notificationData.put("from", mCurrent_user.getUid());
//                    notificationData.put("type", "request");

//                    mUsersDatabase.child(user_id).addValueEventListener(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(DataSnapshot dataSnapshot) {
//                            String device_token = dataSnapshot.child("device_token").getValue().toString();
//                            Log.d("Token ekaaa",device_token);
//                        }
//
//                        @Override
//                        public void onCancelled(DatabaseError databaseError) {
//
//                        }
//                    });


                    sendRequest(mCurrent_user.getUid() , selected_uid);

                    //if success
                    mCurrent_state = "req_sent";
                    mProfileSendReqBtn.setText("Cancel Friend Request");
                    mProfileSendReqBtn.setEnabled(true);



//                    mNotificationDatabase.child(user_id).push().setValue(notificationData).addOnCompleteListener(new OnCompleteListener<Void>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Void> task) {
//
//                            sendNotification();
//                        }
//                    });
//                    //Map for requests
//                    Map requestMap = new HashMap();
//                    // Adding values by dividing forward slashes
//                    requestMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id + "/request_type", "sent");
//                    requestMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid() + "/request_type", "received");
//                    requestMap.put("notifications/" + user_id + "/" + newNotificationId, notificationData);

//                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//                            if(databaseError != null){
//                                Toast.makeText(ProfileActivity.this, "There was some error in sending request", Toast.LENGTH_SHORT).show();
//                            }
//                            else {
//                                mCurrent_state = "req_sent";
//                                mProfileSendReqBtn.setText("Cancel Friend Request");
//                            }
//
//                            mProfileSendReqBtn.setEnabled(true);
//                        }
//                    });
                }

                //Cancel requests state
                else if(mCurrent_state.equals("req_sent")){

                    deleteRequest(mCurrent_user.getUid() , selected_uid);

                    //if success
                    mProfileSendReqBtn.setEnabled(true);
                    mCurrent_state = "not_friends";
                    mProfileSendReqBtn.setText("Send Friend Request");

                    mDeclineBtn.setVisibility(View.INVISIBLE);
                    mDeclineBtn.setEnabled(false);

                }


                //Request Received state
                else if(mCurrent_state.equals("req_received")){

                    //save
                    saveNewFriend(mCurrent_user.getUid(), selected_uid);
                    saveNewFriend(selected_uid , mCurrent_user.getUid());

                    deleteRequest(mCurrent_user.getUid() , selected_uid);
                    deleteRequest(selected_uid , mCurrent_user.getUid());

                    //If success
                    mProfileSendReqBtn.setEnabled(true);
                    mCurrent_state = "friends";
                    mProfileSendReqBtn.setText("Unfriend this Person");

                    mDeclineBtn.setVisibility(View.INVISIBLE);
                    mDeclineBtn.setEnabled(false);






                    //Get date
                    //final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

//                    Map friendsMap = new HashMap();
//                    friendsMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id + "/date", currentDate);
//                    friendsMap.put("Friends/" + user_id + "/"  + mCurrent_user.getUid() + "/date", currentDate);
//
//                    friendsMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id, null);
//                    friendsMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid(), null);


//                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//
//                            if(databaseError == null){
//
//                                mProfileSendReqBtn.setEnabled(true);
//                                mCurrent_state = "friends";
//                                mProfileSendReqBtn.setText("Unfriend this Person");
//
//                                mDeclineBtn.setVisibility(View.INVISIBLE);
//                                mDeclineBtn.setEnabled(false);
//
//                            } else {
//
//                                String error = databaseError.getMessage();
//                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    });
                }


                //Unfriend state
//                if(mCurrent_state.equals("friends")){
//
//                    Map unfriendMap = new HashMap();
//                    unfriendMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id, null);
//                    unfriendMap.put("Friends/" + user_id + "/" + mCurrent_user.getUid(), null);
//
//                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//
//                            if(databaseError == null){
//
//                                mCurrent_state = "not_friends";
//                                mProfileSendReqBtn.setText("Send Friend Request");
//
//                                mDeclineBtn.setVisibility(View.INVISIBLE);
//                                mDeclineBtn.setEnabled(false);
//
//                            } else {
//
//                                String error = databaseError.getMessage();
//
//                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
//
//
//                            }
//
//                            mProfileSendReqBtn.setEnabled(true);
//
//                        }
//                    });
//
//                }


            }
        });
//
//        //Decline Button listener
        mDeclineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //create a map

                deleteRequest(selected_uid ,mCurrent_user.getUid());


                //if success
                mProfileSendReqBtn.setEnabled(true);
                mCurrent_state = "not_friends";
                mProfileSendReqBtn.setText("Send Friend Request");
                mDeclineBtn.setVisibility(View.INVISIBLE);
                mDeclineBtn.setEnabled(false);

//                Map friendsDeclineMap = new HashMap();
//                friendsDeclineMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id, null);
//                friendsDeclineMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid(), null);
//
//                mRootRef.updateChildren(friendsDeclineMap, new DatabaseReference.CompletionListener() {
//                    @Override
//                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//                        if(databaseError == null){
//                            mProfileSendReqBtn.setEnabled(true);
//                            mCurrent_state = "not_friends";
//                            mProfileSendReqBtn.setText("Send Friend Request");
//                            mDeclineBtn.setVisibility(View.INVISIBLE);
//                            mDeclineBtn.setEnabled(false);
//                        }
//                        else {
//
//                            String error = databaseError.getMessage();
//                            Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
            }
        });
    }

    boolean state = false;
    public void getReqType(final String need, String from_uid, String to_uid){

        String FIND_IN_FRIENDS_URL = getString(R.string.API_COMMON_URL)+"findByReq/"+ from_uid +"/" + to_uid;

        // Create a new volley request queue
        queue = Volley.newRequestQueue(getApplicationContext());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                FIND_IN_FRIENDS_URL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Do something with response
                        // Process the JSON
                        try{
                            // Loop through the array elements
                            for(int i=0;i<response.length();i++){
                                Gson gson = new Gson();
                                Type type = new TypeToken<FriendRequest>(){}.getType();
                                friendRequest = gson.fromJson(response.toString(), type);
                                System.out.println(friendRequest);
                                state = true;
                                loadUI();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        // Do something when error occurred
                        System.out.println("An error occuered");
                        System.out.println(error.getMessage());

                        if (checkedInTo == false){
                            calledWhenFrmStateFalse();
                        }
                        else {

                        }

                    }
                }
        );
        queue.add(jsonObjectRequest);
    }


    static boolean isAFriend = false;
    public boolean isFriend(String from, String to){
        String FIND_IN_FRIENDS_URL = getString(R.string.API_COMMON_URL)+"isFriend/"+from+"/"+to;
        String url = null;

        // Create a new volley request queue
        queue = Volley.newRequestQueue(getApplicationContext());

        StringRequest jsonObjectRequest = new StringRequest(
                Request.Method.GET,
                FIND_IN_FRIENDS_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Do something with response
                        // Process the JSON
                        try{
                            System.out.println(response);
                            if (response.toString().equals("true")){
                                isAFriend = true;

                                //change ui
                                mCurrent_state = "friends";
                                mProfileSendReqBtn.setText("Unfriend this Person");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);

                                loadUI();
                            }

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        // Do something when error occurred
                        System.out.println("An error occuered");
                        System.out.println(error);
                    }
                }
        );
        queue.add(jsonObjectRequest);
        System.out.println("Elazzzz"+ isAFriend);
        return isAFriend;
    }




    private void sendRequest(final String from, final String to) {

        String SEND_REQ_URL = getString(R.string.API_COMMON_URL)+"saveReq";

        // Create a new volley request queue
        queue = Volley.newRequestQueue(getApplicationContext());

        FriendRequest request = new FriendRequest();
        request.setFrom(from);
        request.setTo(to);

        final Gson gson = new Gson();
        String json = gson.toJson(request);

        Log.d("TAG", json);

        JSONObject jsonBody = null;

        try {
            jsonBody = new JSONObject(json);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(SEND_REQ_URL, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //Log.d("TAG", response.toString());

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("TAG", error.getMessage(), error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                return params;
            }
        };
        queue.add(jsonObjectRequest);
    }

    private void deleteRequest(final String from, final String to) {

        String DELETE_REQ_URL = getString(R.string.API_COMMON_URL).toString()+"deleteReq/"+from+"/"+to;

        // Create a new volley request queue
        queue = Volley.newRequestQueue(getApplicationContext());

        FriendRequest request = new FriendRequest();
        request.setFrom(from);
        request.setTo(to);

        final Gson gson = new Gson();
        String json = gson.toJson(request);

        Log.d("TAG", json);

        JSONObject jsonBody = null;

        try {
            jsonBody = new JSONObject(json);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println(DELETE_REQ_URL);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.DELETE, DELETE_REQ_URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //Log.d("TAG", response.toString());

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("TAG", error.getMessage(), error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                return params;
            }
        };
        queue.add(jsonObjectRequest);
    }

    public void saveNewFriend(String from, String to){
        String STORE_NEW_FRIEND_URL = getString(R.string.API_COMMON_URL)+"storeFrnd/"+from;

        // Create a new volley request queue
        queue = Volley.newRequestQueue(getApplicationContext());

        Friends friend = new Friends();
        friend.setUid(to);

        final Gson gson = new Gson();
        String json = gson.toJson(friend);

        Log.d("TAG", json);

        JSONObject jsonBody = null;

        try {
            jsonBody = new JSONObject(json);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT , STORE_NEW_FRIEND_URL, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //Log.d("TAG", response.toString());

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("TAG", error.getMessage(), error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                return params;
            }
        };
        queue.add(jsonObjectRequest);
    }
}