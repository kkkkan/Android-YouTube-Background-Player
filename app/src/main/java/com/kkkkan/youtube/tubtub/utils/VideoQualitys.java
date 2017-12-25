/*
 * Copyright (C) 2017 kkkkan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kkkkan.youtube.tubtub.utils;

import java.util.HashMap;


/**
 * videoの画質の指定のためのタグなどの設定を定めたクラス
 * <p>
 * 参考にしたサイト：
 * https://github.com/kkkkan/android-youtubeExtractor/blob/master/youtubeExtractor/src/main/java/at/huber/youtubeExtractor/YouTubeExtractor.java
 * http://rdkblog.wp.xdomain.jp/videosite-soundquality
 * <p>
 * 160:MP4/144p
 * 133:MP4/240p
 * 278:MP4/144p
 * 5:flv/240p
 * 134:DASH/MP4/360p
 * は音声が再生されなかった。
 * <p>
 * ->MediaPlayerはdash形式非対応らしい。
 * http://shunirr.hatenablog.jp/entry/2015/09/16/231512
 * https://developer.android.com/guide/topics/media/exoplayer.html
 */

public class VideoQualitys { // XXX Qualities?
    final static public String VideoQualityPreferenceFileName = "VideoQualityPreferenceFileName";
    final static public String VideoQualityPreferenceKey = "VideoQualityPreferenceKey";
    final static private int videoQualitySuperLow = 0;
    final static private int videoQualityLow = 1;
    final static public int videoQualityNormal = 2;
    final static private int videoQualityHight = 3;

    static public CharSequence[] getVideoQualityChoices() {
        CharSequence[] qualityChoices = new CharSequence[4];
        qualityChoices[videoQualitySuperLow] = "144p (超低画質)";
        qualityChoices[videoQualityLow] = "240p (低画質)";
        qualityChoices[videoQualityNormal] = "360p (標準)";
        qualityChoices[videoQualityHight] = "720p (高画質)";
        return qualityChoices;
    }

    static public HashMap<Integer, Integer[]> getVideoQualityTagsMap() {
        HashMap<Integer, Integer[]> tagMap = new HashMap<>();
        tagMap.put(videoQualitySuperLow, videoQualityTagUnder144);
        tagMap.put(videoQualityLow, videoQualityTagUnder240);
        tagMap.put(videoQualityNormal, videoQualityTagUnder360);
        tagMap.put(videoQualityHight, videoQualityTagUnder720);
        return tagMap;
    }

    final static private Integer[] videoQualityTagUnder144 = {
            17,//17:3gp/144p
            91//91:Live Stream/144p
    };

    final static private Integer[] videoQualityTagUnder240 = {
            36,//36:3gp/240p
            92, //92:Live Stream/240p

            17,//17:3gp/144p
            91//91:Live Stream/144p
    };

    final static private Integer[] videoQualityTagUnder360 = {
            18,//18:Non-DASH/MP4/360p
            43,//43:Non-DASH/WEBM/360p
            93,//93:Live Stream/360p

            36,//36:MP3/240p
            92, //92:Live Stream/240p

            17,//17:MP3/144p
            91//91:Live Stream/144p
    };

    final static private Integer[] videoQualityTagUnder720 = {
            22,//22:Non-DASH/MP4/720p
            95,//95:Live Stream/720p

            18,//18:Non-DASH/MP4/360p
            43,//43:Non-DASH/WEBM/360p
            93,//93:Live Stream/360p

            36,//36:3gp/240p
            92, //92:Live Stream/240p

            17,//17:3gp/144p
            91//91:Live Stream/144p

    };
}
