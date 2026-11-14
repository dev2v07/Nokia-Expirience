# Nokia Experience 2.0 — Workspace Engine device test

Дата: 2026-11-11
Проект: `C:\projects\Nokia\NokiaExperience2`
Пакет: `com.nokiaexperience.launcher`

## 1. Устройство

ADB обнаружил авторизованное устройство:

```text
NOKIA_B device product:a5y17ltexx model:SM_A520F device:a5y17lte
```

Свойства:

- Model: `SM-A520F` — Samsung Galaxy A5 2017.
- Android: `8.0.0`.
- API: `26`.
- ADB state: `device`, авторизация подтверждена.

## 2. Состояние пакета до установки

Пакет уже был установлен. До обновления:

- versionCode: `1`;
- versionName: `0.1-mvp`;
- minSdk: `26`;
- targetSdk: `28`;
- package signature: Android Debug certificate;
- package был выбранной HOME Activity: `com.nokiaexperience.launcher/.LauncherActivity`.

Удаление, `pm clear`, очистка кэша, смена HOME и сброс настроек не выполнялись.

## 3. Проверка совместимости подписи

Перед обновлением APK установленной версии был считан с устройства и проверен через `apksigner`.

SHA-256 certificate digest установленной и нового APK совпадает:

```text
5406b3bc641a6de095c666af6b2c9930b32592e6ec51c1ed55f1028416ed5f3b
```

Сравнение итоговых APK SHA-256 также совпало:

```text
AA81E5F665143A6BFD7881E452E20ECDBB8607F08273CC84C95F28995A9F0E02
```

Обновление через `adb install -r` было безопасно по подписи и версии. Процесс ADB долго не выводил результат и был остановлен после диагностической проверки, но последующая проверка устройства подтвердила, что обновление фактически завершилось:

- новый `lastUpdateTime`: `2026-11-11 22:38:02`;
- новый codePath создан;
- размер APK на устройстве совпал с локальным APK: `13,644,366` байт;
- `ceDataInode` пользователя 0 сохранился: `131107`.

## 4. Запуск PrototypeActivity

Запущен только debug action:

```text
com.nokiaexperience.launcher.action.OPEN_WORKSPACE_ENGINE_PROTOTYPE
```

Подтверждено через ActivityManager:

```text
com.nokiaexperience.launcher/.workspace.engine.prototype.PrototypeActivity
```

`LauncherActivity` напрямую не запускалась.

## 5. Проверка запуска и logcat

Подтверждено:

- процесс `com.nokiaexperience.launcher` жив;
- Activity присутствует в текущем task и имеет `ResumedActivity`;
- `PrototypeActivity` создала `ViewRoot` и получила окно;
- в logcat нет `FATAL EXCEPTION`;
- в logcat нет `AndroidRuntime` crash;
- стартовый action принят ActivityManager без ошибки.

## 6. Ограничение визуальной проверки

Полноценную проверку элементов интерфейса выполнить не удалось, поскольку телефон заблокирован:

```text
keyguard showing=true
mKeyguardOccluded=false
mCurrentFocus=StatusBar
```

Скриншот устройства оказался полностью чёрным/перекрытым keyguard. Поэтому следующие пункты пока **не считаются подтверждёнными визуально**:

- отображение тестовых страниц;
- индикатор страницы;
- кнопки управления;
- создание/отмена временной страницы;
- удаление пустой страницы;
- визуальные состояния Gesture Arbitration;
- визуальные состояния Drag Transaction;
- фактическое касание и горизонтальный свайп.

PIN, обход блокировки и любые операции с keyguard не выполнялись.

## 7. Проверка безопасности

Подтверждено:

- HOME Activity осталась `com.nokiaexperience.launcher/.LauncherActivity`;
- текущая preferred HOME Activity не менялась;
- `LauncherActivity` не запускалась для теста;
- `ceDataInode` пользователя 0 сохранился;
- `pm clear`, `adb uninstall`, factory reset и очистка кэша не выполнялись;
- APK обновлён с совпадающей подписью;
- PrototypeActivity использует отдельные тестовые данные в памяти по коду прототипа;
- пользовательские SharedPreferences, JSON, профили, иконки и виджеты не читались и не изменялись прототипом.

Системные package-added/package-replaced broadcasts в logcat являются обычным следствием установки APK и не означают смену HOME или очистку данных.

## 8. Итог

Установка и программный запуск прошли: пакет обновлён, debug Activity стартует, падения не обнаружены. Визуальный и интерактивный тест заблокирован состоянием keyguard, поэтому нельзя объявлять прототип полностью принятым на устройстве.

## 9. Что необходимо сделать дальше

После ручной разблокировки владельцем телефона:

1. Повторно запустить debug action.
2. Проверить страницы, indicator и кнопки.
3. Проверить preview/confirm/cancel/delete page.
4. Проверить быстрый и медленный swipe, границы и возврат в IDLE.
5. Проверить состояния Drag Transaction кнопками Begin/Preview/Commit/Cancel.
6. Повторно снять `logcat -d -t 3000` и убедиться в отсутствии crash.

APK не устанавливался как новый HOME и приложение HOME по умолчанию не менялось.
