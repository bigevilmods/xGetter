package com.htetznaing.xgetterexample.Player;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import com.htetznaing.lowcostvideo.Model.XModel;
import com.htetznaing.xgetterexample.R;
import com.htetznaing.xgetterexample.Utils.XDownloader;

import java.util.HashMap;
import java.util.Map;

public class SimpleVideoPlayer extends AppCompatActivity {
    private boolean doubleBackToExitPressedOnce = false;
    private ExoPlayer player;
    private PlayerView playerView;
    private String url, cookie = null;
    private ProgressBar progressBar;
    private XDownloader xDownloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        getWindow().addFlags(128);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        if (getActionBar() != null) {
            getActionBar().hide();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Intent intent = getIntent();

        if (intent.getStringExtra("url") != null) {
            url = intent.getStringExtra("url");
        }

        if (intent.getStringExtra("cookie") != null) {
            cookie = intent.getStringExtra("cookie");
        }

        xDownloader = new XDownloader(this);
        xDownloader.OnDownloadFinishedListerner(new XDownloader.OnDownloadFinished() {
            @Override
            public void onCompleted(String path) {
                Toast.makeText(SimpleVideoPlayer.this, path, Toast.LENGTH_SHORT).show();
            }
        });

        if (url == null) {
            finish();
            Toast.makeText(this, "File Not Support!", Toast.LENGTH_SHORT).show();
        } else {
            if (url.startsWith("http")) {
                initApp();
            } else {
                initApp();
            }
        }
    }

    private void download() {
        XModel xModel = new XModel();
        xModel.setUrl(url);
        xModel.setCookie(cookie);
        xDownloader.download(xModel);
    }

    private void initApp() {
        playerView = findViewById(R.id.player_view);
        progressBar = findViewById(R.id.progresbar_video_play);
        progressBar.setVisibility(View.VISIBLE);

        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build();
        playerView.setPlayer(player);

        String userAgent = Util.getUserAgent(this, getResources().getString(R.string.app_name));
        DefaultDataSource.Factory dataSourceFactory;

        Map<String, String> requestProperties = new HashMap<>();
        requestProperties.put("Referer", "");

        if (cookie != null) {
            requestProperties.put("Cookie", cookie);
        }

        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(requestProperties);
        dataSourceFactory = new DefaultDataSource.Factory(this, httpDataSourceFactory);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);

        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                finish();
                Toast.makeText(SimpleVideoPlayer.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton rotate = findViewById(R.id.rotate);

        if (rotate != null) {
            rotate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int rotation = getWindowManager().getDefaultDisplay().getRotation();
                    if (rotation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                }
            });
        }

        ImageButton download = findViewById(R.id.download);
        if (download != null) {
            if (!url.startsWith("http")) {
                download.setVisibility(View.GONE);
                download.setEnabled(false);
            }
            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    download();
                }
            });
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Click BACK again to EXIT!", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onPause() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.release();
        }
        super.onDestroy();
    }
}
