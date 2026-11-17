# Nigiro

<p align="center">
  <img src="assets/nigiro.jpeg" alt="Nigiro" width="320" />
</p>

**Nigiro** è un client Jellyfin per Android TV, leggero e in italiano, nato come fork di [Wholphin](https://github.com/damontecres/Wholphin). L'interfaccia riprende l'app di [AIOStreams](https://github.com/Viren070/AIOStreams); la riproduzione è pensata per un server che pesca soprattutto da stream remoti (Gelato + AIOStreams + Real-Debrid, StreamVix) e solo in parte da file locali.

È un'app personale: è ottimizzata per una sola TV Android 11 a 32 bit (`armeabi-v7a`, 2 GB di RAM, solo Wi-Fi) e un solo server Jellyfin su Raspberry Pi 5, senza encoder video hardware. Molte scelte (bitrate, buffer, ordine delle sorgenti) partono da qui.

## Interfaccia

- **Aspetto**: palette nero, grigi, bianco e beige Nigiro (`#DCD3CA`), font Inter, icone BoxIcons come AIOStreams. Sidebar a rail che si apre su una velatura della pagina, con il logo in fondo.
- **Home**
  - Hero **Top 10 Italia** con sfondo, logo, voto, generi e pulsanti **Guarda ora** / **Altre info**.
  - **Continua a guardare / Prossimi episodi** in card orizzontali con avanzamento e tempo rimanente. Le serie seguite il cui episodio non è ancora uscito restano in riga con il chip **In uscita** e la data.
  - Righe catalogo per piattaforma. Fermandosi su un poster, la card si allarga con sfondo, logo, dettagli e trama.
  - I cataloghi restano in cache: tornando dalla riproduzione si aggiorna solo *Continua a guardare*.
- **Esplora**: piattaforme di streaming (Netflix, Prime Video, Disney+, Apple TV+, Sky/HBO, Paramount+…) e generi curati. Ogni griglia ha il filtro Tutti / Film / Serie TV, l'ordinamento (data di aggiunta, nome, uscita, voto, classifica) e il dado per un titolo a caso.
- **Cerca**: tastiera pensata per il telecomando (apostrofo, *Svuota*, tasto Cerca), microfono, risultati divisi in **Film** e **Serie TV**. Prima di scrivere mostra ricerche recenti, preferiti e **Popolari ora**.
- **Preferiti**: griglia come Esplora, con filtro, ordinamento e dado.
- **Pagina film e serie** in stile AIOStreams: poster, logo, meta, trama, cast e troupe, titoli simili. Nelle serie gli episodi sono sulla pagina stessa: la stagione cambia appena ci si ferma sul poster (stagioni in cache), *Riprendi SxEy* punta al prossimo episodio, gli episodi non ancora usciti sono segnalati e non riproducibili.
- **Menu a pressione lunga** su ogni card: riproduci, vai a, segna come visto, preferiti, scegli una versione.
- **Badge discreti** sui poster: visto, preferito, *Su disco* per i titoli scaricati.

## Riproduzione

- **Play parte subito**, senza chiedere la versione. Usa, in ordine: la versione scelta prima, quella con cui il titolo è stato riprodotto l'ultima volta, altrimenti la migliore secondo `SourceRanking`.
- **Ordine delle sorgenti**: file sul server prima di tutto; poi gli stream in italiano, sempre davanti a quelli in altre lingue; la risoluzione più alta che sta nel budget del Wi-Fi (~10 Mbps); a parità, i link in cache su Real-Debrid e l'audio migliore per la soundbar (DD+ Atmos / DD+ / DD 5.1 in passthrough via HDMI ARC; TrueHD e DTS-HD MA in fondo). Le sorgenti CAM e i codec non supportati scendono in fondo.
- **Failover**: se un link è morto prova da solo la versione italiana successiva, mantenendo la posizione. Non passa mai a un'altra lingua: se non resta niente si ferma su una pagina d'errore con **Riprova / Prova un'altra versione / Indietro**. Il transcode sul Pi è l'ultima risorsa.
- **HLS di StreamVix** letti direttamente dal player (~1,2 s al primo frame invece di ~3,7 s con il remux sul server), anche senza estensione `.m3u8`.
- **Tracce italiane** di default, anche per le sorgenti che il server non ha analizzato; nessun sottotitolo inglese forzato sopra l'audio italiano.
- **Buffer** di 30–60 s con un tetto in MB per la poca RAM della TV.
- **Selettore versioni** in stile AIOStreams (dettagli compatti, *Riprendi da* / *Dall'inizio*, ultima scelta evidenziata), raggiungibile dal menu a pressione lunga e durante la riproduzione.
- Card **Prossimo episodio** 30 s prima della fine quando non c'è un segmento dei titoli di coda.

## Moduli

- Restano **Scopri (Seerr)** per le richieste e lo **Screensaver**.
- **Live TV**, **Musica** e **Foto** sono nascosti a runtime: il codice resta, per facilitare i cherry-pick da Wholphin.
- Le impostazioni TV sono nascoste; resta il selettore **Profilo / Server**.

## Configurazione dal server

All'avvio l'app legge `/nigiro-config.json` (fallback su `/wholphin-config.json`) dal server Jellyfin: moduli, lingue di audio e sottotitoli, screensaver, hero e cataloghi della Home con le collection delle piattaforme. L'ultima configurazione viene tenuta in cache per l'uso offline. Se l'endpoint non è esposto si usa quella integrata nell'app (`RemoteWholphinConfig.DEFAULT`).

## Prestazioni su TV a 32 bit

- Niente layer animati multi-pass (`radialGradient`, `CompositingStrategy.Offscreen`, `BlendMode`) né transizioni pesanti su drawer e sfondi.
- Lo sfondo è dipinto una volta sola sotto ogni pagina; lo sfondo dell'hero e le sue sfumature sono composti in un unico layer.
- Home caricata progressivamente, righe che precaricano una schermata di card, poster che si allargano solo dopo 650 ms di sosta per non rallentare lo scorrimento.
- Coil ed ExoPlayer configurati per la RAM della TV; immagini a piena qualità (sfondi a 1920 px, poster alla dimensione della card).
- Baseline profile incluso nella build.

## Installazione

Requisiti: Android TV / Google TV / Fire TV con Android 6+ (pensato per Android 11 `armeabi-v7a`) e un server Jellyfin 10.10 o 10.11.

L'APK firmato è nelle [Releases](https://github.com/bonfiglioalessio/Nigiro/releases) o negli artifact della CI (`nigiro-apks`). Per installarlo:

```bash
adb connect <TV_IP>:5555
adb install -r Nigiro-release-armeabi-v7a.apk
```

## Build

```bash
./gradlew :app:assembleDefaultRelease            # APK release armeabi-v7a
./gradlew :app:assembleDefaultDebug              # build debug (id .debug, si installa accanto alla release)
./gradlew :app:testDefaultDebugUnitTest          # test unitari
```

Per altre architetture: `-PabiFilters=arm64-v8a,armeabi-v7a`. La `versionCode` è il numero di commit e la `versionName` viene da `git describe`.

### Emulatore

Le immagini Android TV per i Mac Apple Silicon sono solo `arm64-v8a`, senza supporto a 32 bit: l'APK predefinito (`armeabi-v7a`) non si installa (`INSTALL_FAILED_NO_MATCHING_ABIS`), quindi va compilato per arm64.

1. In Android Studio → Device Manager crea un AVD **Television (1080p)** con un'immagine `android-tv` arm64 (API 31 o 34) e, per avvicinarsi alla TV vera, 2 GB di RAM.
2. Avvia l'emulatore, compila e installa:

```bash
~/Library/Android/sdk/emulator/emulator -avd <NOME_AVD> &

ANDROID_SERIAL=emulator-5554 ./gradlew :app:installDefaultDebug -PabiFilters=arm64-v8a

adb -s emulator-5554 shell monkey -p com.github.bonfiglioalessio.nigiro.debug \
  -c android.intent.category.LEANBACK_LAUNCHER 1
```

- `ANDROID_SERIAL` evita che Gradle installi anche sulla TV, se è collegata via adb.
- Nella finestra dell'emulatore le frecce fanno da D-pad, Invio è OK (tenuto premuto: pressione lunga), Esc è Indietro.
- L'emulatore raggiunge la rete di casa attraverso il Mac; se il nome del server non si risolve, usa l'IP.

GitHub Actions compila e testa ogni push su `main` (`Nigiro CI`), le PR (`Nigiro PR Check`) e pubblica le release (`Nigiro Release`).

Architettura, librerie, estensioni native (ffmpeg, libmpv) e preferenze sono descritte in [DEVELOPMENT.md](DEVELOPMENT.md).

## Crediti e licenza

Distribuito sotto licenza [GPLv2](LICENSE), basato su [damontecres/Wholphin](https://github.com/damontecres/Wholphin). L'interfaccia si ispira ad [AIOStreams](https://github.com/Viren070/AIOStreams).

- Icone [BoxIcons](https://boxicons.com) 2.1.4 (MIT)
- Font [Inter](https://rsms.me/inter/) (SIL OFL)

Grazie al team Jellyfin e a tutti i collaboratori di Wholphin.
