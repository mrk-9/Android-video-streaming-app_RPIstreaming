package com.tom.rpistreaming;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

public class StreamingActivity extends AppCompatActivity implements IVLCVout.Callback, LibVLC.HardwareAccelerationError{
    private static final String TAG = StreamingActivity.class.getSimpleName();

    FFmpeg ffmpeg;

    private View view;
    private boolean stillWatching;

    private boolean isRecording;

    // display surface
    private SurfaceView mSurface;
    private SurfaceHolder holder;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private final static int VideoSizeChanged = -1;

    private View progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);

        mSurface = (SurfaceView)findViewById(R.id.player_surface);
        progressBar = (ProgressBar)findViewById(R.id.progressBar1);

        isRecording = false;
        String ip_str = null;

        try {
            Context context = this;
            String fpath = context.getFilesDir().getPath().toString()+"/rpistreaming.txt";
            File file = new File(fpath);
            if(!file.exists()){
                file.createNewFile();
                ip_str = "http://172.24.1.1:8080/stream/video.h264";
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(ip_str);
                bw.close();
            }
            BufferedReader br = new BufferedReader(new FileReader(fpath));
            ip_str = br.readLine();

        } catch (IOException e) {
            e.printStackTrace();
        }
        ((TextView)findViewById(R.id.txtStreamUrl)).setText(ip_str);

        createPlayer(Uri.parse(ip_str));
    }

    private void createPlayer(Uri media) {
        releasePlayer();
        try {
            progressBar.setVisibility(View.VISIBLE);
            // Create LibVLC
            ArrayList<String> options = new ArrayList<String>();
//            options.add("--subsdec-encoding <encoding>");
            options.add(":network-caching=300");
            options.add(":live-caching=0");
            options.add("--demux=h264");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity

//            options.add("--sout=file/ps:"+file_url);
//            options.add("--sout=#transcode{acodec=none}:file{dst="+file_url+",no-overwrite}");
//            options.add(":sout-keep");

            libvlc = new LibVLC(options);

            libvlc.setOnHardwareAccelerationError(this);
            holder = mSurface.getHolder();
            holder.setKeepScreenOn(true);

            // Create media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Set up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
//            vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libvlc, media);
            m.setHWDecoderEnabled(false, true);
            mMediaPlayer.setMedia(m);

            mMediaPlayer.play();
            ((Button)findViewById(R.id.btnStop)).setText("Stop Play");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO: handle this cleaner
    public void releasePlayer() {
        if (libvlc == null)
            return;

        if(stillWatching){
            mMediaPlayer.stop();
            mMediaPlayer.setTime(0);
            mMediaPlayer.play();
            return;
        }
        ((Button)findViewById(R.id.btnStop)).setText("Play");
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;
        progressBar.setVisibility(View.GONE);

        mVideoWidth = 0;
        mVideoHeight = 0;

    }

    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if(holder == null || mSurface == null)
            return;

        // get screen size
        int w = this.getWindow().getDecorView().getWidth();
        int h = this.getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        holder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    public void error()   {
        AlertDialog alertDialog = new AlertDialog.Builder(StreamingActivity.this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage("Unable to play this streaming url.");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        alertDialog.show();
    }

    public void onReplay(View view){
        String ip_str = null;

        try {
            StringBuffer output = new StringBuffer();
            Context context = this;
            String fpath = context.getFilesDir().getPath().toString()+"/rpistreaming.txt";
            File file = new File(fpath);
            BufferedReader br = new BufferedReader(new FileReader(fpath));
            ip_str = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //playVideo("rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov");
        createPlayer(Uri.parse(ip_str));

    }
    public void onClickPlay(View view) {
        if(mMediaPlayer != null){
            if(mMediaPlayer.isPlaying()) {
                ((Button)findViewById(R.id.btnStop)).setText("Play");
                mMediaPlayer.pause();
            }
            else{
                ((Button)findViewById(R.id.btnStop)).setText("Stop Play");
                mMediaPlayer.play();
            }
        }
    }
    public void onClickRecord(View view) {
        if(mMediaPlayer != null){
            if(!isRecording) {
                String stream_url = ((TextView) findViewById(R.id.txtStreamUrl)).getText().toString();
                final File dir = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/rpistreaming");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy-hhmmss");
                String file_url = dir.getAbsolutePath() + "/" + sdf.format(new Date()) + ".mp4";
                String[] command = {"-y", "-i", stream_url, "-c:v", "libx264", "-preset", "ultrafast", "-strict", "-2", "-s", "720*1280", "-aspect", "16:9", file_url};

                ffmpeg = FFmpeg.getInstance(this.getApplicationContext());
                loadFFMpegBinary();
                if (command.length != 0) {
                    execFFmpegBinary(command);
                }
                isRecording = true;
                ((Button)findViewById(R.id.btnRecord)).setText("Stop Record");
            }
            else{
                isRecording = false;
                ((Button)findViewById(R.id.btnRecord)).setText("Record");
                ffmpeg.killRunningProcesses();
                ffmpeg = null;
            }
        }
    }

    /*************
     * Events
     *************/

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

    public void setTime(int progress) {
        mMediaPlayer.setTime(progress * 100);
    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<StreamingActivity> mOwner;

        public MyPlayerListener(StreamingActivity owner) {
            mOwner = new WeakReference<StreamingActivity>(owner);
        }

        private String fromMillisToTime(long millis){
            return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            StreamingActivity player = mOwner.get();

            switch(event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();

                    break;
                case MediaPlayer.Event.TimeChanged:
                    long currentTime = player.mMediaPlayer.getTime();

                    break;
                case MediaPlayer.Event.Playing:
                    long time = player.mMediaPlayer.getLength();
                    player.progressBar.setVisibility(View.GONE);
                    break;
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:

                default:
                    break;
            }
        }
    }

    @Override
    public void eventHardwareAccelerationError() {
        // Handle errors with hardware acceleration
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Not supported")
                .setMessage("Device is not supported")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create()
                .show();

    }
    private void loadFFMpegBinary() {
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        }
    }
    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "FAILED with output : "+s);
                }

                @Override
                public void onSuccess(String s) {
                    Log.d(TAG, "SUCCESS with output : "+s);
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg "+command);
                    Log.d(TAG, "progress : "+s);
//                    progressDialog.setMessage("Processing\n"+s);
                }

                @Override
                public void onStart() {
//                    outputLayout.removeAllViews();

                    Log.d(TAG, "Started command : ffmpeg " + command);
//                    progressDialog.setMessage("Processing...");
//                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg "+command);
//                    progressDialog.dismiss();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }
}
