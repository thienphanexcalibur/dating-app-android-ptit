package com.example.nam_t.datingapp;


import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nam_t.datingapp.Users.user_Adapter;
import com.example.nam_t.datingapp.Users.user_object;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yahoo.mobile.client.android.util.rangeseekbar.RangeSeekBar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class tab_userlist extends Fragment {
    RangeSeekBar<Integer> age;
    RangeSeekBar<Integer> distance;
    private Button btn_save;
    private int min,max;
    private FirebaseAuth mAuth;
    private String currentUId;
    private DatabaseReference usersDb;
    private float min_dis, max_dis;
    private RecyclerView userListRecyclerView;
    private RecyclerView.Adapter userListAdapter;
    private RecyclerView.LayoutManager userListLayoutManager;
    private ArrayList<user_object> userList=new ArrayList<>();
    private List<user_object> getDataSetUser(){
        return userList;
    }
    private float currentUserLong,currentUserLat;
    public tab_userlist() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_tab_userlist, container, false);
        usersDb= FirebaseDatabase.getInstance().getReference().child("Users");
        mAuth=FirebaseAuth.getInstance();
        currentUId=mAuth.getCurrentUser().getUid();
        age=(RangeSeekBar<Integer>) view.findViewById(R.id.filter_age);
        distance=(RangeSeekBar<Integer>) view.findViewById(R.id.filter_distance);
        btn_save=(Button) view.findViewById(R.id.save);
        userListRecyclerView=(RecyclerView)view.findViewById(R.id.user_list);
        final TextView age_view=view.findViewById(R.id.age_view);
        // Get noticed while dragging
        age.setNotifyWhileDragging(true);
        userListRecyclerView=view.findViewById(R.id.user_list);
        userListRecyclerView.setNestedScrollingEnabled(false);
        userListRecyclerView.setHasFixedSize(true);
        userListLayoutManager=new LinearLayoutManager(getContext());
        userListRecyclerView.setLayoutManager(userListLayoutManager);
        userListAdapter=new user_Adapter(getDataSetUser(),getContext());
        userListRecyclerView.setAdapter(userListAdapter);
        checkUserGender();
        getUserPosition();
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                min=age.getSelectedMinValue();
                max=age.getSelectedMaxValue();
                min_dis =Float.parseFloat(distance.getSelectedMinValue().toString());
                max_dis =Float.parseFloat(distance.getSelectedMaxValue().toString());
                age_view.setText(min+"-"+max+" years old");
                userList.clear();
                userListAdapter.notifyDataSetChanged();
                getSuitableUsers();
            }
        });

        return view;
    }

    private String userGender;
    private String oppositeUserGender;
    public void checkUserGender(){
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userDb = usersDb.child(user.getUid());
        userDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    if (dataSnapshot.child("gender").getValue() != null){
                        userGender = dataSnapshot.child("gender").getValue().toString();
                        switch (userGender){
                            case "Male":
                                oppositeUserGender = "Female";
                                break;
                            case "Female":
                                oppositeUserGender = "Male";
                                break;
                        }

                    }

                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void getUserPosition(){
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userDb = usersDb.child(user.getUid());
        userDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    if (dataSnapshot.child("latitude").getValue() !=null && dataSnapshot.child("longtitude").getValue() !=null){
                        currentUserLong = Float.parseFloat(dataSnapshot.child("longtitude").getValue().toString());
                        currentUserLat = Float.parseFloat(dataSnapshot.child("longtitude").getValue().toString());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private double distance_arithmetic(float longA, float latA, float longB, float latB){
        float pk = (float) (180.f/Math.PI);

        float a1 = latA / pk;
        float a2 = longA / pk;
        float b1 = latB / pk;
        float b2 = longB / pk;

        double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
        double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
        double t3 = Math.sin(a1) * Math.sin(b1);
        double tt = Math.acos(t1 + t2 + t3);
        double ans = 6366 * tt/100;
        return  ans;
    }
    private float distance_api(float longA, float latA, float longB, float latB){
        Location locationA = new Location("point A");

        locationA.setLatitude(latA);
        locationA.setLongitude(longA);

        Location locationB = new Location("point B");

        locationB.setLatitude(latB);
        locationB.setLongitude(longB);
        float dis = locationA.distanceTo(locationB)/1000000;
        return dis;
    }
    public static Double getDistanceBetween(float longA, float latA, float longB, float latB) {
        float[] result = new float[1];
        Location.distanceBetween(latA, longA,
                latB, longB, result);
        return (double) result[0]/1000000;
    }

    public void getSuitableUsers(){
        usersDb.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.child("gender").getValue() instanceof String && dataSnapshot.exists() && dataSnapshot.child("gender").getValue().toString().equals(oppositeUserGender)) {
                    int userAge=Calendar.getInstance().get(Calendar.YEAR)-Integer.parseInt(dataSnapshot.child("DOB_yyyy").getValue().toString());
                    double distance_arithmetic = distance_arithmetic(currentUserLong,currentUserLat,Float.parseFloat(dataSnapshot.child("longtitude").getValue().toString()),Float.parseFloat(dataSnapshot.child("latitude").getValue().toString()));
                    float distance_api = distance_api(currentUserLong,currentUserLat,Float.parseFloat(dataSnapshot.child("longtitude").getValue().toString()),Float.parseFloat(dataSnapshot.child("latitude").getValue().toString()));
                    double distance_between =getDistanceBetween(currentUserLong,currentUserLat,Float.parseFloat(dataSnapshot.child("longtitude").getValue().toString()),Float.parseFloat(dataSnapshot.child("latitude").getValue().toString()));
                    System.out.println(distance_between);
                    if (userAge<=max&&userAge>=min&&distance_between<max_dis&&distance_between>min_dis) {
                        user_object item = new user_object(dataSnapshot.getKey(), dataSnapshot.child("name").getValue().toString(),String.valueOf(userAge),dataSnapshot.child("profileImageUrl").getValue().toString(),distance_between);
                        userList.add(item);
                        userListAdapter.notifyDataSetChanged();
                    }
                }
                Collections.shuffle(userList);
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }



}