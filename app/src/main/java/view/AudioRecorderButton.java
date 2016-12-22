package view;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lexinsmart.smarthome.smarthome.MainActivity;
import com.lexinsmart.smarthome.smarthome.R;
import com.unisound.client.SpeechConstants;
import com.unisound.client.SpeechSynthesizer;
import com.unisound.client.SpeechSynthesizerListener;
import com.unisound.client.SpeechUnderstander;
import com.unisound.client.SpeechUnderstanderListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import utils.VibratorUtil;
import voice.Config;

/**
 * Created by xushun on 2016/12/20.
 */

public class AudioRecorderButton extends Button {
    /**
     * 当前识别状态
     */
    enum AsrStatus {
        idle, recording, recognizing
    }
    private static int currentDomain = 0;
    private static int currentSample = 0;
    private static int currentLanguage = 0;

    private static String arrayDomain[] = new String[] { "general", "poi", "song", "movietv", "medical" };
    private static String arrayDomainChina[] = new String[] { "通用识别  ", "地名识别  ", "歌名识别  ", "影视名识别  ", "医药领域识别  " };
    private static String arraySampleStr[] = new String[] { "RATE_AUTO  ", "RATE_16K  ", "RATE_8K  " };
    private static String arrayLanguageStr[] = new String[] { SpeechConstants.LANGUAGE_MANDARIN,
            SpeechConstants.LANGUAGE_ENGLISH, SpeechConstants.LANGUAGE_CANTONESE };
    private static int arraySample[] = new int[] { SpeechConstants.ASR_SAMPLING_RATE_BANDWIDTH_AUTO,
            SpeechConstants.ASR_SAMPLING_RATE_16K, SpeechConstants.ASR_SAMPLING_RATE_8K };

    private AsrStatus statue = AsrStatus.idle;
    private SpeechUnderstander mUnderstander;
    @SuppressWarnings("unused")
    private String mRecognizerText = "";
    private SpeechSynthesizer mTTSPlayer;
    private StringBuffer mAsrResultBuffer;


    private static final int DISTANCE_Y_CANCEL = 50;

    private static final int STATE_NORMAL = 1;
    private static final int STATE_RECORDING = 2;
    private static final int STATE_WANT_TO_CANCLE = 3;


    private int mCurState = STATE_NORMAL;

    //已经开始录音
    private boolean isRecording = false;

    private DialogManager mDialogManager;

    private String resultString = "";
    private SoundPool mSoundPool;//声明一个SoundPool
    private int music;//定义一个整型用load（）；来设置suondID
    public AudioRecorderButton(Context context) {
        this(context, null);

    }

    RecognitionCompletion mRecognitionCompletion;

    public AudioRecorderButton(final Context context, AttributeSet attrs) {

        super(context, attrs);
        mDialogManager = new DialogManager(getContext());
        mAsrResultBuffer = new StringBuffer();
        mRecognitionCompletion = (RecognitionCompletion) context;
        mSoundPool =  new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);//第一个参数为同时播放数据流的最大个数，第二数据流类型，第三为声音质量
        music = mSoundPool.load(context, R.raw.play_completed, 1); //把你的声音素材放到res/raw里，第2个参数即为资源文件，第3个为音乐的优先级


        initRecognizer(context);
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //TODO 真正显示应该在audio end prepared以后
                mDialogManager.showRecordingDialog();
                isRecording = true;
                mSoundPool.play(music, 1, 1, 0, 0, 1);
                VibratorUtil.Vibrate((Activity) context, 100);   //震动100ms

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

                return false;
            }
        });
    }

    private void initRecognizer(Context mContext) {
        // 创建语音理解对象，appKey和 secret通过 http://dev.hivoice.cn/ 网站申请
        mUnderstander = new SpeechUnderstander(mContext, Config.appKey, Config.secret);
        // 开启可变结果
        mUnderstander.setOption(SpeechConstants.ASR_OPT_TEMP_RESULT_ENABLE, true);

        // 创建语音合成对象
        mTTSPlayer = new SpeechSynthesizer(mContext, Config.appKey, Config.secret);
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
                        mDialogManager.updateVoiceLevel((int)(float)(0.07*volume));
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
                    Toast.makeText(getContext(),"没听到",Toast.LENGTH_SHORT).show();
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
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                changeState(STATE_RECORDING);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isRecording) {
                    if (wantToCancle(x, y)) {
                        changeState(STATE_WANT_TO_CANCLE);
                    } else {
                        changeState(STATE_RECORDING);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mCurState == STATE_RECORDING) {
                    mDialogManager.dimissDialog();
                    //release
                    //callBackToAct
                } else if (mCurState == STATE_WANT_TO_CANCLE) {

                    mDialogManager.dimissDialog();

                    // 取消识别
                    mUnderstander.cancel();
                    statue = AsrStatus.idle;
                }
                reset();
                break;
            default:
                break;
        }

        return super.onTouchEvent(event);
    }

    private void reset() {
        isRecording = false;
        changeState(STATE_NORMAL);
    }

    private boolean wantToCancle(int x, int y) {

        if (x < 0 || x > getWidth()) {
            return true;
        }
        if (y < -DISTANCE_Y_CANCEL || y > getHeight() + DISTANCE_Y_CANCEL) {
            return true;
        }
        return false;
    }

    private void changeState(int state) {
        if (mCurState != state) {
            mCurState = state;
            switch (state) {
                case STATE_NORMAL:
                    setBackgroundResource(R.drawable.btn_recorder_normal);
                    setText(R.string.str_recorder_normal);

                    break;
                case STATE_RECORDING:
                    setBackgroundResource(R.drawable.btn_recordingl);
                    setText(R.string.str_recorder_recording);
                    if (isRecording) {
                        mDialogManager.recording();
                    }
                    break;
                case STATE_WANT_TO_CANCLE:
                    setBackgroundResource(R.drawable.btn_recordingl);
                    setText(R.string.str_recorder_want_cancel);

                    mDialogManager.wantToCancel();
                    break;
            }
        }

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
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
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
            resultString = "";
//            mRecognizerResultText.setText("");
            if (asrJsonStatus.equals("change")) {
                resultString += mAsrResultBuffer.toString();
                resultString += asrJsonObject.getString("recognition_result");
                MainActivity.resultTv.setText(resultString );

            } else {
                mAsrResultBuffer.append(asrJsonObject.getString("recognition_result"));
                resultString += mAsrResultBuffer.toString();
                MainActivity.resultTv.setText(resultString);
                mRecognitionCompletion.RecognitionCompletion(resultString);

//                Toast.makeText(getContext(),resultString,Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public interface RecognitionCompletion{
        public void RecognitionCompletion(String sss);
    }
}
