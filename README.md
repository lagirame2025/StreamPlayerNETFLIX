# Stream Player

Aplikasi Android sederhana (Kotlin + Media3 ExoPlayer) untuk memutar video dari server milik Anda sendiri.

Format yang didukung: MP4, MKV, HLS (.m3u8), DASH (.mpd). Mendukung Basic Auth (username/password) dan menyimpan daftar URL.

## Cara build APK lewat GitHub
1. Buat repository baru di GitHub, upload semua isi folder ini (termasuk folder `.github`).
2. Buka tab **Actions** > **Build APK** > **Run workflow** (atau otomatis berjalan saat push ke `main`).
3. Setelah hijau, buka run tersebut, unduh artifact **StreamPlayer-debug-apk**, ekstrak, lalu pasang `app-debug.apk` di hape (izinkan "install dari sumber tidak dikenal").

## Contoh server
- **Jellyfin / Plex / Emby**: gunakan URL direct stream dari server Anda.
- **Nginx / Caddy / Apache** dengan folder file video + Basic Auth.
- **Server HLS** milik sendiri (mis. hasil `ffmpeg -i film.mkv -hls_time 6 -hls_playlist_type vod out.m3u8`).
- Akses dari luar rumah: gunakan Tailscale atau domain HTTPS agar aman.

Gunakan hanya untuk konten yang Anda miliki atau berhak putar.
