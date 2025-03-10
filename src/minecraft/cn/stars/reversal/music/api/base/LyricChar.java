/*
 * Reversal Client - A PVP Client with hack visual.
 * Copyright 2024 Starlight, All rights reserved.
 */
package cn.stars.reversal.music.api.base;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LyricChar {
    int charStartTime;
    int charDuration;
    String character;

    public LyricChar(int charStartTime, int charDuration, String character) {
        this.charStartTime = charStartTime;
        this.charDuration = charDuration;
        this.character = character;
    }
}
