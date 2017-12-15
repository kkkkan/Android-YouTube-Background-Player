# README(ENGLISH ver)
## Android-YouTube-Background-Player
This is an application that allows you to continue playing videos on youtube even if the app goes to the background.

- You can search videos on youtube and play public videos.
- You can play and edit the playlist you have created on youtube by linking to the google account linked to your smartphone.
- You can have one local favorite list without linking to google account.
- By choosing this application from the sharing function in the YouTube official application, you can play videos with this application.
- Shuffle play, repeat play, one repeat play can be done.

<img src="https://user-images.githubusercontent.com/22609306/33920218-1e259d82-e000-11e7-8a8b-350aeb911957.png" width="180" height="320"> <img src="https://user-images.githubusercontent.com/22609306/33920413-260d1240-e001-11e7-926a-0e875adb4c15.png" width="180" height="320">
<img src="https://user-images.githubusercontent.com/22609306/33920741-e9529706-e002-11e7-9160-7666419a5a42.png" width="180" height="320">
<img src="https://user-images.githubusercontent.com/22609306/33920273-617565f4-e000-11e7-8b01-14a868807d70.png" width="180" height="320"> 
<img src="https://user-images.githubusercontent.com/22609306/33920247-422b6b08-e000-11e7-9be7-4456c787022b.png" width="180" height="320"> 
<img src="https://user-images.githubusercontent.com/22609306/33920274-634c15da-e000-11e7-9456-348c4ec8c7a0.png" width="180" height="320"> 
<img src="https://user-images.githubusercontent.com/22609306/33920299-90264558-e000-11e7-9245-8a1e3f6b1ceb.png" width="180" height="320"> 
<img src="https://user-images.githubusercontent.com/22609306/33920297-8e0138c8-e000-11e7-8538-30c9cc2bf724.png" width="180" height="320">
<img src="https://user-images.githubusercontent.com/22609306/33920221-224789d4-e000-11e7-8602-b6a19f64b317.png" width="180" height="320">
<img src="https://user-images.githubusercontent.com/22609306/33920242-3e14002a-e000-11e7-8f65-e5728e5051a9.png" width="320" height="180"> <img src="https://user-images.githubusercontent.com/22609306/33920240-3c0491aa-e000-11e7-8061-0e102ecba9e4.png" width="320" height="180"> <img src="https://user-images.githubusercontent.com/22609306/33920224-25817d12-e000-11e7-8771-4e69f4b4b871.png" width="180" height="320">


## release information

- Latest release information (ver 1.2.0)

  - https://github.com/kkkkan/Android-YouTube-Background-Player/releases/tag/ver_1.2.0_%E3%83%AA%E3%83%AA%E3%83%BC%E3%82%B9

- APK download

  - https://github.com/kkkkan/Android-YouTube-Background-Player/releases/download/ver_1.2.0_%E3%83%AA%E3%83%AA%E3%83%BC%E3%82%B9/com.kkkkan.youtube-release.apk



##  Copyright holder

- Name: kkkkan
- Contact: kan4649kan@gmail.com
  - I'm happy if you can contact me in Japanese.
  
