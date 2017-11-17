# README(ENGLISH ver)
## Android-YouTube-Background-Player
This is an application that allows you to continue playing videos on youtube even if the app goes to the background.

- You can search videos on youtube and play public videos.
- You can play and edit the playlist you have created on youtube by linking to the google account linked to your smartphone.
- You can have one local favorite list without linking to google account.

<img src="https://user-images.githubusercontent.com/22609306/32922866-bd747b94-cb77-11e7-80ad-ea56a1560349.png" width="180" height="320"> <img src="https://user-images.githubusercontent.com/22609306/32922901-edd17c10-cb77-11e7-936d-6dd08d606cb0.png" width="180" height="320">
<img src="https://user-images.githubusercontent.com/22609306/32922904-f09bc7ca-cb77-11e7-86a0-642cda23f819.png" width="180" height="320"> 
<img src="https://user-images.githubusercontent.com/22609306/32922887-dd95f998-cb77-11e7-8237-c3b54332e139.png" width="180" height="320"> 
 <img src="https://user-images.githubusercontent.com/22609306/32923300-2a9af85e-cb7a-11e7-9bfb-e1635c99749d.png" width="180" height="320"> <img src="https://user-images.githubusercontent.com/22609306/32922906-f26939c0-cb77-11e7-88f9-90f2d4f02b42.png" width="320"  height="180">

##  Copyright holder

- Name: kkkkan
- Contact: kan4649kan@gmail.com
  - I'm happy if you can contact me in Japanese.

## What to do to use this app

1. Google Developers Console(https://console.developers.google.com/?hl=en)
Access to the project, create project, move to the authentication information.
1. Create Android's YouTubeApi (v3) API key and OAuth 2.0 client ID, add the package name com.kkkkan.youtube of this application and
Register the fingerprint of SHA - 1 certificate under your own environment.
1. Public static final String YOUTUBE_API_KEY of com.kkkkan.youtube.tubtub.utils.Config of the source code of this application,
Change to the API key you registered earlier.

## Known Bug

- Depending on the model, long-term videos may be played for 9 to 25 minutes and playback may end in the middle and playback of the next video may begin.
  - Machines with this phenomenon seen: FUJITSU arrows M02 (OS: andrid 6.0.1)
  - Machine that did not see this phenomenon: SONY SO - 01G (OS: android 5.0.2)

  
## Functions considering expansion in the future

- Shuffle playback function
- Ability to receive youtube video sharing from other apps



# README(日本語ver)

## Android-YouTube-Background-Player

これはyoutube上のビデオをアプリがバックグラウンドにいっても再生し続けられるアプリです。

- youtube上を検索して公開されているビデオを再生できます。
- スマートフォンに紐づけられているgoogle accountに連携する事でyoutube上に作成しているプレイリストの再生・編集ができます。
- google accountに連携しなくてもローカルでお気に入りリストを1つ持てます。

## 著作権者

- 名前 : kkkkan
- 連絡先 : kan4649kan@gmail.com
  - 日本語のできる方は日本語で連絡いただけると嬉しいです。
  

## このアプリを使うためにすべきこと

1. Google Developers Console(https://console.developers.google.com/?hl=en)
   　にアクセスしてprojectを作成し、認証情報へ移動します。
1. androidのYouTubeApi(v3)のAPIキーとOAuth 2.0 クライアント IDを作成し、それぞれこのアプリのパッケージ名com.kkkkan.youtubeと
自分の環境下でのSHA-1証明書のフィンガープリントを登録します。
1. このアプリのソースコードのcom.kkkkan.youtube.tubtub.utils.Configのpublic static final String YOUTUBE_API_KEYを,
先ほど登録したAPIキーに変更します。

## 既知のBug

- 機種によっては、長時間のビデオは、9～25分再生したところで途中で再生が終わってしまい次のビデオを再生し始めてしまうことがあります。
  - この現象の見られた機種:FUJITSU arrows M02(OS:andrid 6.0.1)
  - この現象の見られなかった機種:SONY SO-01G(OS:android 5.0.2)

  
## 今後拡張を考えている機能

- シャッフル再生機能
- 他のアプリからyoutubeビデオの共有を受け取る機能
