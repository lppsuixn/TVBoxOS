package com.github.tvbox.osc.ui.fragment;

import static xyz.doikki.videoplayer.util.PlayerUtils.getWindowManager;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.Subtitle;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.CacheManager;
import com.github.tvbox.osc.danmu.BiliDanmukuParser;
import com.github.tvbox.osc.danmu.ChatBroadcastWsClient;
import com.github.tvbox.osc.danmu.GiftDanmakuManager;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.player.IjkMediaPlayer;
import com.github.tvbox.osc.player.MyVideoView;
import com.github.tvbox.osc.player.TrackInfo;
import com.github.tvbox.osc.player.TrackInfoBean;
import com.github.tvbox.osc.player.controller.VodController;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.SearchSubtitleDialog;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.dialog.SubtitleDialog;
import com.github.tvbox.osc.util.AdBlocker;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.VideoParseRuler;
import com.github.tvbox.osc.util.XWalkUtils;
import com.github.tvbox.osc.util.thunder.Jianpian;
import com.github.tvbox.osc.util.thunder.Thunder;
import com.github.tvbox.osc.util.urlhttp.CallBackUtil;
import com.github.tvbox.osc.util.urlhttp.UrlHttpUtil;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.Response;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.core.XWalkJavascriptResult;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;
import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import me.jessyan.autosize.AutoSize;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;
import xyz.doikki.videoplayer.player.AbstractPlayer;
import xyz.doikki.videoplayer.player.ProgressManager;

public class PlayFragment extends BaseLazyFragment {
    private MyVideoView mVideoView;
    private TextView mPlayLoadTip;
    private ImageView mPlayLoadErr;
    private ProgressBar mPlayLoading;
    private VodController mController;
    private SourceViewModel sourceViewModel;
    private Handler mHandler;
    private boolean showDanmaku;

    private IDanmakuView danmakuView;

