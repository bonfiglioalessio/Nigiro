# Nigiro — Lite Android TV Client for Jellyfin

<p align="center">
  <img src="assets/nigiro.jpeg" alt="Nigiro" width="320" />
</p>

**Nigiro** è un fork personalizzato, alleggerito e ottimizzato per Android TV (incluse TV 32-bit `armeabi-v7a` con risorse limitate) derivato da [Wholphin](https://github.com/damontecres/Wholphin), progettato con un'interfaccia piatta e reattiva in stile Stremio.

## Caratteristiche principali di Nigiro

- **UI piatta & zero overhead su TV (`armeabi-v7a`):**
  - Disattivati i layer animati multi-pass (`radialGradient` / `CompositingStrategy.Offscreen` / `BlendMode`) e le transizioni pesanti di drawer e backdrop.
  - Icone, banner e nome applicazione dedicati (**Nigiro**).
- **Home stile Stremio con Hero Banner leggero:**
  - **Hero Banner** in alto (`Top 10 Italia`) con singolo `AsyncImage`, crossfade leggero (~280ms), badge/voto/anno, pulsanti **Guarda ora** e **Preferiti**, pausa automatica su focus D-pad o lifecycle stop.
  - **Continua a guardare / Prossimi episodi** subito sotto l'Hero.
  - **Filtro rapido D-pad** `[ Tutti | Film | Serie TV ]` istantaneo in memoria.
  - **Riga Piattaforme** (`Netflix`, `Prime Video`, `Disney+`, `Apple TV+`, `Sky / HBO`, `Paramount+`) con accesso diretto ai relativi BoxSet o scelta rapida Film / Serie TV.
  - **Righe catalogo** (`Top 10 Italia` + piattaforme) con titoli, valutazioni e stato preferiti/visto sempre visibili sotto i poster.
- **Riproduzione pensata per Gelato / Real-Debrid / StreamVix:**
  - **Failover automatico**: se un link remoto è morto (errore HTTP/rete) passa da solo alla versione successiva (max 3 tentativi), mantenendo la posizione; il transcode sul server (Raspberry Pi senza encoder hardware) resta l'ultima risorsa.
  - **Scelta della versione**: i file locali vengono prima; poi si rispetta l'ordine di AIOStreams, abbassando le versioni che non partirebbero o andrebbero a scatti (codec non supportato, bitrate oltre ~40 Mbps per il Wi-Fi, CAM, non in cache RD).
  - **Selettore versione leggibile** (dettagli compatti, focus sulla versione in uso) anche **durante la riproduzione** (Impostazioni → Versione).
  - **Tracce in italiano**: lingua audio preferita anche per le sorgenti non analizzate dal server; niente sottotitoli forzati in inglese sopra l'audio italiano.
  - Sorgenti **HLS di StreamVix** riconosciute anche senza estensione `.m3u8`; buffer limitato in MB per la poca RAM della TV.
  - Pagina di errore con **Riprova / Prova un'altra versione / Indietro**; card "Prossimo episodio" 30 s prima della fine quando non c'è un segmento dei titoli di coda.
- **Moduli essenziali:**
  - Mantiene **Discover (Seerr)** e **Screensaver**.
  - Nasconde **Live TV**, **Musica / Theme Music** e **Foto**: il codice resta (disattivato a runtime) per facilitare i cherry-pick da upstream.
- **Configurazione centralizzata server-side (`/nigiro-config.json`, fallback legacy `/wholphin-config.json`):**
  - Tutte le impostazioni UI, moduli, lingua audio/sottotitoli (`ita`), screensaver e cataloghi Home vengono applicate automaticamente dal server all'avvio (con fallback su cache locale offline). Se l'endpoint non è esposto viene usata la configurazione di default integrata nell'app.
  - Menu **Impostazioni TV** nascosto di default sul client, lasciando visibile il selettore **Profilo / Server**.
- **Build rapida & dedicata:**
  - Compila nativamente per `armeabi-v7a` (`release`) tramite GitHub Actions.

## Requisiti e Installazione

- Android TV / Fire TV (Android 6+, ottimizzato per Android 11 `armeabi-v7a`).
- Server **Jellyfin** (`10.10.x` / `10.11.x`); opzionale l'endpoint `/nigiro-config.json` esposto dal reverse proxy (es. Caddy).
- Scarica l'APK dalla sezione [Releases](https://github.com/bonfiglioalessio/Nigiro/releases) o dai workflow artifacts di questo repository e installalo via ADB:
  ```bash
  adb connect <TV_IP>:5555
  adb install -r Nigiro-release-armeabi-v7a.apk
  ```

## Upstream & Licenza

Questo progetto è distribuito sotto licenza [GPLv2](LICENSE), basato sul codice open-source di [damontecres/Wholphin](https://github.com/damontecres/Wholphin).
Grazie al team Jellyfin e a tutti i collaboratori upstream.
