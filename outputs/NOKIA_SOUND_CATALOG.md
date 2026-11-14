# Nokia N95 sound resources for Nokia Experience

Source: working copy of RM-320 dump at `work/n95dump/N95 8GB (S60v3 - FP1)/data/drives/z/rm-320/`. The original archive was not changed. File roles below are inferred from the path/name unless explicitly marked as tested. This is not a claim that every candidate was the N95 default for its category.

Inventory: **70** audio-like files: 27 AAC, 16 MIDI, 17 WAV, 9 Symbian RNG and 1 MP3. **67** non-silent candidates are packaged under `app/src/main/assets/sounds/n95/`; `No_Sound.wav`, `No_Sound_PoC.wav` and `nullsound.mp3` are intentionally catalogued but not packaged. The separately supplied Connecting People MP3 remains `assets/n95/startup_sound_user.mp3` and is the launcher startup sound in both profiles.

| Original path (relative to RM-320 Z drive) | Format | Likely role | APK location / use |
|---|---|---|---|
| `private/10281CFB/multimediamenuselect.wav` | RIFF PCM WAV, 44.1 kHz stereo 16-bit | Multimedia-menu selection | `assets/sounds/multimediamenuselect.wav`; launcher UI tap default via SoundPool. Also retained in `sounds/n95/other/`. |
| `private/10281CFB/multimediamenubrowse.wav` | RIFF WAV | Multimedia-menu browse | Packaged; optional future UI cue, not globally applied. |
| `private/10281CFB/multimediamenustartup.wav` | RIFF WAV | Multimedia-menu startup, **not** confirmed phone startup | Packaged as candidate; not used over the user-provided startup MP3. |
| `data/Sounds/Digital/Beep.aac`, `Beep twice.aac`, `Drill.aac` | AAC/ADTS | Beep / confirmation / attention candidates | `sounds/n95/digital/`; not assigned to system notification channels. |
| `data/Sounds/Digital/Message 1.aac`, `Message 2.mid`, `Message 3.mid`, `Message 4.mid`, `Message 5.aac`, `Message 6.aac` | AAC / MIDI | Message alert candidates | `sounds/n95/digital/`; Android SMS apps keep their own notification sound. |
| `data/Sounds/Digital/Email alert.aac`, `Chat alert.aac`, `Incoming.aac` | AAC/ADTS | Email, chat and incoming-event alert candidates | `sounds/n95/digital/`; no global override. |
| `data/Sounds/Digital/Alarm 1.aac`, `Alarm 2.mid`, `Clock alert 1.mid`, `Clock alert 2.mid` | AAC / MIDI | Alarm/clock candidates | `sounds/n95/digital/`; external Clock app retains its setting. |
| `system/Sounds/Simple/Alarm.rng`, `Clock.rng`, `Nokia tune.rng` | Symbian RNG | Simple alarm/clock/ringtone candidates | `sounds/n95/system/`; requires RNG decoder before Android playback. |
| `data/Sounds/Digital/Nokia Tune.aac` | AAC/ADTS | Nokia ringtone candidate | `sounds/n95/digital/`; packaged but not set as system ringtone. |
| `system/Sounds/Wakeup tone.aac` | AAC/ADTS | Wake-up tone candidate; assignment not confirmed | `sounds/n95/system/`; not used as startup. |
| `system/Sounds/Digital/CamMojaveCapture1–4.wav`, `CamMojaveFocusSucc.wav`, `CamMojaveStart.wav`, `CamMojaveStop.wav` | WAV | Camera capture/focus/start/stop cues | `sounds/n95/system/`; the external Android camera controls its own sounds. |
| `system/Sounds/Digital/CamcorderJavaCapture.wav`, `CamcorderJavaStart.wav` | WAV | Java camcorder cues | `sounds/n95/system/`; no external camera override. |
| `system/Sounds/Digital/NameDiallerStartTone.wav` | WAV | Voice/name dialler cue | `sounds/n95/system/`; launcher does not run a dialler. |
| `resource/sounds/visualradiostartsound.wav` | WAV | Visual Radio start cue | `sounds/n95/other/`; not used by launcher. |
| `system/apps/Barcodereader/OK.wav` | WAV | Barcode-reader confirmation | `sounds/n95/other/`; not used by launcher. |
| `private/101FD862/TONE_POC_*.rng` (6 files) | Symbian RNG | Push-to-talk speech/call/grant/queue/ended cues | `sounds/n95/other/`; require RNG decoder, no launcher analogue. |
| `resource/No_Sound.wav`, `resource/No_Sound_PoC.wav`, `system/data/nullsound.mp3` | WAV / MP3 | Silence sentinel files | Not packaged or played. |

Additional `data/Sounds/Digital/` ringtone/music candidates (all packaged, not auto-assigned): `Agency.mid`, `Bach.aac`, `Bamb.aac`, `Be movin.aac`, `Bloom.mid`, `Brook.mid`, `Crystal.aac`, `Debussy.aac`, `Deer.mid`, `Dreamy rainy day.mid`, `Elves.mid`, `Fantasia.aac`, `Fondant.aac`, `Glint.aac`, `Kung fu.mid`, `Luminosity.mid`, `Oceanic.mid`, `Old times.aac`, `Schubert.aac`, `Slowdown.mid`, `Stay that way.aac`, `Thrilling.aac`, `Tinker.aac`, `Topic.aac`, `Tropical.aac`.

Validation: `multimediamenuselect.wav` has a standard RIFF/WAVE PCM header and is loaded with Android `SoundPool`; the N95 AAC sample has an ADTS sync header, and MIDI has `MThd`. RNG is Symbian-specific binary data and is **not** presumed playable by Android. On-device sound playback and all candidates' durations still require listening tests on the Samsung A5.

Android limitation: a launcher can play its own UI/startup cues, but cannot silently replace Samsung/Android notification, SMS, alarm, camera or ringtone sounds for other applications. No root, system-file edits, implicit `WRITE_SETTINGS` grant or system sound changes are attempted. Those candidates are bundled for future explicit user-controlled export/selection if desired.
