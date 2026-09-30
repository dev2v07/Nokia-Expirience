package com.nokiaexperience.launcher;

import android.graphics.Color;

/** Replaceable visual tokens. Undocumented pixel values are provisional, not Nokia measurements. */
public interface VisualProfile {
    String id();
    String title();
    int referenceWidth(boolean landscape);
    int referenceHeight(boolean landscape);
    int background();
    int panel();
    int panelLight();
    int text();
    int muted();
    int accent();
    int selection();
    float statusHeight();
    float softkeyHeight();
    float titleHeight();
    int columns();
    int rows();

    final class N95 implements VisualProfile {
        public String id() { return "n95"; }
        public String title() { return "Nokia N95"; }
        public int referenceWidth(boolean landscape) { return landscape ? 320 : 240; }
        public int referenceHeight(boolean landscape) { return landscape ? 240 : 320; }
        public int background() { return Color.rgb(205, 218, 226); }
        public int panel() { return Color.rgb(46, 57, 72); }
        public int panelLight() { return Color.rgb(125, 146, 162); }
        public int text() { return Color.rgb(22, 31, 42); }
        public int muted() { return Color.rgb(70, 86, 101); }
        public int accent() { return Color.rgb(39, 82, 138); }
        public int selection() { return Color.rgb(116, 160, 204); }
        public float statusHeight() { return 28f; }
        public float softkeyHeight() { return 26f; }
        public float titleHeight() { return 22f; }
        public int columns() { return 3; }
        public int rows() { return 6; }
    }

    final class N5530 implements VisualProfile {
        public String id() { return "5530"; }
        public String title() { return "Nokia 5530"; }
        public int referenceWidth(boolean landscape) { return landscape ? 640 : 360; }
        public int referenceHeight(boolean landscape) { return landscape ? 360 : 640; }
        public int background() { return Color.rgb(31, 34, 38); }
        public int panel() { return Color.rgb(13, 15, 18); }
        public int panelLight() { return Color.rgb(61, 65, 68); }
        public int text() { return Color.WHITE; }
        public int muted() { return Color.rgb(199, 202, 205); }
        public int accent() { return Color.rgb(211, 56, 51); }
        public int selection() { return Color.rgb(109, 51, 51); }
        public float statusHeight() { return 47f; }
        public float softkeyHeight() { return 54f; }
        public float titleHeight() { return 44f; }
        public int columns() { return 3; }
        public int rows() { return 5; }
    }
}
