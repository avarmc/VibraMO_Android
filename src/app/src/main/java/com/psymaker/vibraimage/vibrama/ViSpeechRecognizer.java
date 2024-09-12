package com.psymaker.vibraimage.vibrama;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import com.psymaker.vibraimage.vibrama.med.R;

import java.util.ArrayList;
import java.util.Locale;

public class ViSpeechRecognizer {
    private SpeechRecognizer speechRecognizer;
    private VibraimageActivityBase mApp;
    private Intent speechIntent;
    private int current_volume = -1;

    public ViSpeechRecognizer(VibraimageActivityBase mApp) {
        this.mApp = mApp;

    }

    private void initSpeechRecognizer() {
        Context context = mApp.getApp();

        // Create the speech recognizer and set the listener
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(recognitionListener);

        // Create the intent with ACTION_RECOGNIZE_SPEECH
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);

    }

    public boolean isEnabled() {
        if( !mApp.isFragmentVI() )
            return false;
        if( jni.EngineGetIt("VI_INFO_VOICE_ENABLE") == 0 )
            return false;
        return true;
    }

    public boolean listen() {
        if( !isEnabled() ) {
            cancel();
            return false;
        }
        if(speechRecognizer == null)
            initSpeechRecognizer();

        audioMute(true);

        // startListening should be called on Main thread
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = () -> speechRecognizer.startListening(speechIntent);
        mainHandler.post(myRunnable);

        return true;
    }

    public void cancel() {
        if(speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer = null;
        }
        speechIntent = null;

        audioMute(false);
    }

    private void audioMute(boolean bMute) {
        if(bMute) {
            AudioManager audio = (AudioManager) mApp.getApp().getSystemService(Context.AUDIO_SERVICE);
            current_volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        } else
        if(current_volume >= 0) {
            AudioManager audio = (AudioManager) mApp.getApp().getSystemService(Context.AUDIO_SERVICE);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, current_volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            current_volume = -1;
        }
    }

    RecognitionListener recognitionListener = new RecognitionListener() {


        @Override
        public void onReadyForSpeech(Bundle params) {
            audioMute(false);
        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            audioMute(false);
        }

        @Override
        public void onError(int errorCode) {
            audioMute(false);

            // ERROR_NO_MATCH states that we didn't get any valid input from the user
            // ERROR_SPEECH_TIMEOUT is invoked after getting many ERROR_NO_MATCH
            // In these cases, let's restart listening.
            // It is not recommended by Google to listen continuously user input, obviously it drains the battery as well,
            // but for now let's ignore this warning
            if ((errorCode == SpeechRecognizer.ERROR_NO_MATCH) || (errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                listen();
            } else {
            }
        }

        @Override
        public void onResults(Bundle bundle) {
            audioMute(false);

            // it returns 5 results as default.
            ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            if( testString(results,jni.EngineGetStrt("VI_INFO_VOICE_STR1")) )
                mApp.onVoice("start");
            else
            if( testString(results,jni.EngineGetStrt("VI_INFO_VOICE_STR2")) )
                mApp.onVoice("stop");
            else
            if( testString(results,jni.EngineGetStrt("VI_INFO_VOICE_STR3")) )
                mApp.onVoice("reset");

            listen();
        }


        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }

    };

    private boolean testString(ArrayList<String> results, String a) {
        String[] separated = a.split(" ");
        boolean check[] = new boolean[separated.length];

        int nr = results.size();

        for(int j = 0; j < separated.length; ++j )
            check[j] = false;

        for(int i = 0; i < nr; ++i )
            for(int j = 0; j < separated.length; ++j )
                if( separated[j].equalsIgnoreCase(results.get(i)) )
                    check[j] = true;

        for(int j = 0; j < separated.length; ++j )
            if(! check[j] )
                return false;

        return true;
    }
}
