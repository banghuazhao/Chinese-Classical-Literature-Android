package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** One utterance at a time keeps pause/resume and passage tracking in sync. */
public final class ReaderSpeech {
    public interface Listener {
        void onPosition(boolean playing, int start, int end);
        void onUnavailable();
    }

    private final Context context;
    private final Listener listener;
    private final SharedPreferences prefs;
    private TextToSpeech tts;
    private String text = "";
    private boolean ready, playing, traditional;
    private int chunkStart, chunkEnd;
    private volatile int lastSpokenOffset;
    private long sequence;

    public ReaderSpeech(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        prefs = this.context.getSharedPreferences("reader_speech", Context.MODE_PRIVATE);
    }

    public boolean isPlaying() { return playing; }
    public boolean hasSession() { return !text.isEmpty(); }
    public boolean isReady() { return ready; }
    public float speed() { return prefs.getFloat("speed", 1f); }
    public void setSpeed(float speed) {
        prefs.edit().putFloat("speed", speed).apply();
        if (tts != null && ready) tts.setSpeechRate(speed);
    }

    public List<Voice> voices() {
        List<Voice> voices = new ArrayList<>();
        if (tts == null || !ready || tts.getVoices() == null) return voices;
        for (Voice voice : tts.getVoices()) {
            if (voice.getLocale().getLanguage().equals("zh"))
                voices.add(voice);
        }
        Collections.sort(voices, (left, right) -> left.getName().compareTo(right.getName()));
        return voices;
    }

    public String voiceName() { return prefs.getString("voice", ""); }
    public void setVoice(Voice voice) {
        prefs.edit().putString("voice", voice.getName()).apply();
        if (tts != null && ready) tts.setVoice(voice);
    }

    public void start(String chapter, boolean useTraditional, int offset) {
        stop();
        text = chapter == null ? "" : chapter;
        traditional = useTraditional;
        chunkStart = Math.max(0, Math.min(offset, text.length()));
        lastSpokenOffset = chunkStart;
        if (text.isEmpty()) return;
        playing = true;
        listener.onPosition(true, chunkStart, chunkStart);
        tts = new TextToSpeech(context, status -> {
            if (!playing || tts == null) return;
            if (status != TextToSpeech.SUCCESS) { fail(); return; }
            int language = tts.setLanguage(traditional ? Locale.TRADITIONAL_CHINESE : Locale.SIMPLIFIED_CHINESE);
            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                language = tts.setLanguage(Locale.CHINESE);
                if (language == TextToSpeech.LANG_MISSING_DATA
                        || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    fail(); return;
                }
            }
            ready = true;
            tts.setSpeechRate(speed());
            for (Voice voice : voices()) {
                if (voice.getName().equals(voiceName())) { tts.setVoice(voice); break; }
            }
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { report(id, 0, chunkEnd - chunkStart); }
                @Override public void onDone(String id) {
                    contextMain(() -> {
                        if (!playing || !id.equals(String.valueOf(sequence))) return;
                        chunkStart = chunkEnd;
                        speakNext();
                    });
                }
                @Override public void onError(String id) { contextMain(() -> { if (playing) fail(); }); }
                @Override public void onRangeStart(String id, int start, int end, int frame) {
                    lastSpokenOffset = chunkStart + start;
                    report(id, start, end);
                }
            });
            speakNext();
        });
    }

    private void report(String id, int start, int end) {
        contextMain(() -> {
            if (playing && id.equals(String.valueOf(sequence)))
                listener.onPosition(true, chunkStart + start, chunkStart + end);
        });
    }

    private void speakNext() {
        if (!playing || !ready || tts == null) return;
        while (chunkStart < text.length() && Character.isWhitespace(text.charAt(chunkStart))) chunkStart++;
        if (chunkStart >= text.length()) { stop(); return; }
        int max = Math.min(text.length(), chunkStart + Math.min(450,
                TextToSpeech.getMaxSpeechInputLength() - 1));
        chunkEnd = max;
        if (max < text.length()) {
            for (int i = max - 1; i > chunkStart + 90; i--) {
                char c = text.charAt(i);
                if (c == '。' || c == '！' || c == '？' || c == '\n' || c == '.' || c == '!' || c == '?') {
                    chunkEnd = i + 1; break;
                }
            }
        }
        long id = ++sequence;
        if (tts.speak(text.substring(chunkStart, chunkEnd), TextToSpeech.QUEUE_FLUSH,
                null, String.valueOf(id)) == TextToSpeech.ERROR) fail();
    }

    public void pause() {
        if (!playing) return;
        playing = false;
        sequence++;
        if (tts != null) tts.stop();
        chunkStart = Math.max(chunkStart, Math.min(lastSpokenOffset, chunkEnd));
        listener.onPosition(false, chunkStart, chunkStart);
    }

    public void resume() {
        if (playing || text.isEmpty()) return;
        playing = true;
        if (ready) speakNext(); else start(text, traditional, chunkStart);
    }

    public void stop() {
        playing = false;
        ready = false;
        sequence++;
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        text = "";
        chunkStart = 0;
        chunkEnd = 0;
        listener.onPosition(false, -1, -1);
    }

    private void fail() { stop(); listener.onUnavailable(); }
    private void contextMain(Runnable action) { new android.os.Handler(context.getMainLooper()).post(action); }
}