    private DanmakuContext danmakuContext;
    private long videoDuration = -1;
    private GiftDanmakuManager giftDanmakuManager;
    private ChatBroadcastWsClient chatBroadcastWsClient;
    private int scDanmakuType = BaseDanmaku.TYPE_FIX_BOTTOM;
    BaseDanmakuParser danmakuParser;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_play;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE) {
            mController.mSubtitleView.setTextSize((int) event.obj);
        }
    }

    @Override
    protected void init() {
        initView();
        initViewModel();
        initData();
    }


    /**
     * 创建解析器，解析输入流
     */
    private BaseDanmakuParser createParser(InputStream stream) {
        if (stream == null) {
            return new BaseDanmakuParser() {
                @Override
                protected Danmakus parse() {
                    return new Danmakus();
                }
            };
        }
        //A站是Json格式
        ILoader loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);
        try {
            loader.load(stream);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        BaseDanmakuParser parser = new BiliDanmukuParser();
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
        return parser;

    }

    public long getSavedProgress(String url) {
        int st = 0;
        try {
            st = mVodPlayerCfg.getInt("st");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        long skip = st * 1000L;
        Object theCache=CacheManager.getCache(MD5.string2MD5(url));
        if (theCache == null) {
            return skip;
        }
        long rec = 0;
        if (theCache instanceof Long) {
            rec = (Long) theCache;
        } else if (theCache instanceof String) {
            try {
                rec = Long.parseLong((String) theCache);
            } catch (NumberFormatException e) {
                System.out.println("String value is not a valid long.");
            }
        } else {
            System.out.println("Value cannot be converted to long.");
        }
        return Math.max(rec, skip);
    }

    private void initView() {
        EventBus.getDefault().register(this);
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case 100:
                        stopParse();
                        errorWithRetry("嗅探错误", false);
                        break;
                }
                return false;
            }
        });
        mVideoView = findViewById(R.id.mVideoView);
        mPlayLoadTip = findViewById(R.id.play_load_tip);
        mPlayLoading = findViewById(R.id.play_loading);
        mPlayLoadErr = findViewById(R.id.play_load_error);
        mController = new VodController(requireContext());
        mController.setCanChangePosition(true);
        mController.setEnableInNormal(true);
        mController.setGestureEnabled(true);
        ProgressManager progressManager = new ProgressManager() {
            @Override
            public void saveProgress(String url, long progress) {
                CacheManager.save(MD5.string2MD5(url), progress);
            }

            @Override
            public long getSavedProgress(String url) {
                return PlayFragment.this.getSavedProgress(url);
            }
        };
        mVideoView.setProgressManager(progressManager);
        mController.setListener(new VodController.VodControlListener() {
            @Override
            public void playNext(boolean rmProgress) {
                String preProgressKey = progressKey;
                PlayFragment.this.playNext(rmProgress);
                if (rmProgress && preProgressKey != null)
                    CacheManager.delete(MD5.string2MD5(preProgressKey), 0);
            }

            @Override
            public void playPre() {
                PlayFragment.this.playPrevious();
            }

            @Override
            public void changeParse(ParseBean pb) {
                autoRetryCount = 0;
                doParse(pb);
            }

            @Override
            public void updatePlayerCfg() {
                mVodInfo.playerCfg = mVodPlayerCfg.toString();
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_REFRESH, mVodPlayerCfg));
            }

            @Override
            public void replay(boolean replay) {
                autoRetryCount = 0;
                play(replay);
            }

            @Override
            public void errReplay() {
                errorWithRetry("视频播放出错", false);
            }

            @Override
            public void selectSubtitle() {
                try {
                    selectMySubtitle();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void selectAudioTrack() {
                selectMyAudioTrack();
            }

            @Override
            public void toggleDanmu() {
                if(danmakuView != null && danmakuView.isPrepared() && danmakuView.isShown()) {
                    danmakuView.hide();
                }else if(danmakuView != null && danmakuView.isPrepared() && !danmakuView.isShown()) {
                    danmakuView.show();
                }

            }

            @Override
            public void prepared() {
                initSubtitleView();
            }
        });
        mVideoView.setVideoController(mController);
        //设置最大显示行数
        HashMap<Integer, Integer> maxLInesPair = new HashMap<>(16);
        maxLInesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 6);
        //设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<>(16);
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
        String danmuSizeConfig = Hawk.get(HawkConfig.DANMU_SIZE,"标准");
        Float danmuSize = 1.2f;
        if("大".equalsIgnoreCase(danmuSizeConfig)){
            danmuSize = 1.8f;
        }else if("超大".equalsIgnoreCase(danmuSizeConfig)){
            danmuSize = 2.5f;
        }
        danmakuContext = DanmakuContext.create();
        danmakuContext.setDuplicateMergingEnabled(false)
                .setScrollSpeedFactor(1.2f)
                //设置文字的比例
                .setScaleTextSize(danmuSize)
                //设置显示最大行数
                .setMaximumLines(maxLInesPair)
                //设置防，null代表可以重叠
                .preventOverlapping(overlappingEnablePair);
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            Display display = getWindowManager(mContext).getDefaultDisplay();
            float refreshRate = display.getRefreshRate();
            int rate = (int) (1000 / refreshRate);
            danmakuContext.setmUpdateRate(rate);
        }

    }
    void initDanmuView(){
        danmakuView = (IDanmakuView) findViewById(R.id.danmakuView);
        danmakuView.enableDanmakuDrawingCache(true);
        danmakuView.setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                showDanmaku = true;
                danmakuView.start();
            }

            @Override
            public void updateTimer(DanmakuTimer timer) {
                if (Math.abs(mVideoView.getSpeed()) - 1.0 > 1e-6) {
                    timer.add((long) (timer.lastInterval() * (mVideoView.getSpeed() - 1)));
                }
            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {
            }

            @Override
            public void drawingFinished() {

            }
        });
        mVideoView.setDanmuView(danmakuView);
    }

    public void initLiveDanmu(){
        danmakuParser = new BaseDanmakuParser() {
            @Override
            protected IDanmakus parse() {
                return new Danmakus();
            }
        };
        danmakuView = (IDanmakuView) findViewById(R.id.danmakuView);
        danmakuView.enableDanmakuDrawingCache(true);
        danmakuView.show();
        danmakuView.setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                danmakuView.start();
            }

            @Override
            public void updateTimer(DanmakuTimer danmakuTimer) {
            }

            @Override
            public void danmakuShown(BaseDanmaku baseDanmaku) {
            }

            @Override
            public void drawingFinished() {
            }
        });
        mVideoView.setDanmuView(danmakuView);
    }
    private void initChatBroadcast(Long id,String token){
        if(chatBroadcastWsClient != null){
            chatBroadcastWsClient.close();
            chatBroadcastWsClient = null;
        }
        chatBroadcastWsClient = new ChatBroadcastWsClient(id, token);
        chatBroadcastWsClient.setCallBack(new ChatBroadcastWsClient.CallBack() {
            @Override
            public void onStart() {
            }

            @Override
            public void onReceiveDanmu(String text, float textSize, int textColor, boolean textShadowTransparent, String msg) {
                addDanmaku(text, textSize, textColor, textShadowTransparent);
            }

            @Override
            public void onReceiveSuperChatMessage(String message, String messageFontColor, String uname, String msg) {
                if(scDanmakuType == BaseDanmaku.TYPE_FIX_BOTTOM
                        || scDanmakuType == BaseDanmaku.TYPE_FIX_TOP
                        || scDanmakuType == BaseDanmaku.TYPE_SCROLL_RL
                        || scDanmakuType == BaseDanmaku.TYPE_SCROLL_LR
                        || scDanmakuType == BaseDanmaku.TYPE_SPECIAL) {
                    String text = uname + ":" + message;
                    int color;
                    if(Strings.isNullOrEmpty(message)){
                        color = Color.WHITE;
                    }else{
                        color = Color.parseColor(messageFontColor);
                    }
                    addDanmaku(text, 25, color, false, scDanmakuType);
                }
            }

            @Override
            public void onReceiveSendGift(String action, String giftName, Integer num, String uname, String msg) {

            }

            @Override
            public void onReceiveOtherMessage(String message) {
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {

            }
        });
        startChatBroadcastWsClient();
    }

    private void startChatBroadcastWsClient(){
        if(chatBroadcastWsClient != null){
            try {
                chatBroadcastWsClient.start();
            }
            catch (Exception error){
                error.printStackTrace();
                System.out.println(error.getMessage());
            }
        }
    }

    private void addDanmaku(String content, float textSize, int textColor, boolean textShadowTransparent) {
        addDanmaku(content, textSize, textColor, textShadowTransparent, BaseDanmaku.TYPE_SCROLL_RL);
    }

    private void addDanmaku(String content, float textSize, int textColor, boolean textShadowTransparent, int type){
        if(danmakuContext != null  && danmakuView.isPrepared()){
            BaseDanmaku danmaku = danmakuContext.mDanmakuFactory.createDanmaku(type);
            if (danmaku != null) {
                textSize = textSize * (danmakuParser.getDisplayer().getDensity() - 0.6f);
                danmaku.text = content;
                if(type == BaseDanmaku.TYPE_FIX_BOTTOM){
                    int width = danmakuContext.getDisplayer().getWidth();
                    int maxSize = (int) (width / textSize) - 10;
                    int textLength = content.length();
                    int lineLength = (textLength + maxSize - 1) / maxSize;
                    String[] lines = new String[lineLength];
                    for(int i = 0; i < lineLength; i++){
                        lines[i] = content.substring(i * maxSize, Math.min(i * maxSize + maxSize, textLength));
                    }
                    danmaku.lines = lines;
                }
                danmaku.padding = 0;
                danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
                danmaku.isLive = true;
                danmaku.setTime(danmakuView.getCurrentTime() + 500);
                danmaku.textSize = textSize;
                danmaku.textColor = textColor;
                danmaku.textShadowColor = textShadowTransparent ? Color.TRANSPARENT : Color.BLACK;
                danmakuView.addDanmaku(danmaku);
            }
        }
    }

    //设置字幕
    void setSubtitle(String path) {
        if (path != null && path .length() > 0) {
            // 设置字幕
            mController.mSubtitleView.setVisibility(View.GONE);
            mController.mSubtitleView.setSubtitlePath(path);
            mController.mSubtitleView.setVisibility(View.VISIBLE);
        }
    }

    void selectMySubtitle() throws Exception {
        SubtitleDialog subtitleDialog = new SubtitleDialog(getActivity());
        int playerType = mVodPlayerCfg.getInt("pl");
        if (mController.mSubtitleView.hasInternal && playerType == 1) {
            subtitleDialog.selectInternal.setVisibility(View.VISIBLE);
        } else {
            subtitleDialog.selectInternal.setVisibility(View.GONE);
        }
        subtitleDialog.setSubtitleViewListener(new SubtitleDialog.SubtitleViewListener() {
            @Override
            public void setTextSize(int size) {
                mController.mSubtitleView.setTextSize(size);
            }
            @Override
            public void setSubtitleDelay(int milliseconds) {
                mController.mSubtitleView.setSubtitleDelay(milliseconds);
            }
            @Override
            public void selectInternalSubtitle() {
                selectMyInternalSubtitle();
            }
            @Override
            public void setTextStyle(int style) {
                setSubtitleViewTextStyle(style);
            }
        });
        subtitleDialog.setSearchSubtitleListener(new SubtitleDialog.SearchSubtitleListener() {
            @Override
            public void openSearchSubtitleDialog() {
                SearchSubtitleDialog searchSubtitleDialog = new SearchSubtitleDialog(getActivity());
                searchSubtitleDialog.setSubtitleLoader(new SearchSubtitleDialog.SubtitleLoader() {
                    @Override
                    public void loadSubtitle(Subtitle subtitle) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String zimuUrl = subtitle.getUrl();
                                LOG.i("Remote Subtitle Url: " + zimuUrl);
                                setSubtitle(zimuUrl);//设置字幕
                                searchSubtitleDialog.dismiss();
                            }
                        });
                    }
                });
                if(mVodInfo.playFlag.contains("Ali")||mVodInfo.playFlag.contains("parse")){
                    searchSubtitleDialog.setSearchWord(mVodInfo.playNote);
                }else {
                    searchSubtitleDialog.setSearchWord(mVodInfo.name);
                }
                searchSubtitleDialog.show();
            }
        });
        subtitleDialog.setLocalFileChooserListener(new SubtitleDialog.LocalFileChooserListener() {
            @Override
            public void openLocalFileChooserDialog() {
                new ChooserDialog(getActivity())
                        .withFilter(false, false, "srt", "ass", "scc", "stl", "ttml")
                        .withStartFile("/storage/emulated/0/Download")
                        .withChosenListener(new ChooserDialog.Result() {
                            @Override
                            public void onChoosePath(String path, File pathFile) {
                                LOG.i("Local Subtitle Path: " + path);
                                setSubtitle(path);//设置字幕
                            }
                        })
                        .build()
                        .show();
            }
        });
        subtitleDialog.show();
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    void setSubtitleViewTextStyle(int style) {
        if (style == 0) {
            mController.mSubtitleView.setTextColor(getContext().getResources().getColorStateList(R.color.color_FFFFFF));
        } else if (style == 1) {
            mController.mSubtitleView.setTextColor(getContext().getResources().getColorStateList(R.color.color_FFB6C1));
        }
    }

    void selectMyAudioTrack() {
        AbstractPlayer mediaPlayer = mVideoView.getMediaPlayer();
        if (!(mediaPlayer instanceof IjkMediaPlayer)) {
            return;
        }
        TrackInfo trackInfo = null;
        trackInfo = ((IjkMediaPlayer)mediaPlayer).getTrackInfo();
        if (trackInfo == null) {
            Toast.makeText(mContext, "没有音轨", Toast.LENGTH_SHORT).show();
            return;
        }
        List<TrackInfoBean> bean = trackInfo.getAudio();
        if (bean.size() < 1) return;
        SelectDialog<TrackInfoBean> dialog = new SelectDialog<>(getActivity());
        dialog.setTip("切换音轨");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<TrackInfoBean>() {
            @Override
            public void click(TrackInfoBean value, int pos) {
                try {
                    for (TrackInfoBean audio : bean) {
                        audio.selected = audio.index == value.index;
                    }
                    mediaPlayer.pause();
                    long progress = mediaPlayer.getCurrentPosition();//保存当前进度，ijk 切换轨道 会有快进几秒
                    ((IjkMediaPlayer)mediaPlayer).setTrack(value.index);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mediaPlayer.seekTo(progress);
                            mediaPlayer.start();
                        }
                    }, 800);
                    dialog.dismiss();
                } catch (Exception e) {
                    LOG.e("切换音轨出错");
                }
            }

            @Override
            public String getDisplay(TrackInfoBean val) {
                String name = val.name.replace("AUDIO,", "");
                name = name.replace("N/A,", "");
                name = name.replace(" ", "");
                return val.index + " : " + val.language + " : " + name;
            }
        }, new DiffUtil.ItemCallback<TrackInfoBean>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull TrackInfoBean oldItem, @NonNull @NotNull TrackInfoBean newItem) {
                return oldItem.index == newItem.index;
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull TrackInfoBean oldItem, @NonNull @NotNull TrackInfoBean newItem) {
                return oldItem.index == newItem.index;
            }
        }, bean, trackInfo.getAudioSelected(false));
        dialog.show();
    }

    void selectMyInternalSubtitle() {
        AbstractPlayer mediaPlayer = mVideoView.getMediaPlayer();
        if (!(mediaPlayer instanceof IjkMediaPlayer)) {
            return;
        }
        TrackInfo trackInfo = null;
        trackInfo = ((IjkMediaPlayer)mediaPlayer).getTrackInfo();
        if (trackInfo == null) {
            Toast.makeText(mContext, "没有内置字幕", Toast.LENGTH_SHORT).show();
            return;
        }
        List<TrackInfoBean> bean = trackInfo.getSubtitle();
        if (bean.size() < 1) return;
        SelectDialog<TrackInfoBean> dialog = new SelectDialog<>(getActivity());
        dialog.setTip("切换内置字幕");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<TrackInfoBean>() {
            @Override
            public void click(TrackInfoBean value, int pos) {
                try {
                    for (TrackInfoBean subtitle : bean) {
                        subtitle.selected = subtitle.index == value.index;
                    }
                    mediaPlayer.pause();
                    long progress = mediaPlayer.getCurrentPosition();//保存当前进度，ijk 切换轨道 会有快进几秒
                    mController.mSubtitleView.destroy();
                    mController.mSubtitleView.clearSubtitleCache();
                    mController.mSubtitleView.isInternal = true;
                    ((IjkMediaPlayer)mediaPlayer).setTrack(value.index);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mediaPlayer.seekTo(progress);
                            mediaPlayer.start();
                        }
                    }, 800);
                    dialog.dismiss();
                } catch (Exception e) {
                    LOG.e("切换内置字幕出错");
                }
            }

            @Override
            public String getDisplay(TrackInfoBean val) {
                return val.index + " : " + val.language;
            }
        }, new DiffUtil.ItemCallback<TrackInfoBean>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull TrackInfoBean oldItem, @NonNull @NotNull TrackInfoBean newItem) {
                return oldItem.index == newItem.index;
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull TrackInfoBean oldItem, @NonNull @NotNull TrackInfoBean newItem) {
                return oldItem.index == newItem.index;
            }
        }, bean, trackInfo.getSubtitleSelected(false));
        dialog.show();
    }

    void setTip(String msg, boolean loading, boolean err) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(new Runnable() { //影魔
            @Override
            public void run() {
                mPlayLoadTip.setText(msg);
                mPlayLoadTip.setVisibility(View.VISIBLE);
                mPlayLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
                mPlayLoadErr.setVisibility(err ? View.VISIBLE : View.GONE);
            }
        });
    }

    void hideTip() {
        mPlayLoadTip.setVisibility(View.GONE);
        mPlayLoading.setVisibility(View.GONE);
        mPlayLoadErr.setVisibility(View.GONE);
    }

    void errorWithRetry(String err, boolean finish) {
        if (!autoRetry()) {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (finish) {
                        Toast.makeText(mContext, err, Toast.LENGTH_SHORT).show();
                    } else {
                        setTip(err, false, true);
                    }
                }
            });
        }
    }

    void playUrl(String url, HashMap<String, String> headers) {
        LOG.i("playUrl:" + url);
        if(autoRetryCount>0 && url.contains(".m3u8")){
            url="http://home.jundie.top:666/unBom.php?m3u8="+url;//尝试去bom头再次播放
        }
        if (mActivity == null) return;
        if (!isAdded()) return;
        final String finalUrl = url;
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopParse();
                if (mVideoView != null) {
                    mVideoView.release();
                    if (finalUrl != null) {
                        String url = finalUrl;
                        try {
                            int playerType = mVodPlayerCfg.getInt("pl");
                            if (playerType >= 10) {
                                VodInfo.VodSeries vs = mVodInfo.seriesMap.get(mVodInfo.playFlag).get(mVodInfo.playIndex);
                                String playTitle = mVodInfo.name + " " + vs.name;
                                setTip("调用外部播放器" + PlayerHelper.getPlayerName(playerType) + "进行播放", true, false);
                                boolean callResult = false;
                                long progress = getSavedProgress(progressKey);
                                callResult = PlayerHelper.runExternalPlayer(playerType, requireActivity(), url, playTitle, playSubtitle, headers, progress);
                                setTip("调用外部播放器" + PlayerHelper.getPlayerName(playerType) + (callResult ? "成功" : "失败"), callResult, !callResult);
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        hideTip();
                        if (url.startsWith("data:application/dash+xml;base64,")) {
                            PlayerHelper.updateCfg(mVideoView, mVodPlayerCfg, 2);
                            App.getInstance().setDashData(url.split("base64,")[1]);
                            url = ControlManager.get().getAddress(true) + "dash/proxy.mpd";
                        } else if (url.contains(".mpd") || url.contains("type=mpd")) {
                            PlayerHelper.updateCfg(mVideoView, mVodPlayerCfg, 2);
                        } else {
                            PlayerHelper.updateCfg(mVideoView, mVodPlayerCfg);
                        }
                        mVideoView.setProgressKey(progressKey);
                        if (headers != null) {
                            mVideoView.setUrl(url, headers);
                        } else {
                            mVideoView.setUrl(url);
                        }
                        mVideoView.start();
                        mController.resetSpeed();
                        initDanmuSpider();
                    }
                }
            }
        });
    }

    // 解压缩字节流
    private static byte[] decompress(InputStream is) {
        InflaterInputStream iis = new InflaterInputStream(is);
        ByteArrayOutputStream o = new ByteArrayOutputStream(1024);
        try {
            int i = 1024;
            byte[] buf = new byte[i];
            while ((i = iis.read(buf, 0, i)) > 0) {
                o.write(buf, 0, i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return o.toByteArray();
    }

    private void initDanmuSpider() {
        try {
            String playUrl = mVodInfo.seriesMap.get(mVodInfo.playFlag).get(mVodInfo.playIndex).url;
            if (playUrl != null && playUrl.contains("iqiyi.com")) {
                initLiveDanmu();
                UrlHttpUtil.get(playUrl.replace("http", "https"), new CallBackUtil.CallBackString() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        System.out.println(errorMessage);
                    }

                    @Override
                    public void onResponse(String response) {
                        new Thread(() -> {
                            String tvId = null;
                            int duration = 0;
                            Pattern pattern = Pattern.compile("albumData: (\\{.+\\})");
                            Matcher m = pattern.matcher(response);
                            if (m.find()) {
                                String videoListStr = m.group(1);
                                JsonObject iqiyiVodList = new Gson().fromJson(videoListStr, JsonObject.class);
                                JsonArray videoList = iqiyiVodList.get("list").getAsJsonArray();
                                for (int i = 0; i < videoList.size(); i++) {
                                    JsonObject video = videoList.get(i).getAsJsonObject();
                                    if (playUrl.equalsIgnoreCase(video.get("url").getAsString())) {
                                        tvId = video.get("tvId").getAsString();
                                        duration = video.get("duration").getAsInt();
                                        break;
                                    }
                                }
                            } else {
                                pattern = Pattern.compile("playPageInfo = (\\{.+\\})");
                                m = pattern.matcher(response);
                                if (m.find()) {
                                    String videoListStr = m.group(1);
                                    if(videoListStr != null) {
                                        JsonObject video = new Gson().fromJson(videoListStr.split("\\|")[0], JsonObject.class);
                                        if(video != null) {
                                            tvId = video.get("tvId").getAsString();
                                            duration = video.get("duration").getAsInt();
                                        }
                                    }
                                }
                            }
                            if (tvId != null && duration > 0) {
                                for (int d = 1; d <= duration / 300 + 1; d++) {
                                    //https://cmts.iqiyi.com/bullet/35/00/3185901936393500_300_1.z
                                    UrlHttpUtil.syncGet("https://cmts.iqiyi.com/bullet/" + tvId.substring(tvId.length() - 4, tvId.length() - 2)
                                            + "/" + tvId.substring(tvId.length() - 2) + "/" + tvId + "_300_" + d + ".z", new CallBackUtil.CallBackStream() {
                                        @Override
                                        public void onFailure(int code, String errorMessage) {
                                            System.out.println(errorMessage);
                                        }

                                        @Override
                                        public void onResponse(InputStream response) {
                                            new Thread(() -> {
                                                XmlToJson xmlToJson = new XmlToJson.Builder(new InflaterInputStream(response), null)
                                                        .build();
                                                JSONObject iqiyiDmJson = xmlToJson.toJson();
                                                try {
                                                    JSONArray dmArray = iqiyiDmJson.getJSONObject("danmu").getJSONObject("data").getJSONArray("entry");
                                                    for (int i = 0; i < dmArray.length(); i++) {
                                                        JSONArray bulletList = dmArray.getJSONObject(i).getJSONObject("list").getJSONArray("bulletInfo");
                                                        for (int j = 0; j < bulletList.length(); j++) {
                                                            addSimpleDanmaku(bulletList.getJSONObject(j).getString("content"), bulletList.getJSONObject(j).getLong("showTime") * 1000);
                                                        }
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }).start();
                                        }

                                    });
                                }
                            }
                        }).start();
                    }
                });
                danmakuView.prepare(danmakuParser, danmakuContext);
            }
            if (playUrl != null && playUrl.contains("v.qq.com")) {
                initLiveDanmu();
                String qqVid = playUrl.split("/")[playUrl.split("/").length - 1].replace(".html", "");
                UrlHttpUtil.get("https://dm.video.qq.com/barrage/base/" + qqVid, new CallBackUtil.CallBackString() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        System.out.println(errorMessage);
                    }

                    @Override
                    public void onResponse(String response) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                JsonObject danmuBaseInfo = new Gson().fromJson(response, JsonObject.class);
                                JsonObject segmentIndex = danmuBaseInfo.getAsJsonObject("segment_index");
                                for (String key : segmentIndex.keySet()) {
                                    String danmuUrl = "https://dm.video.qq.com/barrage/segment/" + qqVid + "/" + segmentIndex.getAsJsonObject(key).get("segment_name").getAsString();
                                    UrlHttpUtil.syncGet(danmuUrl, new CallBackUtil.CallBackString() {
                                        @Override
                                        public void onFailure(int code, String errorMessage) {
                                        }

                                        @Override
                                        public void onResponse(String danmuListStr) {
                                            JsonObject danmuList = new Gson().fromJson(danmuListStr, JsonObject.class);
                                            JsonArray barrageList = danmuList.get("barrage_list").getAsJsonArray();
                                            for (int i = 0; i < barrageList.size(); i++) {
                                                JsonObject danmuItem = barrageList.get(i).getAsJsonObject();
                                                addSimpleDanmaku(danmuItem.get("content").getAsString(), danmuItem.get("time_offset").getAsLong());
                                            }
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                });
                danmakuView.prepare(danmakuParser, danmakuContext);
            }
            //芒果tv
            //https://galaxy.bz.mgtv.com/getctlbarrage?version=3.0.0&vid=17808762&abroad=0&pid=0&os=&uuid=&deviceid=5357deed-bf12-4926-95a9-3d5c01caac22&cid=412705&ticket=&mac=&platform=0&appVersion=3.0.0&reqtype=form-post&callback=&allowedRC=1
            if (playUrl != null && playUrl.contains("mgtv.com")) {
                initLiveDanmu();
                String mgcid = playUrl.split("/")[playUrl.split("/").length - 2].replace(".html", "");
                String mgvid = playUrl.split("/")[playUrl.split("/").length - 1].replace(".html", "");
                UrlHttpUtil.get("https://galaxy.bz.mgtv.com/getctlbarrage?version=3.0.0&vid=" + mgvid
                        + "&abroad=0&pid=0&os=&uuid=&deviceid=" + UUID.randomUUID() + "&cid=" + mgcid + "&ticket=&mac=&platform=0&appVersion=3.0.0&reqtype=form-post&callback=&allowedRC=1", new CallBackUtil.CallBackString() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        System.out.println(errorMessage);
                    }

                    @Override
                    public void onResponse(String danmuInfoStr) {
                        JsonObject danmuBaseInfo = new Gson().fromJson(danmuInfoStr, JsonObject.class);
                        if (danmuBaseInfo.get("data").getAsJsonObject().has("cdn_version")) {
                            String danmuUrlBase = "https://bullet-ali.hitv.com/" +
                                    danmuBaseInfo.get("data").getAsJsonObject().get("cdn_version").getAsString() + "/";

                            UrlHttpUtil.get("https://pcweb.api.mgtv.com/player/vinfo?video_id=" + mgvid
                                    + "&cid=&pid=&cxid=&_support=10000000&allowedRC=1&_support=10000000", new CallBackUtil.CallBackString() {
                                @Override
                                public void onFailure(int code, String errorMessage) {
                                }

                                @Override
                                public void onResponse(String vodInfoStr) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            JsonObject vodIndo = new Gson().fromJson(vodInfoStr, JsonObject.class);
                                            int duration = vodIndo.get("data").getAsJsonObject().get("duration").getAsInt();
                                            for (int i = 0; i < duration / 60; i++) {
                                                String danmuUrl = danmuUrlBase + i + ".json";
                                                UrlHttpUtil.syncGet(danmuUrl, new CallBackUtil.CallBackString() {
                                                    @Override
                                                    public void onFailure(int code, String errorMessage) {
                                                    }

                                                    @Override
                                                    public void onResponse(String danmuListStr) {
                                                        JsonObject danmuList = new Gson().fromJson(danmuListStr, JsonObject.class);
                                                        if (danmuList.get("data").getAsJsonObject().get("total").getAsInt() > 0) {
                                                            JsonArray barrageList = danmuList.get("data").getAsJsonObject().get("items").getAsJsonArray();
                                                            for (int j = 0; j < barrageList.size(); j++) {
                                                                JsonObject danmuItem = barrageList.get(j).getAsJsonObject();
                                                                addSimpleDanmaku(danmuItem.get("content").getAsString(), danmuItem.get("time").getAsLong());
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    }).start();
                                }
                            });
                        }
                    }
                });
                danmakuView.prepare(danmakuParser, danmakuContext);
            }
            if ("bilidanmu".equalsIgnoreCase(mVodInfo.area)) {
                initDanmuView();
                String cid = playUrl.split("_")[1];
                UrlHttpUtil.get("https://comment.bilibili.com/" + cid + ".xml", new CallBackUtil.CallBackStream() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                    }

                    @Override
                    public void onResponse(InputStream response) {
                        danmakuView.prepare(createParser(new InflaterInputStream(response, new Inflater(true))), danmakuContext);
                    }
                });
            } else if ("bililivedanmu".equalsIgnoreCase(mVodInfo.area) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                initLiveDanmu();
                String id = mVodInfo.id;
                UrlHttpUtil.get("https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo?id=" + id + "&type=0", new CallBackUtil.CallBackString() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        System.out.println(code + errorMessage);
                    }

                    @Override
                    public void onResponse(String response) {
                        JsonObject obj = new Gson().fromJson(response, JsonObject.class);
                        String token = obj.getAsJsonObject("data").get("token").getAsString();
                        initChatBroadcast(Long.valueOf(id), token);
                        danmakuView.prepare(danmakuParser, danmakuContext);
                    }
                });
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void addSimpleDanmaku(String content, long time) {
        if (danmakuContext != null && danmakuView != null && danmakuView.isPrepared()) {
            BaseDanmaku danmaku = danmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
            if (danmaku != null) {
                danmaku.text = content;
                danmaku.textSize = 25 * (danmakuParser.getDisplayer().getDensity() - 0.6f);
                danmaku.textColor = -1;
                danmaku.padding = 0;
                danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
                danmaku.isLive = false;
                danmaku.setTime(time);
                danmakuView.addDanmaku(danmaku);
            }
        }
    }


    private void initSubtitleView() {
        TrackInfo trackInfo = null;
        if (mVideoView.getMediaPlayer() instanceof IjkMediaPlayer) {
            trackInfo = ((IjkMediaPlayer)(mVideoView.getMediaPlayer())).getTrackInfo();
            if (trackInfo != null && trackInfo.getSubtitle().size() > 0) {
                mController.mSubtitleView.hasInternal = true;
            }
            ((IjkMediaPlayer)(mVideoView.getMediaPlayer())).setOnTimedTextListener(new IMediaPlayer.OnTimedTextListener() {
                @Override
                public void onTimedText(IMediaPlayer mp, IjkTimedText text) {
                    if (mController.mSubtitleView.isInternal) {
                        com.github.tvbox.osc.subtitle.model.Subtitle subtitle = new com.github.tvbox.osc.subtitle.model.Subtitle();
                        subtitle.content = text.getText();
                        mController.mSubtitleView.onSubtitleChanged(subtitle);
                    }
                }
            });
        }
        mController.mSubtitleView.bindToMediaPlayer(mVideoView.getMediaPlayer());
        mController.mSubtitleView.setPlaySubtitleCacheKey(subtitleCacheKey);
        String subtitlePathCache = (String)CacheManager.getCache(MD5.string2MD5(subtitleCacheKey));
        if (subtitlePathCache != null && !subtitlePathCache.isEmpty()) {
            mController.mSubtitleView.setSubtitlePath(subtitlePathCache);
        } else {
            if (playSubtitle != null && playSubtitle .length() > 0) {
                mController.mSubtitleView.setSubtitlePath(playSubtitle);
            } else {
                if (mController.mSubtitleView.hasInternal) {
                    mController.mSubtitleView.isInternal = true;
                    if (trackInfo != null && trackInfo.getSubtitle().size()>0) {
                        List<TrackInfoBean> subtitleTrackList = trackInfo.getSubtitle();
                        int selectedIndex = trackInfo.getSubtitleSelected(true);
                        boolean hasCh =false;
                        for(TrackInfoBean subtitleTrackInfoBean : subtitleTrackList) {
                            String lowerLang = subtitleTrackInfoBean.language.toLowerCase();
                            if (lowerLang.contains("zh") || lowerLang.contains("ch")) {
                                hasCh=true;
                                if (selectedIndex != subtitleTrackInfoBean.index) {
                                    ((IjkMediaPlayer)(mVideoView.getMediaPlayer())).setTrack(subtitleTrackInfoBean.index);
                                    break;
                                }
                            }
                        }
                        if(!hasCh)((IjkMediaPlayer)(mVideoView.getMediaPlayer())).setTrack(subtitleTrackList.get(0).index);
                    }
                }
            }
        }
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.playResult.observe(this, new Observer<JSONObject>() {
            @Override
            public void onChanged(JSONObject info) {
                if (info != null) {
                    try {
                        progressKey = info.optString("proKey", null);
                        boolean parse = info.optString("parse", "1").equals("1");
                        boolean jx = info.optString("jx", "0").equals("1");
                        playSubtitle = info.optString("subt", /*"https://dash.akamaized.net/akamai/test/caption_test/ElephantsDream/ElephantsDream_en.vtt"*/"");
                        if(playSubtitle.isEmpty() && info.has("subs")) {
                            try {
                                JSONObject obj =info.getJSONArray("subs").optJSONObject(0);
                                String url = obj.optString("url", "");
                                if (!TextUtils.isEmpty(url) && !FileUtils.hasExtension(url)) {
                                    String format = obj.optString("format", "");
                                    String name = obj.optString("name", "字幕");
                                    String ext = ".srt";
                                    switch (format) {
                                        case "text/x-ssa":
                                            ext = ".ass";
                                            break;
                                        case "text/vtt":
                                            ext = ".vtt";
                                            break;
                                        case "application/x-subrip":
                                            ext = ".srt";
                                            break;
                                        case "text/lrc":
                                            ext = ".lrc";
                                            break;
                                    }
                                    String filename = name + (name.toLowerCase().endsWith(ext) ? "" : ext);
                                    url += "#" + URLEncoder.encode(filename);
                                }
                                playSubtitle = url;
                            } catch (Throwable th) {
                            }
                        }
                        subtitleCacheKey = info.optString("subtKey", null);
                        String playUrl = info.optString("playUrl", "");
                        String msg = info.optString("msg", "");
                        if(!msg.isEmpty()){
                            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                        }
                        String flag = info.optString("flag");
                        String url = info.getString("url");
                        HashMap<String, String> headers = null;
                        webUserAgent = null;
                        webHeaderMap = null;
                        if (info.has("header")) {
                            try {
                                JSONObject hds = new JSONObject(info.getString("header"));
                                Iterator<String> keys = hds.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    if (headers == null) {
                                        headers = new HashMap<>();
                                    }
                                    headers.put(key, hds.getString(key));
                                    if (key.equalsIgnoreCase("user-agent")) {
                                        webUserAgent = hds.getString(key).trim();
                                    }
                                }
                                webHeaderMap = headers;
                            } catch (Throwable th) {

                            }
                        }
                        if (parse || jx) {
                            boolean userJxList = (playUrl.isEmpty() && ApiConfig.get().getVipParseFlags().contains(flag)) || jx;
                            initParse(flag, userJxList, playUrl, url);
                        } else {
                            mController.showParse(false);
                            playUrl(playUrl + url, headers);
                        }
                    } catch (Throwable th) {
//                        errorWithRetry("获取播放信息错误", true);
//                        Toast.makeText(mContext, "获取播放信息错误1", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    errorWithRetry("获取播放信息错误", true);
//                    Toast.makeText(mContext, "获取播放信息错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void setData(Bundle bundle) {
//        mVodInfo = (VodInfo) bundle.getSerializable("VodInfo");
        mVodInfo = App.getInstance().getVodInfo();
        sourceKey = bundle.getString("sourceKey");
        sourceBean = ApiConfig.get().getSource(sourceKey);
        initPlayerCfg();
        play(false);
    }

    private void initData() {
        /*Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {

        }*/
    }

    void initPlayerCfg() {
        try {
            mVodPlayerCfg = new JSONObject(mVodInfo.playerCfg);
        } catch (Throwable th) {
            mVodPlayerCfg = new JSONObject();
        }
        try {
            if (!mVodPlayerCfg.has("pl")) {
                mVodPlayerCfg.put("pl", (sourceBean.getPlayerType() == -1) ? (int)Hawk.get(HawkConfig.PLAY_TYPE, 1) : sourceBean.getPlayerType());
            }
            if (!mVodPlayerCfg.has("pr")) {
                mVodPlayerCfg.put("pr", Hawk.get(HawkConfig.PLAY_RENDER, 0));
            }
            if (!mVodPlayerCfg.has("ijk")) {
                mVodPlayerCfg.put("ijk", Hawk.get(HawkConfig.IJK_CODEC, ""));
            }
            if (!mVodPlayerCfg.has("sc")) {
                mVodPlayerCfg.put("sc", Hawk.get(HawkConfig.PLAY_SCALE, 0));
            }
            if (!mVodPlayerCfg.has("sp")) {
                mVodPlayerCfg.put("sp", 1.0f);
            }
            if (!mVodPlayerCfg.has("st")) {
                mVodPlayerCfg.put("st", 0);
            }
            if (!mVodPlayerCfg.has("et")) {
                mVodPlayerCfg.put("et", 0);
            }
        } catch (Throwable th) {

        }
        mController.setPlayerConfig(mVodPlayerCfg);
    }

    public boolean onBackPressed() {
        int requestedOrientation = getActivity().getRequestedOrientation();
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            mController.mLandscapePortraitBtn.setText("竖屏");
        }
        if (mController.onBackPressed()) {
            return true;
        }
        return false;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null) {
            if (mController.onKeyEvent(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoView != null) {
            mVideoView.pause();
        }
        if(danmakuView != null){
            danmakuView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.resume();
        }
        if (danmakuView != null){
            danmakuView.resume();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            if (mVideoView != null) {
                mVideoView.pause();
            }
            if(danmakuView != null){
                danmakuView.pause();
            }
        } else {
            if (mVideoView != null) {
                mVideoView.resume();
            }
            if(danmakuView != null){
                danmakuView.resume();
            }
        }
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        if(chatBroadcastWsClient != null){
            chatBroadcastWsClient.close();
            chatBroadcastWsClient = null;
        }
        if (mVideoView != null) {
            mVideoView.release();
            mVideoView = null;
        }
        if(danmakuView != null){
            danmakuView.release();
            danmakuView = null;
        }
        stopLoadWebView(true);
        stopParse();
        Thunder.stop(true);//停止磁力下载
        Jianpian.finish();//停止p2p下载
        App.getInstance().setDashData(null);
    }

    private VodInfo mVodInfo;
    private JSONObject mVodPlayerCfg;
    private String sourceKey;
    private SourceBean sourceBean;

    private void playNext(boolean isProgress) {
        boolean hasNext;
        if (mVodInfo == null || mVodInfo.seriesMap.get(mVodInfo.playFlag) == null) {
            hasNext = false;
        } else {
            hasNext = mVodInfo.playIndex + 1 < mVodInfo.seriesMap.get(mVodInfo.playFlag).size();
        }
        if (!hasNext) {
            Toast.makeText(requireContext(), "已经是最后一集了!", Toast.LENGTH_SHORT).show();
            return;
        }else {
            mVodInfo.playIndex++;
        }
        play(false);
    }

    private void playPrevious() {
        boolean hasPre = true;
        if (mVodInfo == null || mVodInfo.seriesMap.get(mVodInfo.playFlag) == null) {
            hasPre = false;
        } else {
            hasPre = mVodInfo.playIndex - 1 >= 0;
        }
        if (!hasPre) {
            Toast.makeText(requireContext(), "已经是第一集了!", Toast.LENGTH_SHORT).show();
            return;
        }
        mVodInfo.playIndex--;
        play(false);
    }

    private int autoRetryCount = 0;

    boolean autoRetry() {
        if (loadFoundVideoUrls != null && loadFoundVideoUrls.size() > 0) {
            autoRetryFromLoadFoundVideoUrls();
            return true;
        }
        if (autoRetryCount < 1) {
            autoRetryCount++;
            play(false);
            return true;
        } else {
            autoRetryCount = 0;
            return false;
        }
    }

    void autoRetryFromLoadFoundVideoUrls() {
        String videoUrl = loadFoundVideoUrls.poll();
        HashMap<String,String> header = loadFoundVideoUrlsHeader.get(videoUrl);
        playUrl(videoUrl, header);
    }

    void initParseLoadFound() {
        loadFoundCount.set(0);
        loadFoundVideoUrls = new LinkedList<String>();
        loadFoundVideoUrlsHeader = new HashMap<String, HashMap<String, String>>();
    }

    public void play(boolean reset) {
        if(mVodInfo==null)return;
        VodInfo.VodSeries vs = mVodInfo.seriesMap.get(mVodInfo.playFlag).get(mVodInfo.playIndex);
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_REFRESH, mVodInfo.playIndex));
        setTip("正在获取播放信息", true, false);
        String playTitleInfo = mVodInfo.name + " " + vs.name;
        mController.setTitle(playTitleInfo);

        stopParse();
        initParseLoadFound();
        if(mVideoView!=null) mVideoView.release();
        String subtitleCacheKey = mVodInfo.sourceKey + "-" + mVodInfo.id + "-" + mVodInfo.playFlag + "-" + mVodInfo.playIndex+ "-" + vs.name + "-subt";
        String progressKey = mVodInfo.sourceKey + mVodInfo.id + mVodInfo.playFlag + mVodInfo.playIndex + vs.name;
        //重新播放清除现有进度
        if (reset) {
            CacheManager.delete(MD5.string2MD5(progressKey), 0);
            CacheManager.delete(MD5.string2MD5(subtitleCacheKey), 0);
        }else{
            try{
                int playerType = mVodPlayerCfg.getInt("pl");
                if(playerType==1){
                    mController.mSubtitleView.setVisibility(View.VISIBLE);
                }else {
                    mController.mSubtitleView.setVisibility(View.GONE);
                }
            }catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(Jianpian.isJpUrl(vs.url)){//荐片地址特殊判断
            String jp_url= vs.url;
            mController.showParse(false);
            if(vs.url.startsWith("tvbox-xg:")){
                playUrl(Jianpian.JPUrlDec(jp_url.substring(9)), null);
            }else {
                playUrl(Jianpian.JPUrlDec(jp_url), null);
            }
            return;
        }
        if (Thunder.play(vs.url, new Thunder.ThunderCallback() {
            @Override
            public void status(int code, String info) {
                if (code < 0) {
                    setTip(info, false, true);
                } else {
                    setTip(info, true, false);
                }
            }

            @Override
            public void list(Map<Integer, String> urlMap) {
            }

            @Override
            public void play(String url) {
                playUrl(url, null);
            }
        })) {
            mController.showParse(false);
            return;
        }
        sourceViewModel.getPlay(sourceKey, mVodInfo.playFlag, progressKey, vs.url, subtitleCacheKey);
    }

    private String playSubtitle;
    private String subtitleCacheKey;
    private String progressKey;
    private String parseFlag;
    private String webUrl;
    private String webUserAgent;
    private Map<String, String > webHeaderMap;

    private void initParse(String flag, boolean useParse, String playUrl, final String url) {
        parseFlag = flag;
        webUrl = url;
        ParseBean parseBean = null;
        mController.showParse(useParse);
        if (useParse) {
            parseBean = ApiConfig.get().getDefaultParse();
        } else {
            if (playUrl.startsWith("json:")) {
                parseBean = new ParseBean();
                parseBean.setType(1);
                parseBean.setUrl(playUrl.substring(5));
            } else if (playUrl.startsWith("parse:")) {
                String parseRedirect = playUrl.substring(6);
                for (ParseBean pb : ApiConfig.get().getParseBeanList()) {
                    if (pb.getName().equals(parseRedirect)) {
                        parseBean = pb;
                        break;
                    }
                }
            }
            if (parseBean == null) {
                parseBean = new ParseBean();
                parseBean.setType(0);
                parseBean.setUrl(playUrl);
            }
        }
        doParse(parseBean);
    }

    JSONObject jsonParse(String input, String json) throws JSONException {
        JSONObject jsonPlayData = new JSONObject(json);
        //小窗版解析方法改到这了  之前那个位置data解析无效
        String url;
        if (jsonPlayData.has("data")) {
            url = jsonPlayData.getJSONObject("data").getString("url");
        } else {
            url = jsonPlayData.getString("url");
        }
        if (url.startsWith("//")) {
            url = "http:" + url;
        }
        if (!url.startsWith("http")) {
            return null;
        }
        JSONObject headers = new JSONObject();
        String ua = jsonPlayData.optString("user-agent", "");
        if (ua.trim().length() > 0) {
            headers.put("User-Agent", " " + ua);
        }
        String referer = jsonPlayData.optString("referer", "");
        if (referer.trim().length() > 0) {
            headers.put("Referer", " " + referer);
        }
        JSONObject taskResult = new JSONObject();
        taskResult.put("header", headers);
        taskResult.put("url", url);
        return taskResult;
    }

    void stopParse() {
        mHandler.removeMessages(100);
        stopLoadWebView(false);
        OkGo.getInstance().cancelTag("json_jx");
        if (parseThreadPool != null) {
            try {
                parseThreadPool.shutdown();
                parseThreadPool = null;
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    ExecutorService parseThreadPool;

    private void doParse(ParseBean pb) {
        stopParse();
        initParseLoadFound();
        if (pb.getType() == 0) {
            setTip("正在嗅探播放地址", true, false);
            mHandler.removeMessages(100);
            mHandler.sendEmptyMessageDelayed(100, 20 * 1000);
            if(pb.getExt()!=null){
                // 解析ext
                try {
                    HashMap<String, String> reqHeaders = new HashMap<>();
                    JSONObject jsonObject = new JSONObject(pb.getExt());
                    if (jsonObject.has("header")) {
                        JSONObject headerJson = jsonObject.optJSONObject("header");
                        Iterator<String> keys = headerJson.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            if (key.equalsIgnoreCase("user-agent")) {
                                webUserAgent = headerJson.getString(key).trim();
                            }else {
                                reqHeaders.put(key, headerJson.optString(key, ""));
                            }
                        }
                        if(reqHeaders.size()>0)webHeaderMap = reqHeaders;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            loadWebView(pb.getUrl() + webUrl);

        } else if (pb.getType() == 1) { // json 解析
            setTip("正在解析播放地址", true, false);
            // 解析ext
            HttpHeaders reqHeaders = new HttpHeaders();
            try {
                JSONObject jsonObject = new JSONObject(pb.getExt());
                if (jsonObject.has("header")) {
                    JSONObject headerJson = jsonObject.optJSONObject("header");
                    Iterator<String> keys = headerJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        reqHeaders.put(key, headerJson.optString(key, ""));
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            OkGo.<String>get(pb.getUrl() + encodeUrl(webUrl))
                    .tag("json_jx")
                    .headers(reqHeaders)
                    .execute(new AbsCallback<String>() {
                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            if (response.body() != null) {
                                return response.body().string();
                            } else {
                                throw new IllegalStateException("网络请求错误");
                            }
                        }

                        @Override
                        public void onSuccess(Response<String> response) {
                            String json = response.body();
                            try {
                                JSONObject rs = jsonParse(webUrl, json);
                                HashMap<String, String> headers = null;
                                if (rs.has("header")) {
                                    try {
                                        JSONObject hds = rs.getJSONObject("header");
                                        Iterator<String> keys = hds.keys();
                                        while (keys.hasNext()) {
                                            String key = keys.next();
                                            if (headers == null) {
                                                headers = new HashMap<>();
                                            }
                                            headers.put(key, hds.getString(key));
                                        }
                                    } catch (Throwable th) {

                                    }
                                }
                                playUrl(rs.getString("url"), headers);
                            } catch (Throwable e) {
                                e.printStackTrace();
                                errorWithRetry("解析错误", false);
//                                setTip("解析错误", false, true);
                            }
                        }

                        @Override
                        public void onError(Response<String> response) {
                            super.onError(response);
                            errorWithRetry("解析错误", false);
//                            setTip("解析错误", false, true);
                        }
                    });
        } else if (pb.getType() == 2) { // json 扩展
            setTip("正在解析播放地址", true, false);
            parseThreadPool = Executors.newSingleThreadExecutor();
            LinkedHashMap<String, String> jxs = new LinkedHashMap<>();
            for (ParseBean p : ApiConfig.get().getParseBeanList()) {
                if (p.getType() == 1) {
                    jxs.put(p.getName(), p.mixUrl());
                }
            }
            parseThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    JSONObject rs = ApiConfig.get().jsonExt(pb.getUrl(), jxs, webUrl);
                    if (rs == null || !rs.has("url") || rs.optString("url").isEmpty()) {
//                        errorWithRetry("解析错误", false);
                        setTip("解析错误", false, true);
                    } else {
                        HashMap<String, String> headers = null;
                        if (rs.has("header")) {
                            try {
                                JSONObject hds = rs.getJSONObject("header");
                                Iterator<String> keys = hds.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    if (headers == null) {
                                        headers = new HashMap<>();
                                    }
                                    headers.put(key, hds.getString(key));
                                }
                            } catch (Throwable th) {

                            }
                        }
                        if (rs.has("jxFrom")) {
                            if(!isAdded())return;
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, "解析来自:" + rs.optString("jxFrom"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        boolean parseWV = rs.optInt("parse", 0) == 1;
                        if (parseWV) {
                            String wvUrl = DefaultConfig.checkReplaceProxy(rs.optString("url", ""));
                            loadUrl(wvUrl);
                        } else {
                            playUrl(rs.optString("url", ""), headers);
                        }
                    }
                }
            });
        } else if (pb.getType() == 3) { // json 聚合
            setTip("正在解析播放地址", true, false);
            parseThreadPool = Executors.newSingleThreadExecutor();
            LinkedHashMap<String, HashMap<String, String>> jxs = new LinkedHashMap<>();
            String extendName = "";
            for (ParseBean p : ApiConfig.get().getParseBeanList()) {
                HashMap<String, String> data = new HashMap<String, String>();
                data.put("url", p.getUrl());
                if (p.getUrl().equals(pb.getUrl())) {
                    extendName = p.getName();
                }
                data.put("type", p.getType() + "");
                data.put("ext", p.getExt());
                jxs.put(p.getName(), data);
            }
            String finalExtendName = extendName;
            parseThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    JSONObject rs = ApiConfig.get().jsonExtMix(parseFlag + "111", pb.getUrl(), finalExtendName, jxs, webUrl);
                    if (rs == null || !rs.has("url") || rs.optString("url").isEmpty()) {
//                        errorWithRetry("解析错误", false);
                        setTip("解析错误", false, true);
                    } else {
                        if (rs.has("parse") && rs.optInt("parse", 0) == 1) {
                            if (rs.has("ua")) {
                                webUserAgent = rs.optString("ua").trim();
                            }
                            if(!isAdded())return;
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String mixParseUrl = DefaultConfig.checkReplaceProxy(rs.optString("url", ""));
                                    stopParse();
                                    setTip("正在嗅探播放地址", true, false);
                                    mHandler.removeMessages(100);
                                    mHandler.sendEmptyMessageDelayed(100, 20 * 1000);
                                    loadWebView(mixParseUrl);
                                }
                            });
                        } else {
                            HashMap<String, String> headers = null;
                            if (rs.has("header")) {
                                try {
                                    JSONObject hds = rs.getJSONObject("header");
                                    Iterator<String> keys = hds.keys();
                                    while (keys.hasNext()) {
                                        String key = keys.next();
                                        if (headers == null) {
                                            headers = new HashMap<>();
                                        }
                                        headers.put(key, hds.getString(key));
                                    }
                                } catch (Throwable th) {
                                    th.printStackTrace();
                                }
                            }
                            if (rs.has("jxFrom")) {
                                if(!isAdded())return;
                                requireActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mContext, "解析来自:" + rs.optString("jxFrom"), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            playUrl(rs.optString("url", ""), headers);
                        }
                    }
                }
            });
        }
    }

    private String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }

    // webview
    private XWalkView mXwalkWebView;
    private WebView mSysWebView;
    private final Map<String, Boolean> loadedUrls = new HashMap<>();
    private LinkedList<String> loadFoundVideoUrls = new LinkedList<>();
    private HashMap<String, HashMap<String, String>> loadFoundVideoUrlsHeader = new HashMap<>();
    private final AtomicInteger loadFoundCount = new AtomicInteger(0);

    void loadWebView(String url) {
        if (mSysWebView == null && mXwalkWebView == null) {
            boolean useSystemWebView = Hawk.get(HawkConfig.PARSE_WEBVIEW, true);
            if (!useSystemWebView) {
                XWalkUtils.tryUseXWalk(mContext, new XWalkUtils.XWalkState() {
                    @Override
                    public void success() {
                        initWebView(!sourceBean.getClickSelector().isEmpty());
                        loadUrl(url);
                    }

                    @Override
                    public void fail() {
                        Toast.makeText(mContext, "XWalkView不兼容，已替换为系统自带WebView", Toast.LENGTH_SHORT).show();
                        initWebView(true);
                        loadUrl(url);
                    }

                    @Override
                    public void ignore() {
                        Toast.makeText(mContext, "XWalkView运行组件未下载，已替换为系统自带WebView", Toast.LENGTH_SHORT).show();
                        initWebView(true);
                        loadUrl(url);
                    }
                });
            } else {
                initWebView(true);
                loadUrl(url);
            }
        } else {
            loadUrl(url);
        }
    }

    void initWebView(boolean useSystemWebView) {
        if (useSystemWebView) {
            mSysWebView = new MyWebView(mContext);
            configWebViewSys(mSysWebView);
        } else {
            mXwalkWebView = new MyXWalkView(mContext);
            configWebViewX5(mXwalkWebView);
        }
    }

    void loadUrl(String url) {
        if(!isAdded())return;
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mXwalkWebView != null) {
                    mXwalkWebView.stopLoading();
                    if(webUserAgent != null) {
                        mXwalkWebView.getSettings().setUserAgentString(webUserAgent);
                    }
                    //mXwalkWebView.clearCache(true);
                    if(webHeaderMap != null){
                        mXwalkWebView.loadUrl(url,webHeaderMap);
                    }else {
                        mXwalkWebView.loadUrl(url);
                    }
                }
                if (mSysWebView != null) {
                    mSysWebView.stopLoading();
                    if(webUserAgent != null) {
                        mSysWebView.getSettings().setUserAgentString(webUserAgent);
                    }
                    //mSysWebView.clearCache(true);
                    if(webHeaderMap != null){
                        mSysWebView.loadUrl(url,webHeaderMap);
                    }else {
                        mSysWebView.loadUrl(url);
                    }
                }
            }
        });
    }

    void stopLoadWebView(boolean destroy) {
        if (mActivity == null) return;
        if(!isAdded())return;
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mXwalkWebView != null) {
                    mXwalkWebView.stopLoading();
                    mXwalkWebView.loadUrl("about:blank");
                    if (destroy) {
//                        mXwalkWebView.clearCache(true);
                        mXwalkWebView.removeAllViews();
                        mXwalkWebView.onDestroy();
                        mXwalkWebView = null;
                    }
                }
                if (mSysWebView != null) {
                    mSysWebView.stopLoading();
                    mSysWebView.loadUrl("about:blank");
                    if (destroy) {
//                        mSysWebView.clearCache(true);
                        final ViewGroup viewGroup = (ViewGroup) mSysWebView.getParent();
                        if (viewGroup != null) {
                            viewGroup.removeView(mSysWebView);
                        }
                        mSysWebView.removeAllViews();
                        mSysWebView.destroy();
                        mSysWebView = null;
                    }
                }
            }
        });
    }

    boolean checkVideoFormat(String url) {
        try{
            if (url.contains("url=http") || url.contains(".html")) {
                return false;
            }
            if (sourceBean.getType() == 3) {
                Spider sp = ApiConfig.get().getCSP(sourceBean);
                if (sp != null && sp.manualVideoCheck()){
                    return sp.isVideoFormat(url);
                }
            }
            return VideoParseRuler.checkIsVideoForParse(webUrl, url);
        }catch (Exception e){
            return false;
        }
    }

    class MyWebView extends WebView {
        public MyWebView(@NonNull Context context) {
            super(context);
        }

        @Override
        public void setOverScrollMode(int mode) {
            super.setOverScrollMode(mode);
            if (mContext instanceof Activity)
                AutoSize.autoConvertDensityOfCustomAdapt((Activity) mContext, PlayFragment.this);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return false;
        }
    }

    class MyXWalkView extends XWalkView {
        public MyXWalkView(Context context) {
            super(context);
        }

        @Override
        public void setOverScrollMode(int mode) {
            super.setOverScrollMode(mode);
            if (mContext instanceof Activity)
                AutoSize.autoConvertDensityOfCustomAdapt((Activity) mContext, PlayFragment.this);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return false;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configWebViewSys(WebView webView) {
        if (webView == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = Hawk.get(HawkConfig.DEBUG_OPEN, false)
                ? new ViewGroup.LayoutParams(800, 400) :
                new ViewGroup.LayoutParams(1, 1);
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        webView.clearFocus();
        webView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        if(!isAdded())return;
        requireActivity().addContentView(webView, layoutParams);
        /* 添加webView配置 */
        final WebSettings settings = webView.getSettings();
        settings.setNeedInitialFocus(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            settings.setBlockNetworkImage(false);
        } else {
            settings.setBlockNetworkImage(true);
        }
        settings.setUseWideViewPort(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setSupportZoom(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
//        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        /* 添加webView配置 */
        //设置编码
        settings.setDefaultTextEncodingName("utf-8");
        settings.setUserAgentString(webView.getSettings().getUserAgentString());
//         settings.setUserAgentString(ANDROID_UA);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return false;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                return true;
            }
        });
        SysWebClient mSysWebClient = new SysWebClient();
        webView.setWebViewClient(mSysWebClient);
        webView.setBackgroundColor(Color.BLACK);
    }

    private class SysWebClient extends WebViewClient {

        @SuppressLint("WebViewClientOnReceivedSslError")
        @Override
        public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
            sslErrorHandler.proceed();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted( view,  url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view,url);
            String click=sourceBean.getClickSelector();
            LOG.i("onPageFinished url:" + url);

            if(!click.isEmpty()){
                String selector;
                if(click.contains(";")){
                    if(!url.contains(click.split(";")[0]))return;
                    selector=click.split(";")[1];
                }else {
                    selector=click.trim();
                }
                String js="$(\""+ selector+"\").click();";
                LOG.i("javascript:" + js);
                mSysWebView.loadUrl("javascript:"+js);
            }
        }

        WebResourceResponse checkIsVideo(String url, HashMap<String, String> headers) {
            if (url.endsWith("/favicon.ico")) {
                if (url.startsWith("http://127.0.0.1")) {
                    return new WebResourceResponse("image/x-icon", "UTF-8", null);
                }
                return null;
            }

            boolean isFilter = VideoParseRuler.isFilter(webUrl, url);
            if (isFilter) {
                LOG.i( "shouldInterceptLoadRequest filter:" + url);
                return null;
            }

            boolean ad;
            if (!loadedUrls.containsKey(url)) {
                ad = AdBlocker.isAd(url);
                loadedUrls.put(url, ad);
            } else {
                ad = Boolean.TRUE.equals(loadedUrls.get(url));
            }

            if (!ad) {
                if (checkVideoFormat(url)) {
                    loadFoundVideoUrls.add(url);
                    loadFoundVideoUrlsHeader.put(url, headers);
                    LOG.i("loadFoundVideoUrl:" + url );
                    if (loadFoundCount.incrementAndGet() == 1) {
                        url = loadFoundVideoUrls.poll();
                        mHandler.removeMessages(100);
                        String cookie = CookieManager.getInstance().getCookie(url);
                        if(!TextUtils.isEmpty(cookie))headers.put("Cookie", " " + cookie);//携带cookie
                        playUrl(url, headers);
                        stopLoadWebView(false);
                    }
                }
            }

            return ad || loadFoundCount.get() > 0 ?
                    AdBlocker.createEmptyResource() :
                    null;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
//            WebResourceResponse response = checkIsVideo(url, new HashMap<>());
            return null;
        }

        @Nullable
        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            LOG.i("shouldInterceptRequest url:" + url);
            HashMap<String, String> webHeaders = new HashMap<>();
            Map<String, String> hds = request.getRequestHeaders();
            if (hds != null && hds.keySet().size() > 0) {
                for (String k : hds.keySet()) {
                    if (k.equalsIgnoreCase("user-agent")
                            || k.equalsIgnoreCase("referer")
                            || k.equalsIgnoreCase("origin")) {
                        webHeaders.put(k," " + hds.get(k));
                    }
                }
            }
            return checkIsVideo(url, webHeaders);
        }

        @Override
        public void onLoadResource(WebView webView, String url) {
            super.onLoadResource(webView, url);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configWebViewX5(XWalkView webView) {
        if (webView == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = Hawk.get(HawkConfig.DEBUG_OPEN, false)
                ? new ViewGroup.LayoutParams(800, 400) :
                new ViewGroup.LayoutParams(1, 1);
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        webView.clearFocus();
        webView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        if(!isAdded())return;
        requireActivity().addContentView(webView, layoutParams);
        /* 添加webView配置 */
        final XWalkSettings settings = webView.getSettings();
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptEnabled(true);

        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            settings.setBlockNetworkImage(false);
        } else {
            settings.setBlockNetworkImage(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }
        settings.setUseWideViewPort(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setSupportZoom(false);
//        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // settings.setUserAgentString(ANDROID_UA);

        webView.setBackgroundColor(Color.BLACK);
        webView.setUIClient(new XWalkUIClient(webView) {
            @Override
            public boolean onConsoleMessage(XWalkView view, String message, int lineNumber, String sourceId, ConsoleMessageType messageType) {
                return false;
            }

            @Override
            public boolean onJsAlert(XWalkView view, String url, String message, XWalkJavascriptResult result) {
                return true;
            }

            @Override
            public boolean onJsConfirm(XWalkView view, String url, String message, XWalkJavascriptResult result) {
                return true;
            }

            @Override
            public boolean onJsPrompt(XWalkView view, String url, String message, String defaultValue, XWalkJavascriptResult result) {
                return true;
            }
        });
        XWalkWebClient mX5WebClient = new XWalkWebClient(webView);
        webView.setResourceClient(mX5WebClient);
    }

    private class XWalkWebClient extends XWalkResourceClient {
        public XWalkWebClient(XWalkView view) {
            super(view);
        }

        @Override
        public void onDocumentLoadedInFrame(XWalkView view, long frameId) {
            super.onDocumentLoadedInFrame(view, frameId);
        }

        @Override
        public void onLoadStarted(XWalkView view, String url) {
            super.onLoadStarted(view, url);
        }

        @Override
        public void onLoadFinished(XWalkView view, String url) {
            super.onLoadFinished(view, url);
        }

        @Override
        public void onProgressChanged(XWalkView view, int progressInPercent) {
            super.onProgressChanged(view, progressInPercent);
        }

        @Override
        public XWalkWebResourceResponse shouldInterceptLoadRequest(XWalkView view, XWalkWebResourceRequest request) {
            String url = request.getUrl().toString();
            LOG.i("shouldInterceptLoadRequest url:" + url);
            // suppress favicon requests as we don't display them anywhere
            if (url.endsWith("/favicon.ico")) {
                if (url.startsWith("http://127.0.0.1")) {
                    return createXWalkWebResourceResponse("image/x-icon", "UTF-8", null);
                }
                return null;
            }

            boolean isFilter = VideoParseRuler.isFilter(webUrl, url);
            if (isFilter) {
                LOG.i( "shouldInterceptLoadRequest filter:" + url);
                return null;
            }

            boolean ad;
            if (!loadedUrls.containsKey(url)) {
                ad = AdBlocker.isAd(url);
                loadedUrls.put(url, ad);
            } else {
                ad = Boolean.TRUE.equals(loadedUrls.get(url));
            }
            if (!ad ) {

                if (checkVideoFormat(url)) {
                    HashMap<String, String> webHeaders = new HashMap<>();
                    Map<String, String> hds = request.getRequestHeaders();
                    if (hds != null && hds.keySet().size() > 0) {
                        for (String k : hds.keySet()) {
                            if (k.equalsIgnoreCase("user-agent")
                                    || k.equalsIgnoreCase("referer")
                                    || k.equalsIgnoreCase("origin")) {
                                webHeaders.put(k," " + hds.get(k));
                            }
                        }
                    }
                    loadFoundVideoUrls.add(url);
                    loadFoundVideoUrlsHeader.put(url, webHeaders);
                    LOG.i("loadFoundVideoUrl:" + url );
                    if (loadFoundCount.incrementAndGet() == 1) {
                        mHandler.removeMessages(100);
                        url = loadFoundVideoUrls.poll();
                        String cookie = CookieManager.getInstance().getCookie(url);
                        if(!TextUtils.isEmpty(cookie))webHeaders.put("Cookie", " " + cookie);//携带cookie
                        playUrl(url, webHeaders);
                        stopLoadWebView(false);
                    }
                }
            }
            return ad || loadFoundCount.get() > 0 ?
                    createXWalkWebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes())) :
                    null;
        }

        @Override
        public boolean shouldOverrideUrlLoading(XWalkView view, String s) {
            return false;
        }

        @Override
        public void onReceivedSslError(XWalkView view, ValueCallback<Boolean> callback, SslError error) {
            callback.onReceiveValue(true);
        }
    }

}
