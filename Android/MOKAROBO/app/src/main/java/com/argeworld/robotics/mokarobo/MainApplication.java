package com.argeworld.robotics.mokarobo;

import android.app.Application;

import java.util.List;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;

public class MainApplication extends Application
{
    public List<SongListItem> songList;

    public List<Rhythm> rhythms;

    public int iSelectedSong;

    public String strSelectedSongName;

    public boolean bInited = false;

    public int iSelectedModule = 0;

    public BluetoothSPP bt;

    @Override
    public void onCreate()
    {
        super.onCreate();
    }
}
