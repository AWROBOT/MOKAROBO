package com.argeworld.robotics.mokarobo;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SongScreen extends AppCompatActivity
{
    private static final String TAG = SongScreen.class.getSimpleName();

    private ImageView imageView;

    private MediaPlayer mediaPlayer;

    private int play_index = 0;

    private MainApplication m_Main;

    private boolean bRunning = true;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        m_Main = (MainApplication) this.getApplication();

        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.song);

        imageView = (ImageView) findViewById(R.id.imageView);
    }

    @Override
    public void onStart()
    {
        Log.i(TAG, "onStart");

        super.onStart();

        ReadOSU();

        SongThread.start();
    }

    @Override
    public void onStop()
    {
        Log.i(TAG, "onStop");

        super.onStop();

        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying())
                    mediaPlayer.stop();

                mediaPlayer.release();

                mediaPlayer = null;
            }

            bRunning = false;

            if (SongThread != null) {
                SongThread = null;
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "onStop Error:" + e.getMessage());
        }
    }

    void ReadOSU()
    {
        int idx = m_Main.iSelectedSong;

        String osu_path = m_Main.songList.get(idx).osuPath;
        String song_path = m_Main.songList.get(idx).songPath;
        String image_path = m_Main.songList.get(idx).imagePath;

        setTitle(m_Main.strSelectedSongName);

        imageView.setImageBitmap(loadImageFromAssetsFile(getApplicationContext(), image_path));

        PlaySong(song_path);

        Log.i(TAG, "osuPath: " + osu_path);

        m_Main.rhythms = new ArrayList<Rhythm>();

        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(getAssets().open(osu_path)));

            String line = "";

            while (!line.contains("Mode:"))
            {
                line = reader.readLine();
            }

            int MODE = Integer.parseInt(line.split(":")[1].trim());

            Log.i(TAG, "MODE:" + MODE);

            while (!reader.readLine().equals("[HitObjects]"))
            {
            }

            /*
            if(MODE != 1)
            {
                Toast.makeText(getApplicationContext()
                        , "This Game Mode Is Not Supported"
                        , Toast.LENGTH_SHORT).show();
            }
            */

            //Adding Notes
            if(true) //(MODE == 1)
            {
                while ((line = reader.readLine()) != null)
                {
                    if (line.split(",")[3].split(":")[0].equals("5") || line.split(",")[3].split(":")[0].equals("1"))
                    {
                        int startTime = Integer.parseInt(line.split(",")[2]); //-540?
                        int type = Integer.parseInt(line.split(",")[4]);

                        m_Main.rhythms.add(new Rhythm(startTime, type));

                        Log.i(TAG, "NOTE:" + startTime + " " + type);
                    }
                }
            }
            else
            {
                Log.e(TAG, "This Game Mode Is Not Supported");
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "ReadOSU Error:" + e.getMessage());
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
                    Log.e(TAG, "ReadOSU Error2:" + e.getMessage());
                }
            }
        }
    }

    public static Bitmap loadImageFromAssetsFile(Context context, String fileName)
    {
        Bitmap image = null;
        AssetManager am = context.getAssets();

        try
        {
            InputStream is = am.open(fileName);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = true;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            image = BitmapFactory.decodeStream(is, null, options);
            is.close();

        }
        catch (IOException e)
        {
            Log.e(TAG, "loadImageFromAssetsFile Error:" + e.getMessage());
        }

        return image;
    }

    void PlaySong(String song_path)
    {
        Log.i(TAG, "PlaySong: " + song_path);

        try
        {
            AssetFileDescriptor afd = getAssets().openFd(song_path);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            mediaPlayer.prepare();

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {
                @Override
                public void onPrepared(MediaPlayer mp)
                {
                    if (mp == mediaPlayer)
                    {
                        Log.i(TAG, "PlaySong onPrepared");

                        mediaPlayer.start();
                    }
                }
            });

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    if (mp == mediaPlayer)
                    {
                        Log.i(TAG, "PlaySong onCompletion");

                        mediaPlayer.release();

                        finish();
                    }
                }
            });
        }
        catch (IOException e)
        {
            Log.e(TAG, "PlaySong:" + e.getMessage());
        }
    }

    Thread SongThread = new Thread(new Runnable()
    {
        public void run()
        {
            try
            {
                while(bRunning)
                {
                    Thread.sleep(50);

                    if(play_index < m_Main.rhythms.size() && mediaPlayer != null && mediaPlayer.isPlaying())
                    {
                        int audio_time = mediaPlayer.getCurrentPosition();

                        //Log.i(TAG, "SONG TIME: " + audio_time + " " + rhythms.get(play_index).startTime);

                        if (audio_time > m_Main.rhythms.get(play_index).startTime && m_Main.rhythms.get(play_index).startTime > 0)
                        {
                            Log.i(TAG, "ID: " + m_Main.rhythms.get(play_index).noteType);

                            if (m_Main.rhythms.get(play_index).noteType == Rhythm.NoteType.DON)
                            {
                                MainActivity.getInstance().SendCommand("0");
                            }
                            else if (m_Main.rhythms.get(play_index).noteType == Rhythm.NoteType.KAT)
                            {
                                MainActivity.getInstance().SendCommand("1");
                            }
                            else if (m_Main.rhythms.get(play_index).noteType == Rhythm.NoteType.BIGDON || m_Main.rhythms.get(play_index).noteType == Rhythm.NoteType.BIGKAT)
                            {
                                MainActivity.getInstance().SendCommand("2");
                            }

                            if (m_Main.rhythms.size() > play_index)
                                play_index++;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    });
}
