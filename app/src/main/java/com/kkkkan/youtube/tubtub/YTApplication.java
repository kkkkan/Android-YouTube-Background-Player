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
package com.kkkkan.youtube.tubtub;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.kkkkan.youtube.tubtub.database.YouTubeSqlDb;

import io.fabric.sdk.android.Fabric;

/**
 * Class for obtaining application context
 * <p>
 * アプリコンテキスト取得のためのクラス
 * <p>
 * Created by smedic on 5.3.17..
 */
public class YTApplication extends Application {

    private static Context mContext;

    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        mContext = getApplicationContext();
        YouTubeSqlDb.getInstance().init(this);
    }

    public static Context getAppContext() {
        return mContext;
    }

}
