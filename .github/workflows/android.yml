name: Build and Sign APK & AAB

on:
  push:
    branches:
      - 'release/**'
  workflow_dispatch:

env:
  main_project_module: app
  playstore_name: Frogobox ID

jobs:
  build:

    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set current date as env variable
        run: echo "date_today=$(date +'%Y-%m-%d')" >> $GITHUB_ENV

      - name: Set repository name as env variable
        run: echo "repository_name=$(echo '${{ github.repository }}' | awk -F '/' '{print $2}')" >> $GITHUB_ENV

      - name: Set Up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          cache: 'gradle'

      - name: Change wrapper permissions
        run: chmod +x ./gradlew

      # دیکد کردن keystore از سکرت
      - name: Decode keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > release.keystore

      # بیلد release APK unsigned
      - name: Build APK Release
        run: ./gradlew assembleRelease

      # امضا APK با keystore
      - name: Sign APK
        run: |
          jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
          -keystore release.keystore \
          -storepass ${{ secrets.KEYSTORE_PASSWORD }} \
          -keypass ${{ secrets.KEY_PASSWORD }} \
          app/build/outputs/apk/release/app-release-unsigned.apk \
          ${{ secrets.KEY_ALIAS }}

      # بهینه سازی APK با zipalign
      - name: Align APK
        run: |
          zipalign -v 4 app/build/outputs/apk/release/app-release-unsigned.apk app-release-signed.apk

      # آپلود APK signed
      - name: Upload Signed APK
        uses: actions/upload-artifact@v4
        with:
          name: Signed APK - ${{ env.date_today }}
          path: app-release-signed.apk

      # بیلد AAB Release
      - name: Build AAB Release
        run: ./gradlew ${{ env.main_project_module }}:bundleRelease

      # آپلود AAB
      - name: Upload AAB Release
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env.repository_name }} - App bundle AAB release
          path: ${{ env.main_project_module }}/build/outputs/bundle/release/
