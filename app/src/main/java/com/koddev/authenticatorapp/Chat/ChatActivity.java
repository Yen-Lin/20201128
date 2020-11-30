package com.koddev.authenticatorapp.Chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseSmartReply;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestion;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult;
import com.koddev.authenticatorapp.Chat.util.Define;
import com.koddev.authenticatorapp.R;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener
{
    private String TAG = "ChatActivity";
    private String mUserUUID="";
    private String mUserName="";
    private String mSendName = "";
    private RecyclerView mRecyclerView;
    private ImageView mButtonSendIV;
    private EditText mEditTextMessage;

    private TextView mNameTV,txt;

    private Button btn01,btn02,btn03;

    private ChatMessageAdapter mAdapter;

    private LinearLayout replyLinear;
    private ArrayList<ChatMessage> mChatMessageArrayList;
    private ChipGroup smartReplyChipGroup;
    private final String REMOTE_USER_ID = UUID.randomUUID().toString();
    private List<FirebaseTextMessage> chatHistory = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_layout);


        if(getIntent().getExtras()!=null)
        {
            mUserUUID = getIntent().getExtras().getString(Define.UserUUID);

            mUserName = getIntent().getExtras().getString(Define.UserName);
        }

        Log.i(TAG,"mUserName : " + mUserName);

        btn01 = findViewById(R.id.btn01);
        btn02 = findViewById(R.id.btn02);
        btn03 = findViewById(R.id.btn03);

        btn01.setOnClickListener(this);
        btn02.setOnClickListener(this);
        btn03.setOnClickListener(this);

        replyLinear = findViewById(R.id.replylinear);
        mNameTV = findViewById(R.id.textview_name);
        mNameTV.setText(mUserName);
        mChatMessageArrayList = new ArrayList<>();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mButtonSendIV = (ImageView) findViewById(R.id.btn_send);
        mEditTextMessage = (EditText) findViewById(R.id.et_message);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));//設定樣式
        mAdapter = new ChatMessageAdapter(this, new ArrayList<ChatMessage>());

        mRecyclerView.setAdapter(mAdapter);

        mButtonSendIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mEditTextMessage.getText().toString();
                if (TextUtils.isEmpty(message)) {
                    return;
                }
                String createTime = getDateTime();
                sendToFirebase(message,createTime);//
                mEditTextMessage.setText("");
            }
        });

        init();
    }

    private void init()
    {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();

//        Query queryu = FirebaseDatabase.getInstance().getReference("Users").orderByChild("email").equalTo(user.getEmail());
//        queryu.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
//                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//
//                    String name = "" + ds.child("name").getValue();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });





        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("email").equalTo(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    String name = "" + ds.child("name").getValue();
                    mSendName = name;
                }
                if(mSendName.length() > 0)
                    getUserRecord(mSendName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void getUserRecord(final String name){
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Chats").child(mUserName);
        ref.addValueEventListener(new ValueEventListener() {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Chats").child(mUserName);
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                for (DataSnapshot  ds: dataSnapshot.getChildren()){
                    HashMap<Object,String> contentHashMap = (HashMap<Object,String>)ds.getValue();
                    Log.i(TAG,"ds.getValue() : " + ds.getValue());
                    String time = ds.getKey();
                    //ChatMessage chatMessage = null;
                    boolean isCanAdd = true;
                    Log.i(TAG,"message : " + contentHashMap.get("message"));

                    for(int i=0;i<mAdapter.getMessage().size();i++)
                    {
                        if(mAdapter.getMessage().get(i).dateTime().equals(time))
                        {
                            isCanAdd = false;
                            break;
                        }
                    }
                    if(isCanAdd){
                        Log.i(TAG,"mSendName :"+ name);
                        if((contentHashMap.get("receiver").toString().equals(name)) || ((contentHashMap.get("sender").toString().equals(name)))) {
                            if (contentHashMap.get("receiver").contains(name)) {
                                ChatMessage chatMessage = new ChatMessage(contentHashMap.get("message"), time, false, false);
                                mAdapter.add(chatMessage);
                                chatHistory.add(FirebaseTextMessage.createForRemoteUser(contentHashMap.get("message"), System.currentTimeMillis(), REMOTE_USER_ID));
                            } else {
                                ChatMessage chatMessage = new ChatMessage(contentHashMap.get("message"), time, true, false);
                                mAdapter.add(chatMessage);
                                chatHistory.add(FirebaseTextMessage.createForRemoteUser(contentHashMap.get("message"), System.currentTimeMillis(), REMOTE_USER_ID));
                            }
                            suggestReplyingMessages(chatHistory);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private String getDateTime()
    {
        String createTimeTemp = Define.convertUnixTimeToString(Define.getCurrentUnixTime(), "yyyyMMddhhmmss");
        String createTime = "";

        try
        {
            //Local
            createTime = Define.changeDateFormat(createTimeTemp, "yyyyMMddhhmmss", TimeZone.getTimeZone("UTC"), "yyyy-MM-dd:HH:mm:ss", TimeZone.getDefault());
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        return createTime;
    }

    private void sendToFirebase(String message,String createTime)
    {
        FirebaseDatabase database=FirebaseDatabase.getInstance();
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

        //使用者自己
        //DatabaseReference mine =  database.getReference("Users").child(fUser.getUid()).child(Define.ChatContent).child(mUserUUID).child(createTime);
        //聊天的對象
        Log.i(TAG,"mUserUUID : " + mUserUUID);
        Log.i(TAG,"mUserName : " + mUserName);
        Log.i(TAG,"fUser.getUid() :" + fUser.getUid());
        DatabaseReference mine =  database.getReference("Users").child(fUser.getUid()).child(Define.ChatContentName);

        //New Firebase db
        DatabaseReference chatsDB = database.getReference(Define.Chats);
        HashMap<Object,String>hashMapChats=new HashMap<>();

        //hashMapChats.put(Define.isSeen, "true");
        hashMapChats.put(Define.message,message);
        hashMapChats.put(Define.receiver,mUserName);
        hashMapChats.put(Define.sender,mSendName);
        hashMapChats.put(Define.timestamp,getDateTime());
        Log.i(TAG,"mUserName : " + mUserName);
        Log.i(TAG,"hashMapChats : " + hashMapChats.toString());
        chatsDB.child(mSendName).child(getDateTime()).setValue(hashMapChats);


        HashMap<Object,String>hashMapSenderChats=new HashMap<>();

        //hashMapChats.put(Define.isSeen, "true");
        hashMapSenderChats.put(Define.message,message);
        hashMapSenderChats.put(Define.receiver,mUserName);
        hashMapSenderChats.put(Define.sender,mSendName);
        hashMapSenderChats.put(Define.timestamp,getDateTime());
        //hashMapChats.put(Define.timestamp,String.valueOf(Define.getCurrentUnixTime()));
        Log.i(TAG,"mUserName : " + mUserName);
        Log.i(TAG,"hashMapChats : " + hashMapSenderChats.toString());
        chatsDB.child(mUserName).child(getDateTime()).setValue(hashMapSenderChats);


        //chatsDB.child(mUserName).child(getDateTime()).setValue(hashMapChats);

        HashMap<Object,String> mineHashMap=new HashMap<>();
        mineHashMap.put(Define.sender, mUserName);
        mine.setValue(mineHashMap);
//        HashMap<Object,String> friendHashMap=new HashMap<>();
//
//        mineHashMap.put(Define.Sentence, message);
//        mineHashMap.put(Define.IsMine, "true");
//
//        friendHashMap.put(Define.Sentence, message);
//        friendHashMap.put(Define.IsMine, "false");

        //mine.setValue(mineHashMap);
        //friend.setValue(friendHashMap);
    }

    private void suggestReplyingMessages(List<FirebaseTextMessage> chat) {

        FirebaseSmartReply smartReply = FirebaseNaturalLanguage.getInstance().getSmartReply();
        smartReply.suggestReplies(chat).addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
            @Override
            public void onSuccess(SmartReplySuggestionResult result) {
                if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                    Log.e(TAG,"The conversation\\'s language isn\\'t supported, so the result doesn\\'t contain any suggestions.");
                } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                    for (SmartReplySuggestion suggestion : result.getSuggestions()) {
                        result.getSuggestions().size();
                        replyLinear.setVisibility(View.VISIBLE);
                        btn01.setText(result.getSuggestions().get(0).getText());
                        btn02.setText(result.getSuggestions().get(1).getText());
                        btn03.setText(result.getSuggestions().get(2).getText());
                        Log.i(TAG,"suggestion.getText() : " + suggestion.getText());
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG,e.getMessage());
            }
        });
    }


//    private void smartReplies(){
//        FirebaseNaturalLanguage.getInstance().getSmartReply().suggestReplies(chatHistory)
//                .addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
//                    @Override
//                    public void onSuccess(SmartReplySuggestionResult result) {
//                        if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
//                            // The conversation's language isn't supported, so the
//                            // the result doesn't contain any suggestions.
//                        } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
//                            String replyText="";
//                            for (SmartReplySuggestion suggestion : result.getSuggestions()) {
//                                replyText += suggestion.getText()+". ";
//                            }
//                            Log.i(TAG,"replies : " + );
//                            replies.setText(replyText);
//                        }
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        // Task failed with an exception
//                        // ...
//                    }
//                });
//
//    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn01:
                mEditTextMessage.setText(btn01.getText().toString());
                break;
            case R.id.btn02:
                mEditTextMessage.setText(btn02.getText().toString());
                break;
            case R.id.btn03:
                mEditTextMessage.setText(btn03.getText().toString());
                break;

        }
    }

}