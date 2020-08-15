package com.argeworld.robotics.mokarobo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import info.hoang8f.widget.FButton;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener
{
    private static final String TAG = MainActivity.class.getSimpleName();

    private FButton btnWalk;
    private FButton btnBack;
    private FButton btnLeft;
    private FButton btnRight;
    private FButton btnRandom;
    //private FButton btnUpDown;
    private FButton btnDance;
    private FButton btnTalk;
    private FButton btnPatrol;
    //private FButton btnMoonWalk;

    // DRUM MODULE
    private FButton btnDrumLeft;
    private FButton btnDrumRight;
    private FButton btnDrumBoth;
    private FButton btnPlaySong;

    // SHOVEL MODULE
    private FButton btnShovelUp;
    private FButton btnShovelDown;

    // CLAW MODULE
    private FButton btnClawUp;
    private FButton btnClawDown;
    private FButton btnClawOpen;
    private FButton btnClawClose;

    private MaterialDialog dialog;

    private int iBTSetup;

    private SlidingMenu menu;
    private ListView menuList;
    private TextView txtTitle;

    private static MainActivity instance;
    private MainApplication m_Main;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        m_Main = (MainApplication) this.getApplication();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        if(m_Main.bt == null)
        {
            m_Main.bt = new BluetoothSPP(this);

            if (!m_Main.bt.isBluetoothAvailable()) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title("Error")
                        .content("Bluetooth is not available !")
                        .negativeText("EXIT")
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                System.exit(0);
                            }
                        })
                        .canceledOnTouchOutside(false)
                        .cancelable(false)
                        .show();

                return;
            }

            m_Main.bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
                public void onDataReceived(byte[] data, String message) {
                    Log.i(TAG, "BT onDataReceived: " + message);
                }
            });

            m_Main.bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
                public void onDeviceConnected(String name, String address) {
                    Log.i(TAG, "BT onDeviceConnected: " + name + " " + address);

                    Toast.makeText(getApplicationContext()
                            , "Connected to " + name
                            , Toast.LENGTH_SHORT).show();

                    SendCommand("I");

                    iBTSetup = 1;

                    SharedPreferences settings = getApplicationContext().getSharedPreferences("MOKAROBO_APP", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt("BT_SETUP", iBTSetup);
                    editor.apply();
                }

                public void onDeviceDisconnected() {
                    Toast.makeText(getApplicationContext()
                            , "Connection lost", Toast.LENGTH_SHORT).show();
                }

                public void onDeviceConnectionFailed() {
                    Toast.makeText(getApplicationContext()
                            , "Unable to connect", Toast.LENGTH_SHORT).show();
                }
            });

            m_Main.bt.setBluetoothStateListener(new BluetoothSPP.BluetoothStateListener() {
                public void onServiceStateChanged(int state) {
                    if (state == BluetoothState.STATE_CONNECTED) {
                        Log.i(TAG, "BT onServiceStateChanged to CONNECTED");

                        if (dialog != null)
                            dialog.dismiss();

                        Toast.makeText(getApplicationContext()
                                , "Connected to MOKAROBO"
                                , Toast.LENGTH_SHORT).show();

                        SendCommand("I");
                    } else if (state == BluetoothState.STATE_CONNECTING) {
                        Log.i(TAG, "BT onServiceStateChanged to CONNECTING");
                    } else if (state == BluetoothState.STATE_LISTEN) {
                        Log.i(TAG, "BT onServiceStateChanged to LISTEN");
                    } else if (state == BluetoothState.STATE_NONE) {
                        Log.i(TAG, "BT onServiceStateChanged to STATE_NONE");
                    }
                }
            });

            m_Main.bt.setAutoConnectionListener(new BluetoothSPP.AutoConnectionListener() {
                public void onNewConnection(String name, String address) {
                    Log.i(TAG, "New Connection - " + name + " - " + address);

                    if (!name.equals("MOKAROBO")) {
                        if (dialog != null)
                            dialog.dismiss();

                        iBTSetup = 0;

                        SharedPreferences settings = getApplicationContext().getSharedPreferences("MOKAROBO_APP", 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putInt("BT_SETUP", iBTSetup);
                        editor.apply();

                        InitBT();
                    }
                }

                public void onAutoConnectionStarted() {
                    Log.i(TAG, "Auto menu_connection started");
                }
            });

            InitBT();
        }

        SetupButtons();

        Show();
    }

    public static MainActivity getInstance()
    {
        return instance;
    }

    public void onStart()
    {
        Log.i(TAG, "onStart");

        super.onStart();

        if(!m_Main.bInited)
        {
            if (!m_Main.bt.isBluetoothEnabled())
            {
                Log.i(TAG, "BT disabled");

                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
            }
            else
            {
                Log.i(TAG, "BT enabled");

                if(!m_Main.bt.isServiceAvailable())
                {
                    StartBTService();
                }
                else
                {
                    SendCommand("I");
                }
            }
        }
    }

    public void onDestroy()
    {
        super.onDestroy();
        //m_Main.bt.stopService();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(!m_Main.bInited)
        {
            if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE)
            {
                if(resultCode == Activity.RESULT_OK)
                    m_Main.bt.connect(data);
            }
            else if(requestCode == BluetoothState.REQUEST_ENABLE_BT)
            {
                if(resultCode == Activity.RESULT_OK)
                {
                    StartBTService();
                }
                else
                {
                    Toast.makeText(getApplicationContext()
                            , "Bluetooth was not enabled."
                            , Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    public void StartBTService()
    {
        Log.i(TAG,"StartBTService");

        dialog = new MaterialDialog.Builder(this)
                .title("Please Wait")
                .content("Connecting to MOKAROBO...")
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .canceledOnTouchOutside(false)
                .cancelable(false)
                .show();

        m_Main.bt.setupService();
        m_Main.bt.startService(BluetoothState.DEVICE_OTHER);

        if(iBTSetup == 1)
        {
            Log.i(TAG,"AutoConnect");

            m_Main.bt.autoConnect("MOKAROBO");
        }
    }

    public void InitBT()
    {
        if(!m_Main.bInited)
        {
            SharedPreferences settings = getApplicationContext().getSharedPreferences("MOKAROBO_APP", 0);
            iBTSetup = settings.getInt("BT_SETUP", 0);

            if (iBTSetup == 0)
            {
                if (m_Main.bt.getServiceState() == BluetoothState.STATE_CONNECTED)
                {
                    m_Main.bt.disconnect();
                }
                else
                    {
                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                }
            }
        }
    }

    public void SetupButtons()
    {
        btnWalk = (FButton) findViewById(R.id.walk_button);
        btnWalk.setButtonColor(getResources().getColor(R.color.fbutton_color_orange));
        btnWalk.setOnTouchListener(this);

        btnLeft = (FButton) findViewById(R.id.left_button);
        btnLeft.setButtonColor(getResources().getColor(R.color.fbutton_color_orange));
        btnLeft.setOnTouchListener(this);

        btnRight = (FButton) findViewById(R.id.right_button);
        btnRight.setButtonColor(getResources().getColor(R.color.fbutton_color_orange));
        btnRight.setOnTouchListener(this);

        btnBack = (FButton) findViewById(R.id.back_button);
        btnBack.setButtonColor(getResources().getColor(R.color.fbutton_color_orange));
        btnBack.setOnTouchListener(this);

        btnRandom = (FButton) findViewById(R.id.random_button);
        btnRandom.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnRandom.setOnTouchListener(this);

        //btnUpDown = (FButton) findViewById(R.id.updown_button);
        //btnUpDown.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        //btnUpDown.setOnTouchListener(this);

        btnDance = (FButton) findViewById(R.id.dance_button);
        btnDance.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnDance.setOnTouchListener(this);

        btnTalk = (FButton) findViewById(R.id.talk_button);
        btnTalk.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnTalk.setOnTouchListener(this);

        btnPatrol = (FButton) findViewById(R.id.patrol_button);
        btnPatrol.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnPatrol.setOnTouchListener(this);

        //btnMoonWalk = (FButton) findViewById(R.id.moonwalk_button);
        //btnMoonWalk.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        //btnMoonWalk.setOnTouchListener(this);

        // DRUM MODULE

        btnDrumLeft = (FButton) findViewById(R.id.drum_left_button);
        btnDrumLeft.setButtonColor(getResources().getColor(R.color.fbutton_color_alizarin));
        btnDrumLeft.setOnTouchListener(this);

        btnDrumRight = (FButton) findViewById(R.id.drum_right_button);
        btnDrumRight.setButtonColor(getResources().getColor(R.color.fbutton_color_alizarin));
        btnDrumRight.setOnTouchListener(this);

        btnDrumBoth = (FButton) findViewById(R.id.drum_both_button);
        btnDrumBoth.setButtonColor(getResources().getColor(R.color.fbutton_color_emerald));
        btnDrumBoth.setOnTouchListener(this);

        btnPlaySong = (FButton) findViewById(R.id.play_song_button);
        btnPlaySong.setButtonColor(getResources().getColor(R.color.fbutton_color_amethyst));
        btnPlaySong.setOnTouchListener(this);

        btnDrumLeft.setVisibility(View.GONE);
        btnDrumRight.setVisibility(View.GONE);
        btnDrumBoth.setVisibility(View.GONE);
        btnPlaySong.setVisibility(View.GONE);

        // SHOVEL MODULE

        btnShovelUp = (FButton) findViewById(R.id.shovel_up_button);
        btnShovelUp.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnShovelUp.setOnTouchListener(this);

        btnShovelDown = (FButton) findViewById(R.id.shovel_down_button);
        btnShovelDown.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnShovelDown.setOnTouchListener(this);

        btnShovelUp.setVisibility(View.GONE);
        btnShovelDown.setVisibility(View.GONE);

        // CLAW MODULE

        btnClawUp = (FButton) findViewById(R.id.claw_up_button);
        btnClawUp.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnClawUp.setOnTouchListener(this);

        btnClawDown = (FButton) findViewById(R.id.claw_down_button);
        btnClawDown.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnClawDown.setOnTouchListener(this);

        btnClawOpen = (FButton) findViewById(R.id.claw_open_button);
        btnClawOpen.setButtonColor(getResources().getColor(R.color.fbutton_color_alizarin));
        btnClawOpen.setOnTouchListener(this);

        btnClawClose = (FButton) findViewById(R.id.claw_close_button);
        btnClawClose.setButtonColor(getResources().getColor(R.color.fbutton_color_alizarin));
        btnClawClose.setOnTouchListener(this);

        btnClawUp.setVisibility(View.GONE);
        btnClawDown.setVisibility(View.GONE);
        btnClawOpen.setVisibility(View.GONE);
        btnClawClose.setVisibility(View.GONE);
    }

    public void HideButtons()
    {
        btnRandom.setVisibility(View.GONE);
        btnDance.setVisibility(View.GONE);
        btnTalk.setVisibility(View.GONE);
        btnPatrol.setVisibility(View.GONE);

        // DRUM MODULE

        btnDrumLeft.setVisibility(View.GONE);
        btnDrumRight.setVisibility(View.GONE);
        btnDrumBoth.setVisibility(View.GONE);
        btnPlaySong.setVisibility(View.GONE);

        // SHOVEL MODULE

        btnShovelUp.setVisibility(View.GONE);
        btnShovelDown.setVisibility(View.GONE);

        // CLAW MODULE

        btnClawUp.setVisibility(View.GONE);
        btnClawDown.setVisibility(View.GONE);
        btnClawOpen.setVisibility(View.GONE);
        btnClawClose.setVisibility(View.GONE);
    }

    public void Show()
    {
        txtTitle = (TextView) findViewById(R.id.title);

        //Left Menu

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int menu_w = (int)(metrics.widthPixels * 0.75f);

        menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setFadeDegree(0.35f);
        menu.setShadowWidth(30);
        menu.setBehindOffset(30);
        menu.setBehindWidth(menu_w);
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        menu.setMenu(R.layout.menu);

        menuList = (ListView) this.findViewById(R.id.menuList);

        ArrayList<String> menuItems = new ArrayList<String>();

        menuItems.add("BASIC KIT");
        menuItems.add("SHOVEL MODULE");
        menuItems.add("CLAW MODULE");
        menuItems.add("DRUM MODULE");

        menuList.setAdapter(new com.argeworld.robotics.mokarobo.adapters.MenuAdapter(this, R.layout.menu_item, menuItems.toArray( new String[menuItems.size()])));

        menuList.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
            {
                Log.e(TAG, "MENU: " + arg2);

                m_Main.iSelectedModule = arg2;

                SetupModule();

                menu.toggle(true);
            }
        });

        Button btnMenu = (Button) findViewById(R.id.btnMenu);
        btnMenu.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                    {
                        v.getBackground().setColorFilter(0xFFDDDDDD, PorterDuff.Mode.LIGHTEN);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    {
                        v.getBackground().clearColorFilter();
                        v.invalidate();
                        break;
                    }
                }
                return false;
            }
        });
        btnMenu.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                menu.toggle(true);
            }
        });

        SetupModule();
    }

    public void SetupModule()
    {
        if(m_Main.iSelectedModule == 0)
        {
            txtTitle.setText("BASIC KIT");

            HideButtons();

            btnRandom.setVisibility(View.VISIBLE);
            btnDance.setVisibility(View.VISIBLE);
            btnTalk.setVisibility(View.VISIBLE);
            btnPatrol.setVisibility(View.VISIBLE);
        }
        else if(m_Main.iSelectedModule == 1)
        {
            txtTitle.setText("SHOVEL MODULE");

            HideButtons();

            btnShovelUp.setVisibility(View.VISIBLE);
            btnShovelDown.setVisibility(View.VISIBLE);
        }
        else if(m_Main.iSelectedModule == 2)
        {
            txtTitle.setText("CLAW MODULE");

            HideButtons();

            btnClawUp.setVisibility(View.VISIBLE);
            btnClawDown.setVisibility(View.VISIBLE);
            btnClawOpen.setVisibility(View.VISIBLE);
            btnClawClose.setVisibility(View.VISIBLE);
        }
        else if(m_Main.iSelectedModule == 3)
        {
            txtTitle.setText("DRUM MODULE");

            HideButtons();

            btnDrumLeft.setVisibility(View.VISIBLE);
            btnDrumRight.setVisibility(View.VISIBLE);
            btnDrumBoth.setVisibility(View.VISIBLE);
            btnPlaySong.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if(v.getId() == R.id.walk_button)
            {
                Log.i(TAG,"Walk Button Down");

                btnWalk.onTouch(v, event);

                SendCommand("F");
            }
            else if(v.getId() == R.id.back_button)
            {
                Log.i(TAG,"Back Button Down");

                btnBack.onTouch(v, event);

                SendCommand("B");
            }
            else if(v.getId() == R.id.left_button)
            {
                Log.i(TAG,"Left Button Down");

                btnLeft.onTouch(v, event);

                SendCommand("L");
            }
            else if(v.getId() == R.id.right_button)
            {
                Log.i(TAG,"Right Button Down");

                btnRight.onTouch(v, event);

                SendCommand("R");
            }
            else if(v.getId() == R.id.random_button)
            {
                Log.i(TAG,"Random Button Down");

                btnRandom.onTouch(v, event);

                SendCommand("C");
            }
            /*
            else if(v.getId() == R.id.updown_button)
            {
                Log.i(TAG,"UpDown Button Down");

                btnUpDown.onTouch(v, event);

                SendCommand("U");
            }
            */
            else if(v.getId() == R.id.dance_button)
            {
                Log.i(TAG,"Dance Button Down");

                btnDance.onTouch(v, event);

                SendCommand("D");
            }
            else if(v.getId() == R.id.talk_button)
            {
                Log.i(TAG,"Talk Button Down");

                btnTalk.onTouch(v, event);

                SendCommand("T");
            }
            else if(v.getId() == R.id.patrol_button)
            {
                Log.i(TAG,"Patrol Button Down");

                btnPatrol.onTouch(v, event);

                SendCommand("X");
            }
            /*
            else if(v.getId() == R.id.moonwalk_button)
            {
                Log.i(TAG,"MoonWalk Button Down");

                btnMoonWalk.onTouch(v, event);

                SendCommand("M");
            }
            */

            // DRUM MODULE

            else if(v.getId() == R.id.drum_both_button)
            {
                Log.i(TAG,"Drum Both Button Down");

                btnDrumBoth.onTouch(v, event);

                SendCommand("2");
            }
            else if(v.getId() == R.id.drum_left_button)
            {
                Log.i(TAG,"Drum Left Button Down");

                btnDrumLeft.onTouch(v, event);

                SendCommand("0");
            }
            else if(v.getId() == R.id.drum_right_button)
            {
                Log.i(TAG,"Drum Right Button Down");

                btnDrumRight.onTouch(v, event);

                SendCommand("1");
            }
            else if(v.getId() == R.id.play_song_button)
            {
                Log.i(TAG,"Play Song Button Down");

                btnPlaySong.onTouch(v, event);

                Intent intent = new Intent(getApplicationContext(), SongListScreen.class);
                startActivity(intent);
            }

            // SHOVEL MODULE

            else if(v.getId() == R.id.shovel_up_button)
            {
                Log.i(TAG,"Shovel Up Button Down");

                btnShovelUp.onTouch(v, event);

                SendCommand("3");
            }
            else if(v.getId() == R.id.shovel_down_button)
            {
                Log.i(TAG,"Shovel Down Button Down");

                btnShovelDown.onTouch(v, event);

                SendCommand("4");
            }

            // CLAW MODULE

            else if(v.getId() == R.id.claw_up_button)
            {
                Log.i(TAG,"Claw Up Button Down");

                btnClawUp.onTouch(v, event);

                SendCommand("4");
            }
            else if(v.getId() == R.id.claw_down_button)
            {
                Log.i(TAG,"Claw Down Button Down");

                btnClawDown.onTouch(v, event);

                SendCommand("3");
            }
            else if(v.getId() == R.id.claw_open_button)
            {
                Log.i(TAG,"Claw Open Button Down");

                btnClawOpen.onTouch(v, event);

                SendCommand("5");
            }
            else if(v.getId() == R.id.claw_close_button)
            {
                Log.i(TAG,"Claw Close Button Down");

                btnClawClose.onTouch(v, event);

                SendCommand("6");
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            if(v.getId() == R.id.walk_button)
            {
                Log.i(TAG,"Walk Button Up");

                btnWalk.onTouch(v, event);

                SendCommand("S");
            }
            else if(v.getId() == R.id.back_button)
            {
                Log.i(TAG,"Back Button Up");

                btnBack.onTouch(v, event);

                SendCommand("S");
            }
            else if(v.getId() == R.id.left_button)
            {
                Log.i(TAG,"Left Button Up");

                btnLeft.onTouch(v, event);

                SendCommand("S");
            }
            else if(v.getId() == R.id.right_button)
            {
                Log.i(TAG,"Right Button Up");

                btnRight.onTouch(v, event);

                SendCommand("S");
            }
            else if(v.getId() == R.id.random_button)
            {
                Log.i(TAG,"Random Button Up");

                btnRandom.onTouch(v, event);
            }
            /*
            else if(v.getId() == R.id.updown_button)
            {
                Log.i(TAG,"UpDown Button Up");

                btnUpDown.onTouch(v, event);
            }
            */
            else if(v.getId() == R.id.dance_button)
            {
                Log.i(TAG,"Dance Button Up");

                btnDance.onTouch(v, event);
            }
            else if(v.getId() == R.id.talk_button)
            {
                Log.i(TAG,"Talk Button Up");

                btnTalk.onTouch(v, event);
            }
            else if(v.getId() == R.id.patrol_button)
            {
                Log.i(TAG,"Patrol Button Up");

                btnPatrol.onTouch(v, event);
            }
            /*
            else if(v.getId() == R.id.moonwalk_button)
            {
                Log.i(TAG,"MoonWalk Button Up");

                btnMoonWalk.onTouch(v, event);
            }
            */

            // DRUM MODULE

            else if(v.getId() == R.id.drum_both_button)
            {
                Log.i(TAG,"Drum Both Button Up");

                btnDrumBoth.onTouch(v, event);
            }
            else if(v.getId() == R.id.drum_left_button)
            {
                Log.i(TAG,"Drum Left Button Up");

                btnDrumLeft.onTouch(v, event);
            }
            else if(v.getId() == R.id.drum_right_button)
            {
                Log.i(TAG,"Drum Right Button Up");

                btnDrumRight.onTouch(v, event);
            }
            else if(v.getId() == R.id.play_song_button)
            {
                Log.i(TAG,"Play Song Button Up");

                btnPlaySong.onTouch(v, event);
            }

            // SHOVEL MODULE

            else if(v.getId() == R.id.shovel_up_button)
            {
                Log.i(TAG,"Shovel Up Button Up");

                btnShovelUp.onTouch(v, event);
            }
            else if(v.getId() == R.id.shovel_down_button)
            {
                Log.i(TAG,"Shovel Down Button Up");

                btnShovelDown.onTouch(v, event);
            }

            // CLAW MODULE

            else if(v.getId() == R.id.claw_up_button)
            {
                Log.i(TAG,"Claw Up Button Up");

                btnClawUp.onTouch(v, event);
            }
            else if(v.getId() == R.id.claw_down_button)
            {
                Log.i(TAG,"Claw Down Button Up");

                btnClawDown.onTouch(v, event);
            }
            else if(v.getId() == R.id.claw_open_button)
            {
                Log.i(TAG,"Claw Open Button Up");

                btnClawOpen.onTouch(v, event);
            }
            else if(v.getId() == R.id.claw_close_button)
            {
                Log.i(TAG,"Claw Close Button Up");

                btnClawClose.onTouch(v, event);
            }
        }

        return true;
    }

    public void SendCommand(String cmd)
    {
        Log.i(TAG, "SendCommand: " + cmd);

        if(m_Main.bt != null)
        {
            m_Main.bt.send(cmd, true);

            if (cmd.equals("I"))
            {
                m_Main.bInited = true;
            }
        }
    }
}
