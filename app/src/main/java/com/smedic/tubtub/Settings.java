package com.smedic.tubtub;


/**
 * 横画面ロック・一曲リピート・プレイリストリピートの設定を
 * 入れておくsingletonクラス
 * Created by admin on 2017/11/07.
 */

public class Settings {
    final private static String TAG = "Settings";
    static private Settings settings = new Settings();
    private ScreenLock screenLock = ScreenLock.OFF;
    private RepeatOne repeatOne = RepeatOne.OFF;
    private RepeatPlaylist repeatPlaylist = RepeatPlaylist.OFF;

    private Settings() {

    }

    static public Settings getInstance() {
        return settings;
    }

    public ScreenLock getScreenLock() {
        return screenLock;
    }

    public RepeatOne getRepeatOne() {
        return repeatOne;
    }

    public RepeatPlaylist getRepeatPlaylist() {
        return repeatPlaylist;
    }

    public void setScreenLock(ScreenLock screenLock) {
        this.screenLock = screenLock;
    }

    public void setRepeatOne(RepeatOne repeatOne) {
        this.repeatOne = repeatOne;
    }

    public void setRepeatPlaylist(RepeatPlaylist repeatPlaylist) {
        this.repeatPlaylist = repeatPlaylist;
    }


    /**
     * 横画面ロックか否かのための
     * enum
     */
    public enum ScreenLock {
        ON,
        OFF
    }

    /**
     * 一曲リピートか否かのための
     * enum
     */
    public enum RepeatOne {
        ON,
        OFF
    }

    /**
     * プレイリストリピートか否かのための
     * enum
     */
    public enum RepeatPlaylist {
        ON,
        OFF
    }

}
