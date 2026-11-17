# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`, пост о выходе — в `post/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow теперь кладёт в GitHub Release ещё и `Glow-v<версия>.aab`.
  Первая версия с ним — **1.2.2**, но в Play загружайте **1.2.3** (в ней кнопка «Поддержать»). Секреты `SIGNING_*` в репозитории
  заданы; без них сборка подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках перестаёт работать. Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.glow`.
  2. **Select key** → ключ с SHA-256 `5a8d5543b5bfc1f111d61545c775f6c7561f22850634faf8ff98b410608e14f2`
     (сертификат CN=Glow, O=Stillcraft — им подписаны все релизы, проверено по `Glow.apk` из v1.2.1).
  3. **Upload APK** → скопировать сниппет и запустить workflow **Package registration proof**
     (Actions → Run workflow → вставить сниппет). Он соберёт релизный APK с файлом
     `assets/adi-registration.properties`, подпишет тем же ключом и проверит отпечаток; APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Тогда APK из GitHub и версия из Play
  обновляют друг друга. Если оставить «Google создаст ключ подписи», они друг на друга не встанут.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне (на двух языках). После push ссылка:
  `https://github.com/bigdevwhale/glow/blob/main/PRIVACY.md`
- **Автозагрузка из CI** (`.github/workflows/play.yml`) — после каждого успешного Release берёт AAB из
  релиза и отправляет в Play. «What's new»: en-US — из текста релиза (раздел версии в `CHANGELOG.md`),
  ru-RU — из скрытого блока `<!-- tg ... -->` того же раздела, обрезается до 500 символов. Если такой релиз
  на треке уже есть, обновляются только заметки. До первой ручной загрузки workflow не нужен, поэтому:
  1. В репозитории Settings → Environments → создать **`google-play`** с Required reviewer (вы) —
     иначе job запустится без подтверждения сразу после релиза.
  2. Play Console → Users and permissions → пригласить `play-publisher@intense-context-514611-h3.iam.gserviceaccount.com`
     с правом выпускать релизы для Glow.
  3. В Google Cloud у пула `github` (Workload Identity Federation) разрешить репозиторию
     `bigdevwhale/glow` действовать от имени этого сервисного аккаунта — как для остальных приложений.
  Пока это не сделано, отклоняйте запуск «Google Play» после релиза (или он упадёт на авторизации — без вреда).
  По умолчанию workflow выкладывает в **production**; пока идёт закрытый тест, запускайте его вручную
  с треком `alpha` (closed testing).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Glow: Praise for Every Win` |
| Default language | English (United States) – en-US |
| App or game | **App** |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать (только телефон, портрет) | — |
| Video | пусто | — |

Скриншоты 1080×1920 собирает `tools/play.py` из снимков демо-сборки (`raw/<язык>/*.png`, 1080×2340,
`./gradlew installDebug -Pdemo` на Mi 9, снято через adb): статус-бар телефона закрашивается чистым
(9:41), Play не берёт картинки длиннее двух ширин, поэтому экран уходит за нижний край под подписью.
Подписи — Lora, шрифт приложения, на розово-кремовом градиенте экрана похвалы. На скриншотах: сегодня,
похвала за огромную победу, банка, пять голосов, «Мне плохо», переводчик, добрые слова, ордена
(`raw/*/9-report.png` — отчёт за неделю, запасной). Баннер и пост — нарисованное солнце над стеклянными
шариками. Демо по-русски заполняется, только если язык **системы** русский (данные создаются при первом
запуске по `Locale.getDefault()`). Как пересобрать графику:

```bash
python tools/play.py    # графика для Play (нужен pip install pillow)
```

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | App → **Lifestyle** |
| Tags (до 5) | **Self-help**, **Lifestyle**, **Notebook**, **Health & fitness**, **Meditation** |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/glow/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** Glow — дневник побед и поддержка, а не медицинское приложение: в Lifestyle его ищут рядом
с дневниками благодарности и аффирмациями. Health & Fitness тоже возможна, но там ревьюер строже смотрит
на всё, что похоже на психическое здоровье, а Glow прямо говорит, что он не терапия. Категорию можно
сменить позже в Store settings.

**Теги** — из списка Play (те же, что уже брали для других приложений Stillcraft). Self-help — главное:
похвала, переводчик с самокритичного, вечер гордости. Lifestyle — категория. Notebook — победы,
добрые слова и дневник гордости записываются. Health & fitness — забота о себе и экран «Мне плохо».
Meditation — круг для дыхания; если покажется натяжкой, оставьте четыре тега.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, как у остальных приложений.

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*
(Аккаунтов нет; после короткого знакомства — имя, голос, время напоминаний — открыто всё.)

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC, категория **All Other App Types**)
- На все вопросы — **No**: насилия, страха, сексуального контента, ненормативной лексики, алкоголя,
  табака и наркотиков нет.
- **Users interact / share content** — **No**: общения между пользователями нет; похвала, грамота и
  отчёт уходят через системное меню «Поделиться» в чужие приложения.
- **Shares location** — **No**. **Digital purchases** — No. **Web browser / unrestricted internet** — No.
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **16–17, 18 and over**. Младше 13 не отмечать: иначе действует политика Families с
  отдельными требованиями. 13–15 тоже лучше не отмечать: экран «Мне плохо» говорит о трудных
  состояниях и телефоне доверия, и для подростков ревью строже.
- *Could your store listing unintentionally appeal to children?* → **No**: рисунки мягкие, но это дневник
  побед для взрослых.

**News apps** — No. **COVID-19** — My app is not a publicly available COVID-19 app.
**Government apps** — No. **Financial features** — *My app doesn't provide any financial features.*

**Health apps** — отметьте только **Stress management, relaxation, mental acuity** (круг для дыхания,
экран «Мне плохо»). **Mental and behavioral health** не отмечать: Glow ничего не диагностирует и не
лечит, а в описании, README и политике прямо сказано, что он не терапия. Если ревьюер всё же попросит —
отметьте и сошлитесь на раздел «Not a medical service» в `PRIVACY.md`.

**Data safety**
- У приложения **нет разрешения INTERNET** — оно физически не может ничего передать. Нет серверов,
  аналитики, отчётов о сбоях. Скриншот к доброму слову выбирается через системный выбор фото, без разрешений.
- *Does your app collect or share any of the required user data types?* → **No**.
- Итог в карточке: «No data collected», «No data shared with third parties».
- Резервную копию (JSON) и «Поделиться» пользователь отправляет сам, системными средствами — по
  определению Google это не сбор данных приложением.

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Разрешения** — особых деклараций не нужно: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` и `VIBRATE`
не относятся к чувствительным. Точных будильников (exact alarm) и фоновых служб нет.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Closed testing** (для нового аккаунта; иначе Internal testing) → Create new release.
2. Загрузить `Glow-v<версия>.aab` из GitHub Release (для первой загрузки — 1.2.3).
3. Release name — номер версии. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

   В `release_notes.txt` сейчас «First release on Google Play», новинки 1.2.1 и исправление переводчика из 1.2.2 — если в следующей версии
   появится что-то ещё, допишите (≤500 символов).
4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран переключается сразу;
   утреннее уведомление и виджет «Похвала дня» работают; банка катает шарики при наклоне;
   Настройки → Резервная копия → Экспорт сохраняет файл.

## Следующие версии

- Ничего отдельно писать не нужно: `play.yml` берёт «What's new» из раздела версии в `CHANGELOG.md`
  (en-US — видимый текст, ru-RU — блок `<!-- tg -->`). Релиз уходит в Play после подтверждения в
  окружении `google-play`.
- Скриншоты переснимать, только если поменялись показанные экраны: демо-сборка → те же экраны → `raw/`
  → `python tools/play.py`.

## Риск: кнопка «Поддержать»

В настройках есть карточка «Поддержать Glow»: она открывает страницу поддержки StillCraft
(`https://bigdevwhale.github.io/stillcraft/support/`) в браузере. В приложении ничего не продаётся и не
открывается, поэтому под правило о платежах за цифровые товары она обычно не попадает. Если ревью всё же
придерётся (Payments policy), уберите карточку из сборки для Play (`SupportCard()` в `SettingsScreen.kt`)
или замените её ссылкой на сайт без слова «поддержать».

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.glow`.
