# Installing tilfazi on a TV, for an AI agent

The person may know nothing about computers. They never type an address or a command: you find the TV, run the
scripts, and guide them through the few things only their hands can do, one at a time. Speak Arabic, in short,
friendly sentences, one step per message, and wait for their answer before the next step. Use the exact menu names
below; never mention ADB, ports or IP addresses unless they ask.

## Rules

- Change on the TV only what tilfazi needs: install it, make it the home, switch off the TV's own home screen
  (switch off, never uninstall), choose tilfazi's screensaver. Nothing else: no "debloat", no other apps off.
- Never switch off `com.google.android.tungsten.setupwraith` (the setup wizard): on many TVs it owns the Wi-Fi
  network saved during setup, and Android deletes that network when it is switched off.
- Never switch off a launcher the person installed themselves.
- If something fails twice in the same way, stop and tell them plainly what happened.

## The process

### 0. Before anything

Make sure `adb` works on this computer (`adb version`). If not, install Android platform-tools yourself
(macOS: `brew install android-platform-tools`; Windows: `winget install Google.PlatformTools`; Linux: the
distribution's `android-tools` / `adb` package). Get this repository (`git clone https://github.com/TalalRugi/TilfaziTV`).
On Windows, run the scripts in Git Bash.

Then ask, together:
> هل التلفزيون شغّال ومتصل بنفس شبكة الواي فاي اللي عليها هذا الجهاز؟

### 1. Turn on developer mode (once)

> على التلفزيون: افتح **الإعدادات** ← **النظام** ← **حول**، وانزل لين **«إصدار نظام Android TV»** (أو «الإصدار» أو «Build»)، واضغط عليه **سبع مرات** ورا بعض لين تطلع رسالة **«أصبحت مطوّراً»**. قلّي «تم».

If they cannot find it: on some sets it is **الإعدادات ← تفضيلات الجهاز ← حول**. If it says «أنت مطوّر بالفعل», that is fine.

### 2. Turn on debugging

> ارجع للإعدادات ← **النظام** (أو تفضيلات الجهاز) ← **خيارات المطوّرين**، وشغّل **«تصحيح الأخطاء عبر USB»**. وإذا لقيت **«تصحيح الأخطاء عبر الشبكة»** شغّله بعد. قلّي «تم».

### 3. Run the installer

`install/install.sh` (no address: it finds the TV itself). While it runs, tell them:
> بتطلع رسالة على التلفزيون «السماح بتصحيح الأخطاء؟». اختر **«السماح دائماً من هذا الكمبيوتر»** ثم **«موافق»**.

It ends either with «Home opens tilfazi» or with a line `ASK: <reason>`. For each reason:

- **`ASK: no-tv`**: no TV answered on this network. Ask, one at a time, and run the installer again after each fix:
  1. > هل التلفزيون شغّال ومو في وضع الاستعداد؟ اضغط أي زر في الريموت.
  2. > تأكد إن التلفزيون والجهاز على **نفس شبكة الواي فاي**: في التلفزيون الإعدادات ← الشبكة والإنترنت، وقلّي وش اسم الشبكة اللي متصل فيها.
     (Compare with this computer's Wi-Fi name; a guest network or a second router does not count.)
  3. > تأكد إن **«تصحيح الأخطاء عبر USB»** شغّال في خيارات المطوّرين (الخطوة الثانية).
  4. Still nothing: some sets (Chromecast, newer Google TV) have only **«تصحيح الأخطاء اللاسلكي»**. Ask:
     > في خيارات المطوّرين، هل فيه **«تصحيح الأخطاء اللاسلكي»**؟ إذا فيه شغّله وادخل عليه، واختر **«إقران الجهاز باستخدام رمز إقران»**، وقلّي الأرقام اللي تطلع: الرمز (٦ أرقام)، والعنوان والمنفذ اللي تحته.
     Then `adb pair <address>:<port> <code>`, `adb connect` to the address and port shown on the main «تصحيح الأخطاء اللاسلكي» screen, `adb tcpip 5555`, and run the installer again.
- **`ASK: which-tv`**: more than one TV on the network; the lines before it are «address model». Ask which one by
  its model in plain words (> عندك أكثر من تلفزيون: «MiTV-MOEU0» و«…». أي واحد نثبّت عليه؟), then run
  `install/install.sh <its address>`.
- **`ASK: allow-debugging`**: the TV is waiting for their yes.
  > طلعت رسالة على التلفزيون؟ اختر **«السماح دائماً»** ثم **«موافق»**، وقلّي «تم».
  If no message is on screen: > اطفي «تصحيح الأخطاء عبر USB» وشغّله مرة ثانية. Then run the installer again.
- **`ASK: no-connection`**: the TV was seen but refused. Same as `allow-debugging`, then step 2 again.
- **`ASK: adb-missing`**: install platform-tools yourself (step 0), then run again.
- **«This TV does not let its own home be switched off»**: tilfazi is installed and opens from the apps. Tell them:
  > ثبّتنا تلفازي، بس هالتلفزيون ما يسمح نغيّر الواجهة الرئيسية. تقدر تفتحه من قائمة التطبيقات.
- **«Home did not open tilfazi»**: wait 5 minutes (the TV puts its own home back by itself), then ask them to press
  Home; if the TV asks which home, > اختر **«تلفازي»** ثم **«دائماً»**.

### 4. Hand it over

> تم 🎉 اضغط زر **الرئيسية** في الريموت، وتلفازي بيسألك عن مدينتك وفريقك، وبعدها تدخل الواجهة.

Tell them they may turn «تصحيح الأخطاء عبر USB» off again if they wish; tilfazi keeps working.

## Undo

If they want the TV back as it was: `install/undo.sh` (it finds the TV like the installer). It switches back on only
what the installer switched off, and removes tilfazi.

## Not supported

tilfazi runs on Android TV and Google TV only: not on Samsung, LG webOS, Hisense VIDAA or Fire TV. If their TV is
one of those, say so kindly and stop.

## Adding a font

If the person asks for another font, use one whose licence allows passing it on (SIL Open Font License; every font
on fonts.google.com qualifies). Take it from its official source (for Google Fonts: github.com/google/fonts), never
from a font pack site. Then put its files in tilfazi's folder on the TV:

```bash
adb shell mkdir -p /sdcard/Android/data/app.tilfazi.tv/files/fonts
adb push Name-Regular.ttf Name-Bold.ttf /sdcard/Android/data/app.tilfazi.tv/files/fonts/
```

Files named `Name-Light`, `Name-Regular`, `Name-Medium`, `Name-Bold` are weights of one font; a single variable file
(`Name[wght].ttf`) works too. The font then shows under «إعدادات تلفازي» ← «الخط», by its file name. A font whose
licence forbids passing it on (Thmanyah's, for one) the person must download themselves; you may still push their
own copy to their own TV.
