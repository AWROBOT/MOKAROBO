package com.argeworld.robotics.mokarobo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SongListScreen extends AppCompatActivity
{
    private static final String TAG = SongListScreen.class.getSimpleName();

    private ListView listView;

    private MainApplication m_Main;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        m_Main = (MainApplication) this.getApplication();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.song_list);

        listView = (ListView) findViewById(R.id.listView);

        LoadSongList();

        List<String> song_array= new ArrayList<String>();

        for (int i = 0; i < m_Main.songList.size(); i++)
        {
            String name = m_Main.songList.get(i).name;

            Log.i(TAG, "Song Name: " + name);

            song_array.add(name);
        }

        ArrayAdapter<String> mHistory = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, song_array);
        listView.setAdapter(mHistory);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                m_Main.iSelectedSong = position;

                m_Main.strSelectedSongName = m_Main.songList.get(position).name;

                Intent intent = new Intent(getApplicationContext(), SongScreen.class);
                startActivity(intent);
            }
        });
    }

    void LoadSongList()
    {
        Log.i(TAG, "LoadSongList");

        m_Main.songList = new ArrayList<SongListItem>();

        String [] list;
        try
        {
            list = getAssets().list("Songs");
            if (list.length > 0)
            {
                // This is a folder
                for (String file : list)
                {
                    Log.i(TAG, "File: " + file);

                    SongListItem tmp_item = new SongListItem();

                    String [] list2 = getAssets().list("Songs/" + file);

                    for (String file2 : list2)
                    {
                        //Log.i(TAG, "File2: " + file2);

                        if(file2.indexOf(".mp3") > -1)
                        {
                            tmp_item.songPath = "Songs/" + file + "/" + file2;

                            Log.i(TAG, "Song Path: " + tmp_item.songPath);
                        }

                        if(file2.indexOf(".jpg") > -1 || file2.indexOf(".png") > -1)
                        {
                            tmp_item.imagePath = "Songs/" + file + "/" + file2;

                            Log.i(TAG, "Image Path: " + tmp_item.imagePath);
                        }

                        if(file2.indexOf(".osu") > -1 && (file2.indexOf("[Futsuu]") > -1 || file2.indexOf("[Normal]") > -1)) //[Kantan]
                        {
                            tmp_item.osuPath = "Songs/" + file + "/" + file2;

                            Log.i(TAG, "Osu Path: " + tmp_item.osuPath);

                            tmp_item.name = GetSongName(tmp_item.osuPath);

                            Log.i(TAG, "Name:" + tmp_item.name);

                            //Add it to list
                            m_Main.songList.add(tmp_item);
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "List Error:" + e.getMessage());
        }
    }

    String GetSongName(String osu_path)
    {
        String name = "";

        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(getAssets().open(osu_path)));

            String line = "";

            while (!line.contains("Title:"))
            {
                line = reader.readLine();
            }

            name = line.split(":")[1].trim();
            if (name.length() >= 30)
            {
                name = name.substring(0, 25) + "...";
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "GetSongName Error:" + e.getMessage());
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    Log.e(TAG, "GetSongName Error2:" + e.getMessage());
                }
            }
        }

        return name;
    }
}
