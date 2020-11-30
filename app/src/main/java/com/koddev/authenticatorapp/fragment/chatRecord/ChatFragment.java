package com.koddev.authenticatorapp.fragment.chatRecord;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.koddev.authenticatorapp.AgentActivity;
import com.koddev.authenticatorapp.Login.MainActivity;
import com.koddev.authenticatorapp.R;
import com.koddev.authenticatorapp.Chat.util.Define;

import com.koddev.authenticatorapp.Chat.AdapterChat;
import com.koddev.authenticatorapp.users.AdapterUsers;
import com.koddev.authenticatorapp.users.ModelUser;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatFragment extends Fragment {
    private String TAG = "ChatFragment";
    RecyclerView recyclerView;
    AdapterChat adapterChat;
    List<ModelUser> chatList;
    AdapterChatRecord adapterUsers;
    List<ModelUser> userList;
    String mSendName = "";
    ArrayList<String> mylist = new ArrayList<String>();
    FirebaseAuth firebaseAuth;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    FirebaseUser user;

    DatabaseReference refChat;

    public ChatFragment(){

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        Log.i(TAG,"onCreateView");

        firebaseAuth=FirebaseAuth.getInstance();

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");

        recyclerView = view.findViewById(R.id.chat_recyclerView);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        userList = new ArrayList<>();
        chatList = new ArrayList<>();

        getChatRecordName();

        return view;
    }

    private void getAllUsers(final ArrayList<String> arr) {
        Log.i(TAG,"arr : " + arr.toString());
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    for(int x = 0;x<arr.size();x++){
                        if(ds.child("name").toString().contains(arr.get(x))){
                            Log.i("ChatFragment", "name : " + ds.child("name").getValue());
                            ModelUser modelUser = ds.getValue(ModelUser.class);
                            userList.add(modelUser);
                        }
                    }
//                    if(!userList.isEmpty()) {
//                        ModelUser modelUser = ds.getValue(ModelUser.class);
//                        userList.add(modelUser);
//                        adapterUsers = new AdapterUsers(getActivity(), userList);
//                        recyclerView.setAdapter(adapterUsers);
//                    }
                        adapterUsers = new AdapterChatRecord(getActivity(), userList);
                        recyclerView.setAdapter(adapterUsers);
                }
                ref.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void getChatRecordName() {
        final Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    String name = "" + ds.child("name").getValue();
                    mSendName = name;

                    Log.i(TAG,"mSendName : "+mSendName);

                }
                if(mSendName.length() > 0)
                    getFriendRecord(mSendName);
                query.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    public void getFriendRecord(final String name){
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

        ArrayList[] test;
        Log.i(TAG,"name : " + name);
        refChat = FirebaseDatabase.getInstance().getReference("Chats").child(name);

        refChat.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mylist.clear();
                chatList.clear();
                userList.clear();
                recyclerView.removeAllViewsInLayout();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    boolean isAdd = true;
                    HashMap<Object,String> contentHashMap = (HashMap<Object,String>)ds.getValue();
                    Log.i(TAG,"ds.getValue() : " + ds.getValue());
                    String time = ds.getKey();
                    for(int x = 0;x<mylist.size();x++){
                        Log.i(TAG,"x : " + x);
                        Log.i(TAG,"=====================================");
                        Log.i(TAG,"mylist.get(x) : " + mylist.get(x));
                        Log.i(TAG,"contentHashMap.get(\"receiver\").toString() : " + contentHashMap.get("receiver").toString());
                        if((mylist.get(x).toString().contains(contentHashMap.get("receiver").toString())) || (name.contains(contentHashMap.get("receiver").toString()))){
                            Log.i(TAG,"isAdd = false");
                            isAdd = false;
                        }
                    }
                    if(isAdd){
                        Log.i(TAG,"name : " + name);
                        Log.i(TAG,"contentHashMap.get(\"receiver\") : " + contentHashMap.get("receiver"));
                        if(!name.contains(contentHashMap.get("receiver").toString())) {
                            Log.i(TAG, "isAdd");
                            if ((contentHashMap.get("receiver").toString().contains(name))) {
                                mylist.add(contentHashMap.get("sender").toString());
                            }else{
                                mylist.add(contentHashMap.get("receiver").toString());
                            }
                        }
                    }
                }
                for(int y = 0;y<mylist.size();y++){
                    Log.i(TAG,"mylist : " + mylist.get(y));
                }
                getAllUsers(mylist);
                refChat.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void  checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {

        } else {
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main,menu);
        super.onCreateOptionsMenu(menu,inflater);
    }



    @Override
    public boolean onOptionsItemSelected( MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }




        if (item.getItemId()==R.id.action_Service)
        {
            startActivity(new Intent(getActivity(), AgentActivity.class));

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        Log.i(TAG,"onPause");

//        mylist.clear();
//        chatList.clear();
//        userList.clear();
//        recyclerView.removeAllViewsInLayout();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();  // Always call the superclass method first
        Log.i(TAG,"onDestroyView");
        //recyclerView.removeAllViewsInLayout();

    }

    }