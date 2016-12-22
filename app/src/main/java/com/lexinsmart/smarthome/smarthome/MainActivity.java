package com.lexinsmart.smarthome.smarthome;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.unisound.client.SpeechConstants;
import com.unisound.client.SpeechSynthesizer;
import com.unisound.client.SpeechSynthesizerListener;
import com.unisound.client.SpeechUnderstander;
import com.unisound.client.SpeechUnderstanderListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import mqtt.MqttV3Service;
import view.AudioRecorderButton;
import voice.Config;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AudioRecorderButton.RecognitionCompletion {


    @Override
    public void RecognitionCompletion(String sss) {
        Toast.makeText(MainActivity.this, "识别结果：" + sss, Toast.LENGTH_SHORT).show();

        MqttV3Service.publishMsg(sss, Qos, 0);

    }

    /**
     * 当前识别状态
     */
    enum AsrStatus {
        idle, recording, recognizing
    }

    private static int currentDomain = 0;
    private static String arrayDomain[] = new String[]{"general", "poi", "song", "movietv", "medical"};
    private static String arrayDomainChina[] = new String[]{"通用识别  ", "地名识别  ", "歌名识别  ", "影视名识别  ", "医药领域识别  "};
    private static String arraySampleStr[] = new String[]{"RATE_AUTO  ", "RATE_16K  ", "RATE_8K  "};
    private static String arrayLanguageStr[] = new String[]{SpeechConstants.LANGUAGE_MANDARIN,
            SpeechConstants.LANGUAGE_ENGLISH, SpeechConstants.LANGUAGE_CANTONESE};
    private static int arraySample[] = new int[]{SpeechConstants.ASR_SAMPLING_RATE_BANDWIDTH_AUTO,
            SpeechConstants.ASR_SAMPLING_RATE_16K, SpeechConstants.ASR_SAMPLING_RATE_8K};
    private static int currentSample = 0;
    private static int currentLanguage = 0;

    private AsrStatus statue = AsrStatus.idle;
    private SpeechUnderstander mUnderstander;
    @SuppressWarnings("unused")
    private String mRecognizerText = "";
    private SpeechSynthesizer mTTSPlayer;
    private StringBuffer mAsrResultBuffer;
    public static TextView resultTv;


    private Context context;
    String ADDRESS = "180.76.179.148";
    String PORT = "1883";
    int Qos = 1;
    ArrayList<String> topicList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        context = this;

        resultTv = (TextView) findViewById(R.id.recognizer_result_tv);
        mAsrResultBuffer = new StringBuffer();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (statue == AsrStatus.idle) {
                    mAsrResultBuffer.delete(0, mAsrResultBuffer.length());
                    // 在收到 onRecognizerStart 回调前，录音设备没有打开，请添加界面等待提示，
                    // 录音设备打开前用户说的话不能被识别到，影响识别效果。
                    // 修改录音采样率
                    mUnderstander.setOption(SpeechConstants.ASR_SAMPLING_RATE, arraySample[currentSample]);
                    // 修改识别领域
                    mUnderstander.setOption(SpeechConstants.ASR_DOMAIN, arrayDomain[currentDomain]);
                    // 修改识别语音
                    mUnderstander.setOption(SpeechConstants.ASR_LANGUAGE, arrayLanguageStr[currentLanguage]);
                    mUnderstander.start();
                } else if (statue == AsrStatus.recording) {
                    stopRecord();
                } else if (statue == AsrStatus.recognizing) {
                    // 取消识别
                    mUnderstander.cancel();
                    statue = AsrStatus.idle;
                }

            }
        });

        // 初始化对象
        initRecognizer();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        topicList.add("babycradle");
        new Thread(new MqttProcThread()).start();
    }

    private void initRecognizer() {

        // 创建语音理解对象，appKey和 secret通过 http://dev.hivoice.cn/ 网站申请
        mUnderstander = new SpeechUnderstander(this, Config.appKey, Config.secret);
        // 开启可变结果
        mUnderstander.setOption(SpeechConstants.ASR_OPT_TEMP_RESULT_ENABLE, true);

        // 创建语音合成对象
        mTTSPlayer = new SpeechSynthesizer(this, Config.appKey, Config.secret);
        mTTSPlayer.setOption(SpeechConstants.TTS_SERVICE_MODE, SpeechConstants.TTS_SERVICE_MODE_NET);
        // 设置语音合成回调监听
        mTTSPlayer.setTTSListener(new SpeechSynthesizerListener() {

            @Override
            public void onEvent(int type) {
                switch (type) {
                    case SpeechConstants.TTS_EVENT_INIT:
                        // 初始化成功回调
                        break;
                    case SpeechConstants.TTS_EVENT_SYNTHESIZER_START:
                        // 开始合成回调
                        break;
                    case SpeechConstants.TTS_EVENT_SYNTHESIZER_END:
                        // 合成结束回调
                        break;
                    case SpeechConstants.TTS_EVENT_BUFFER_BEGIN:
                        // 开始缓存回调
                        break;
                    case SpeechConstants.TTS_EVENT_BUFFER_READY:
                        // 缓存完毕回调
                        break;
                    case SpeechConstants.TTS_EVENT_PLAYING_START:
                        // 开始播放回调
                        break;
                    case SpeechConstants.TTS_EVENT_PLAYING_END:
                        // 播放完成回调
                        break;
                    case SpeechConstants.TTS_EVENT_PAUSE:
                        // 暂停回调
                        break;
                    case SpeechConstants.TTS_EVENT_RESUME:
                        // 恢复回调
                        break;
                    case SpeechConstants.TTS_EVENT_STOP:
                        // 停止回调
                        break;
                    case SpeechConstants.TTS_EVENT_RELEASE:
                        // 释放资源回调
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onError(int type, String errorMSG) {
                // 语音合成错误回调
                hitErrorMsg(errorMSG);

            }
        });
        mTTSPlayer.init("");
        // 保存录音数据
        // recognizer.setRecordingDataEnable(true);
        mUnderstander.setListener(new SpeechUnderstanderListener() {
            @Override
            public void onEvent(int type, int timeMs) {
                switch (type) {
                    case SpeechConstants.ASR_EVENT_NET_END:
                        log_v("onEnd");

                        statue = AsrStatus.idle;
                        break;
                    case SpeechConstants.ASR_EVENT_VOLUMECHANGE:
                        // 说话音量实时返回
                        int volume = (Integer) mUnderstander.getOption(SpeechConstants.GENERAL_UPDATE_VOLUME);
//                        mVolume.setProgress(volume);
                        break;
                    case SpeechConstants.ASR_EVENT_VAD_TIMEOUT:
                        // 说话音量实时返回
                        log_v("onVADTimeout");
                        // 收到用户停止说话事件，停止录音
                        stopRecord();
                        break;
                    case SpeechConstants.ASR_EVENT_RECORDING_STOP:
                        // 停止录音，请等待识别结果回调
                        log_v("onRecordingStop");
                        statue = AsrStatus.recognizing;

                        break;
                    case SpeechConstants.ASR_EVENT_SPEECH_DETECTED:
                        //用户开始说话
                        log_v("onSpeakStart");
                        break;
                    case SpeechConstants.ASR_EVENT_RECORDING_START:
                        //录音设备打开，开始识别，用户可以开始说话
                        statue = AsrStatus.recording;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(int type, String errorMSG) {
                if (errorMSG != null) {
                    // 显示错误信息
                    hitErrorMsg(errorMSG);
                } else {
                    Toast.makeText(MainActivity.this, "没听到", Toast.LENGTH_SHORT).show();
//                    if ("".equals(mRecognizerResultText.getText().toString())) {
//                        mRecognizerResultText.setText(R.string.no_hear_sound);
//                    }
                }
            }

            @Override
            public void onResult(int type, String jsonResult) {
                switch (type) {
                    case SpeechConstants.ASR_RESULT_NET:
                        // 在线识别结果，通常onResult接口多次返回结果，保留识别结果组成完整的识别内容。
                        log_v("onRecognizerResult");
                        if (jsonResult.contains("net_asr")
                                && jsonResult.contains("net_nlu")) {
                            try {
//                                mNluResultText.setText(jsonResult);
                                JSONObject json = new JSONObject(jsonResult);
                                JSONArray jsonArray = json.getJSONArray("net_asr");
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                String status = jsonObject.getString("result_type");
                                log_v("jsonObject = " + jsonObject.toString());

                                if (status.equals("full")) {
                                    log_v("full");
                                    String result = (String) jsonObject
                                            .get("recognition_result");
                                    if (jsonResult != null) {
                                        mTTSPlayer.playText(result.trim());
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            //取出语音识别结果
                            asrResultOperate(jsonResult);
                        }
                        break;
                    default:
                        break;
                }
            }

        });
        mUnderstander.init("");


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * 打印日志信息
     *
     * @param msg
     */
    private void log_v(String msg) {
        Log.v("demo", msg);
    }

    @SuppressWarnings("unused")
    private void log_e(String msg) {
        Log.e("demo", msg);
    }

    private void hitErrorMsg(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        mUnderstander.stop();
    }

    private void asrResultOperate(String jsonResult) {
        JSONObject asrJson;
        try {
            asrJson = new JSONObject(jsonResult);
            JSONArray asrJsonArray = asrJson.getJSONArray("net_asr");
            JSONObject asrJsonObject = asrJsonArray.getJSONObject(0);
            String asrJsonStatus = asrJsonObject.getString("result_type");
//            mRecognizerResultText.setText("");
            if (asrJsonStatus.equals("change")) {
                Toast.makeText(MainActivity.this, asrJsonObject.getString("recognition_result"), Toast.LENGTH_SHORT).show();
//                mRecognizerResultText.append(mAsrResultBuffer.toString());
//                mRecognizerResultText.append(asrJsonObject.getString("recognition_result"));
            } else {
                mAsrResultBuffer.append(asrJsonObject.getString("recognition_result"));
                Toast.makeText(MainActivity.this, mAsrResultBuffer.toString(), Toast.LENGTH_SHORT).show();

//                mRecognizerResultText.append(mAsrResultBuffer.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public class MqttProcThread implements Runnable {

        int randomid = (int) Math.floor(10000 + Math.random() * 90000);

        @Override
        public void run() {
            Message msg = new Message();
            boolean ret = MqttV3Service.connectionMqttServer(myHandler, ADDRESS, PORT, "lexin" + randomid, topicList);
            if (ret) {
                msg.what = 1;
            } else {
                msg.what = 0;
            }
            msg.obj = "strresult";
            myHandler.sendMessage(msg);
        }
    }

    @SuppressWarnings("HandlerLeak")
    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                Toast.makeText(context, "连接成功", Toast.LENGTH_SHORT).show();

            } else if (msg.what == 0) {
                Toast.makeText(context, "连接失败", Toast.LENGTH_SHORT).show();
            } else if (msg.what == 2) {
                String strContent = "";
                strContent += msg.getData().getString("content");
                System.out.println("strcontent:" + strContent);
            } else if (msg.what == 3) {
                if (MqttV3Service.closeMqtt()) {
                    Toast.makeText(context, "断开连接", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
}
