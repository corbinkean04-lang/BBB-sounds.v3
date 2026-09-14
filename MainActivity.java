package com.bbbsounds.quest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 42;
    private static final String PREFS = "bbb_sounds";
    private static final String CUSTOM_URIS = "custom_uris";
    private static final String CUSTOM_NAMES = "custom_names";

    private final ArrayList<SoundItem> sounds = new ArrayList<>();
    private final ArrayList<MediaPlayer> players = new ArrayList<>();
    private final Map<String, MediaPlayer> active = new HashMap<>();

    private LinearLayout board;
    private SeekBar masterSeek;
    private TextView masterPercent;
    private float masterVolume = 0.70f;

    private int red = Color.rgb(255, 32, 32);
    private int black = Color.rgb(5, 5, 5);
    private int panel = Color.rgb(16, 16, 16);
    private int white = Color.WHITE;
    private int gray = Color.rgb(170, 170, 170);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(black);

        loadSounds();
        buildUi();
    }

    private void loadSounds() {
        sounds.clear();
        sounds.add(new SoundItem("Trollface Phonk Edit", R.raw.trollface_phonk_edit, true, null));
        sounds.add(new SoundItem("Mr Beast Phonk", R.raw.mr_beast_phonk, true, null));
        sounds.add(new SoundItem("Snow White Trailer Comments", R.raw.snow_white_trailer_comments, true, null));

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String uriString = p.getString(CUSTOM_URIS, "");
        String namesString = p.getString(CUSTOM_NAMES, "");
        if (!uriString.isEmpty()) {
            String[] uris = uriString.split("\\|", -1);
            String[] names = namesString.split("\\|", -1);
            for (int i = 0; i < uris.length; i++) {
                if (uris[i].trim().isEmpty()) continue;
                String name = (i < names.length && !names[i].isEmpty()) ? names[i] : "Custom Sound";
                sounds.add(new SoundItem(name, 0, false, Uri.parse(uris[i])));
            }
        }
    }

    private void saveCustomSounds() {
        SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        StringBuilder uris = new StringBuilder();
        StringBuilder names = new StringBuilder();
        for (SoundItem s : sounds) {
            if (s.locked) continue;
            if (uris.length() > 0) { uris.append("|"); names.append("|"); }
            uris.append(s.uri.toString().replace("|", ""));
            names.append(s.name.replace("|", ""));
        }
        e.putString(CUSTOM_URIS, uris.toString());
        e.putString(CUSTOM_NAMES, names.toString());
        e.apply();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(black);

        board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(24, 18, 24, 18);
        scroll.addView(board);

        // Header
        LinearLayout header = row();
        TextView menu = label("☰", 34, white, true);
        TextView title = label("BBB", 50, white, true);
        TextView soundsTitle = label(" SOUNDS", 42, red, true);
        TextView tagline = label("PLAY ANYTHING. ANYTIME.", 18, white, true);
        header.addView(menu, weight(1));
        header.addView(title, weight(1));
        header.addView(soundsTitle, weight(1));
        header.addView(tagline, weight(2));
        board.addView(header, new LinearLayout.LayoutParams(-1, 105));

        TextView mainHeader = sectionTitle("MAIN SOUNDS     🔒 PRELOADED • CANNOT BE DELETED");
        board.addView(mainHeader);

        LinearLayout mainGrid = row();
        for (int i = 0; i < 3; i++) mainGrid.addView(soundCard(sounds.get(i)), weight(1));
        board.addView(mainGrid);

        board.addView(sectionTitle("CUSTOM SOUNDS"));

        LinearLayout customGrid = row();
        int count = 0;
        for (int i = 3; i < sounds.size(); i++) {
            customGrid.addView(soundCard(sounds.get(i)), weight(1));
            count++;
            if (count == 5) {
                board.addView(customGrid);
                customGrid = row();
                count = 0;
            }
        }
        if (count > 0) board.addView(customGrid);

        // Controls
        LinearLayout controls = row();
        TextView mv = label("🔊  Master Volume", 18, white, true);
        controls.addView(mv, weight(1));
        masterSeek = new SeekBar(this);
        masterSeek.setMax(100);
        masterSeek.setProgress((int)(masterVolume * 100));
        masterSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                masterVolume = p / 100f;
                masterPercent.setText(p + "%");
                for (MediaPlayer mp : active.values()) {
                    try { mp.setVolume(masterVolume, masterVolume); } catch (Exception ignored) {}
                }
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        controls.addView(masterSeek, weight(2));
        masterPercent = label("70%", 18, white, true);
        controls.addView(masterPercent, weight(0.4f));
        board.addView(controls, new LinearLayout.LayoutParams(-1, 75));

        LinearLayout buttons = row();
        Button loopAll = actionButton("⟳  LOOP ALL");
        loopAll.setOnClickListener(v -> {
            for (SoundItem s : sounds) s.loop = true;
            refreshBoard();
        });
        Button stop = actionButton("■  STOP ALL");
        stop.setOnClickListener(v -> stopAll());
        Button add = actionButton("＋  ADD SOUND");
        add.setOnClickListener(v -> pickSound());
        buttons.addView(loopAll, weight(1));
        buttons.addView(stop, weight(1));
        buttons.addView(add, weight(1));
        board.addView(buttons, new LinearLayout.LayoutParams(-1, 78));

        TextView nav = label("⌂  Sounds        ▣  My Sounds        ⚙  Settings        ◉  Quest Mode", 18, white, true);
        nav.setGravity(Gravity.CENTER);
        board.addView(nav, new LinearLayout.LayoutParams(-1, 65));

        setContentView(scroll);
    }

    private void refreshBoard() {
        loadSounds();
        buildUi();
    }

    private View soundCard(SoundItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 12, 16, 12);
        GradientDrawableCompat.set(card, item.locked ? red : red, panel);

        TextView name = label((item.locked ? "🔒 " : "") + item.name, 17, white, true);
        name.setMaxLines(2);
        card.addView(name, new LinearLayout.LayoutParams(-1, 58));

        LinearLayout actions = row();
        Button play = smallButton("▶");
        play.setOnClickListener(v -> play(item));
        Button loop = smallButton(item.loop ? "↻ ON" : "↻ LOOP");
        loop.setOnClickListener(v -> {
            item.loop = !item.loop;
            loop.setText(item.loop ? "↻ ON" : "↻ LOOP");
        });
        actions.addView(play, weight(1));
        actions.addView(loop, weight(1));
        if (!item.locked) {
            Button del = smallButton("✕");
            del.setOnClickListener(v -> deleteSound(item));
            actions.addView(del, weight(0.6f));
        }
        card.addView(actions, new LinearLayout.LayoutParams(-1, 58));

        SeekBar vol = new SeekBar(this);
        vol.setMax(100);
        vol.setProgress(70);
        vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                MediaPlayer mp = active.get(item.key());
                if (mp != null) {
                    float v = (p / 100f) * masterVolume;
                    try { mp.setVolume(v, v); } catch (Exception ignored) {}
                }
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        card.addView(vol, new LinearLayout.LayoutParams(-1, 50));
        return card;
    }

    private void play(SoundItem item) {
        stop(item.key());
        MediaPlayer mp = null;
        try {
            if (item.locked) {
                mp = MediaPlayer.create(this, item.resId);
            } else {
                mp = new MediaPlayer();
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                mp.setDataSource(this, item.uri);
                mp.prepare();
            }
            mp.setLooping(item.loop);
            mp.setVolume(masterVolume, masterVolume);
            MediaPlayer finalMp = mp;
            active.put(item.key(), mp);
            mp.setOnCompletionListener(x -> {
                if (!item.loop) {
                    active.remove(item.key());
                    try { x.release(); } catch (Exception ignored) {}
                }
            });
            mp.start();
        } catch (Exception ex) {
            if (mp != null) try { mp.release(); } catch (Exception ignored) {}
            Toast.makeText(this, "Couldn't play " + item.name, Toast.LENGTH_SHORT).show();
        }
    }

    private void stop(String key) {
        MediaPlayer mp = active.remove(key);
        if (mp != null) {
            try { mp.stop(); } catch (Exception ignored) {}
            try { mp.release(); } catch (Exception ignored) {}
        }
    }

    private void stopAll() {
        for (MediaPlayer mp : new ArrayList<>(active.values())) {
            try { mp.stop(); } catch (Exception ignored) {}
            try { mp.release(); } catch (Exception ignored) {}
        }
        active.clear();
    }

    private void pickSound() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, PICK_AUDIO);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_AUDIO || resultCode != RESULT_OK || data == null) return;

        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++)
                addUri(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) {
            addUri(data.getData());
        }
        saveCustomSounds();
        refreshBoard();
    }

    private void addUri(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        String name = uri.getLastPathSegment();
        if (name == null || name.isEmpty()) name = "Custom Sound";
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        if (name.length() > 28) name = name.substring(0, 28);
        sounds.add(new SoundItem(name, 0, false, uri));
    }

    private void deleteSound(SoundItem item) {
        if (item.locked) return; // Main-board sounds are intentionally undeletable.
        stop(item.key());
        sounds.remove(item);
        saveCustomSounds();
        refreshBoard();
    }

    @Override protected void onDestroy() {
        stopAll();
        super.onDestroy();
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private TextView sectionTitle(String s) {
        TextView t = label(s, 24, white, true);
        t.setPadding(12, 12, 12, 12);
        return t;
    }

    private TextView label(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button smallButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(white);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setBackgroundColor(red);
        return b;
    }

    private Button actionButton(String s) {
        Button b = smallButton(s);
        b.setTextSize(16);
        return b;
    }

    private LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, -1, w);
    }

    static class SoundItem {
        String name;
        int resId;
        boolean locked;
        Uri uri;
        boolean loop = false;
        SoundItem(String n, int r, boolean l, Uri u) {
            name = n; resId = r; locked = l; uri = u;
        }
        String key() {
            return locked ? "raw:" + resId : "uri:" + uri.toString();
        }
    }

    static class GradientDrawableCompat {
        static void set(View v, int stroke, int fill) {
            android.graphics.drawable.GradientDrawable d =
                    new android.graphics.drawable.GradientDrawable();
            d.setColor(fill);
            d.setStroke(3, stroke);
            d.setCornerRadius(22);
            v.setBackground(d);
        }
    }
}
