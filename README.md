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
- **Moduli essenziali:**
  - Mantiene **Discover (Seerr)** e **Screensaver**.
  - Rimuove completamente **Live TV**, **Musica / Theme Music** e **Foto**.
- **Configurazione centralizzata server-side (`/wholphin-config.json`):**
  - Tutte le impostazioni UI, moduli, lingua audio/sottotitoli (`ita`), screensaver e cataloghi Home vengono applicate automaticamente dal server all'avvio (con fallback su cache locale offline).
  - Menu **Impostazioni TV** nascosto di default sul client, lasciando visibile il selettore **Profilo / Server**.
- **Build rapida & dedicata:**
  - Compila nativamente per `armeabi-v7a` (`release`) tramite GitHub Actions.

## Requisiti e Installazione

- Android TV / Fire TV (Android 6+, ottimizzato per Android 11 `armeabi-v7a`).
- Server **Jellyfin** (`10.10.x` / `10.11.x`) con endpoint `/wholphin-config.json` esposto dal reverse proxy (es. Caddy).
- Scarica l'APK dalla sezione [Releases](https://github.com/bonfiglioalessio/Nigiro/releases) o dai workflow artifacts di questo repository e installalo via ADB:
  ```bash
  adb connect <TV_IP>:5555
  adb install -r Nigiro-armeabi-v7a.apk
  ```

## Upstream & Licenza

Questo progetto è distribuito sotto licenza [GPLv2](LICENSE), basato sul codice open-source di [damontecres/Wholphin](https://github.com/damontecres/Wholphin).
Grazie al team Jellyfin e a tutti i collaboratori upstream.