- from fork:smedic(https://github.com/smedic/Android-YouTube-Background-Player)

## What to do to use this app


1. Google Developers Console(https://console.developers.google.com/?hl=en)
Access to the project, create project, move to the authentication information.
1. Create ~Android's~ YouTubeApi (v3) API key and OAuth 2.0 client ID, add the package name com.kkkkan.youtube of this application and
Register the fingerprint of SHA - 1 certificate under your own environment. **Since it seems that there is an error when you set the API key limit to android, it seems better not limit the API key.**
1. Public static final String YOUTUBE_API_KEY of com.kkkkan.youtube.tubtub.utils.Config of the source code of this application,
Change to the API key you registered earlier.


- We are checking the build in the next development environment.
  - Android Studio 3.0.1

<img src="https://user-images.githubusercontent.com/22609306/32926054-656fb22c-cb88-11e7-8a8a-a05c47af6a41.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/32926056-67344aa0-cb88-11e7-8f9b-204c632d5916.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/32926062-6b66f992-cb88-11e7-9fc8-2e1fdddbae27.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/32926058-6937df7e-cb88-11e7-884e-c766a607a23f.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/32997295-48585ca2-cdd1-11e7-80cf-5a6b1e3563bf.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/33010535-2a354364-ce1e-11e7-834f-b9ce11deba4e.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/32926067-7597db16-cb88-11e7-9f7a-857c8a7be05b.png" width="420"  height="180">

## Known Bug

- ~~Depending on the model, long-term videos may be played for 9 to 25 minutes and playback may end in the middle and playback of the next video may begin.~~

  - ~~Machines with this phenomenon seen: FUJITSU arrows M02 (OS: andrid 6.0.1)~~
  
  - ~~Machine that did not see this phenomenon: SONY SO - 01G (OS: android 5.0.2)~~
  
  </br>
  
  **This phenomenon can be seen when playing back with MP4 (playback image quality 360p / 720p), but this phenomenon can not be seen in 3GP playback (240p / 144p).**
  
  
## Functions considering expansion in the future




# README(日本語ver)

## Android-YouTube-Background-Player

これはyoutube上のビデオをアプリがバックグラウンドにいっても再生し続けられるアプリです。

- youtube上を検索して公開されているビデオを再生できます。　
- スマートフォンに紐づけられているgoogle accountに連携する事でyoutube上に作成しているプレイリストの再生・編集ができます。
- google accountに連携しなくてもローカルでお気に入りリストを1つ持てます。
- YouTube公式アプリなどで、共有機能からこのアプリを選ぶことで、このアプリで動画再生できます。
- シャッフル再生、リピート再生、1曲リピート再生が出来ます。



<img src="https://user-images.githubusercontent.com/22609306/33920218-1e259d82-e000-11e7-8a8b-350aeb911957.png" width="180" height="320"> <img src="https://user-images.githubusercontent.com/22609306/33920413-260d1240-e001-11e7-926a-0e875adb4c15.png" width="180" height="320">
<img src="https://user-images.githubusercontent.com/22609306/33920741-e9529706-e002-11e7-9160-7666419a5a42.png" width="180" height="320">
<img src="https://user-images.githubusercontent.com/22609306/33920273-617565f4-e000-11e7-8b01-14a868807d70.png" width="180" height="320"> 
<img src="https://user-images.githubusercontent.com/22609306/33920247-422b6b08-e000-11e7-9be7-4456c787022b.png" width="180" height="320"> 
<img src="https://user-images.githubusercontent.com/22609306/33920274-634c15da-e000-11e7-9456-348c4ec8c7a0.png" width="180" height="320"> 
<img src="https://user-images.githubusercontent.com/22609306/33920299-90264558-e000-11e7-9245-8a1e3f6b1ceb.png" width="180" height="320"> 
<img src="https://user-images.githubusercontent.com/22609306/33920297-8e0138c8-e000-11e7-8538-30c9cc2bf724.png" width="180" height="320">
<img src="https://user-images.githubusercontent.com/22609306/33920221-224789d4-e000-11e7-8602-b6a19f64b317.png" width="180" height="320">
<img src="https://user-images.githubusercontent.com/22609306/33920242-3e14002a-e000-11e7-8f65-e5728e5051a9.png" width="320" height="180"> <img src="https://user-images.githubusercontent.com/22609306/33920240-3c0491aa-e000-11e7-8061-0e102ecba9e4.png" width="320" height="180"> <img src="https://user-images.githubusercontent.com/22609306/33920224-25817d12-e000-11e7-8771-4e69f4b4b871.png" width="180" height="320">

## リリース情報

- 最新版リリース情報 (ver 1.2.0)

  - https://github.com/kkkkan/Android-YouTube-Background-Player/releases/tag/ver_1.2.0_%E3%83%AA%E3%83%AA%E3%83%BC%E3%82%B9

- APK download

  - https://github.com/kkkkan/Android-YouTube-Background-Player/releases/download/ver_1.2.0_%E3%83%AA%E3%83%AA%E3%83%BC%E3%82%B9/com.kkkkan.youtube-release.apk



## 著作権者

- 名前 : kkkkan
- 連絡先 : kan4649kan@gmail.com
  - 日本語のできる方は日本語で連絡いただけると嬉しいです。
  
- fork元:smedic(https://github.com/smedic/Android-YouTube-Background-Player)
  

## このアプリを使うためにすべきこと

1. Google Developers Console(https://console.developers.google.com/?hl=en)
   　にアクセスしてprojectを作成し、認証情報へ移動します。
1. ~androidの~ YouTubeApi(v3)のAPIキーとOAuth 2.0 クライアント IDを作成し、それぞれこのアプリのパッケージ名com.kkkkan.youtubeと
自分の環境下でのSHA-1証明書のフィンガープリントを登録します。　**APIキーの制限をandroidにするとエラーが出ることがあるようなので、APIキーに制限はかけない方がよさそうです。**
1. このアプリのソースコードのcom.kkkkan.youtube.tubtub.utils.Configのpublic static final String YOUTUBE_API_KEYを,
先ほど登録したAPIキーに変更します。


- 次の開発環境でのビルドを確認しています。
  - Android Studio 3.0.1


<img src="https://user-images.githubusercontent.com/22609306/32926054-656fb22c-cb88-11e7-8a8a-a05c47af6a41.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/32926056-67344aa0-cb88-11e7-8f9b-204c632d5916.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/32926062-6b66f992-cb88-11e7-9fc8-2e1fdddbae27.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/32926058-6937df7e-cb88-11e7-884e-c766a607a23f.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/32997295-48585ca2-cdd1-11e7-80cf-5a6b1e3563bf.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/33010535-2a354364-ce1e-11e7-834f-b9ce11deba4e.png" width="420"  height="180"> <img src="https://user-images.githubusercontent.com/22609306/32926067-7597db16-cb88-11e7-9f7a-857c8a7be05b.png" width="420"  height="180">


## 既知のBug

-  ~~機種によっては、長時間のビデオは、9～25分再生したところで途中で再生が終わってしまい次のビデオを再生し始めてしまうことがあります。~~
  
  - ~~この現象の見られた機種:FUJITSU arrows M02(OS:andrid 6.0.1)~~
  
  - ~~この現象の見られなかった機種:SONY SO-01G(OS:android 5.0.2)~~
  
  </br>
  
  **MP4で再生しているとき（再生画質360p/720p)のときはこの現象が見られますが、3GP再生（240p/144p）のときは見られませんでした。**

  
## 今後拡張を考えている機能



