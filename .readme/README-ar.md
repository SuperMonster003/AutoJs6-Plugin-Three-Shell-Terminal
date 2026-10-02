<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-shell-terminal-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>طرفية متعددة الجلسات لـ AutoJs6 وبرامجه النصية, تشغل shell النظام في pty مع جلسات في الخلفية وشريط مفاتيح وأوامر Node.js</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات

******

يدعم README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالي

******

### مقدمة

******

يتولى 3-Shell Terminal الطرفية المدمجة في AutoJs6: مفتاح "الطرفية" في الدرج الرئيسي, و"فتح في الطرفية" في قائمة المجلدات بمدير الملفات وشريط أدوات المشروع, والكائن العام `terminal` في جانب البرنامج النصي لفتح الجلسات وقيادتها ومراقبتها. كل جلسة هي shell النظام (`/system/bin/sh`) يعمل في pty ويستمر في الخلفية بعد مغادرة الشاشة.

يكتشف AutoJs6 المكون الإضافي عبر خدمة Binder, ويفتح شاشة الطرفية بـ Intent صريح, ويستخدم Binder لقراءة عدد الجلسات أو إغلاقها جميعا أو قيادة جلسات البرامج النصية; ويعود مخرج الجلسة إلى البرامج النصية عبر أنبوب. عند تثبيت المكون الإضافي Node.js Runtime تقرأ الطرفية عقد البيان الخاص به مباشرة, وتتحقق من التوقيع والمشغل, ثم توفر node / npm / npx / corepack / yarn / pnpm.

******

### الحالة

******

معاينة تطوير محلية P4: تم تنفيذ واجهة الطرفية والجلسات المتعددة وواجهة البرمجة النصية, بما يشمل أحداث المخرجات والإدخال التفاعلي وانتظار رمز الخروج. تأتي الإعدادات المستقلة في P5 من [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md). تتطلب الواجهة إصدار AutoJs6 يتضمن تنفيذ P4.

******

### الميزات

******

توفر الإضافة القدرات التالية:

- جلسات متعددة: إنشاء وتبديل وإغلاق ومدير جلسات; تبقي خدمة المقدمة الجلسات تعمل بعد مغادرة الشاشة, ويعرض إشعارها المجلد الحالي وعدد الجلسات مع إجراء "إغلاق الجلسات".
- شاشة الطرفية: شريط مفاتيح من صفين (Esc / Tab / Ctrl / الأسهم / الرموز الشائعة), تحديد نص أصلي مع نسخ / تحديد الكل, نسخ النسخة المكتوبة ومشاركتها, حجم النص, اللصق والمسح.
- سلسلة أدوات Node.js: مع تثبيت المكون الإضافي Node.js Runtime (1.5.0+) تتوفر node / npm / npx / corepack / yarn / pnpm, مع إعدادات سجل npm و"تجاهل نصوص التثبيت" وقائمة الحزم (npm init / install / run script والمزيد).
- مداخل AutoJs6: مفتاح الدرج الرئيسي (عدد الجلسات, إغلاق الكل), و"فتح في الطرفية" في قائمة المجلدات بمدير الملفات وشريط أدوات المشروع.
- واجهة البرمجة النصية `terminal` (الاسم البديل `$terminal`): إدارة الجلسات, التنفيذ المرئي (`exec`, `npm.run`), وكائن جلسة بأحداث `output` / `exit` و`write` و`waitFor`; كل فشل هو `TerminalError` برمز `code` ثابت.
- تطبيق مستقل: تفتح أيقونة المشغل الطرفية مباشرة; صفحة إعدادات (مظهر يتبع AutoJs6, حجم النص, سجل npm, تكامل Node.js, الوصول إلى كل الملفات, مسح بيانات الطرفية), حول وسجل الإصدارات.

******

### الاستخدام

******

1. ثبت APK المكون الإضافي المطابق لـ ABI الجهاز (أو APK الشامل) من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) على جهاز به AutoJs6 بالبنية 5304 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `3-Shell Terminal`, ثم فعله.
3. شغل "الطرفية" من الدرج الرئيسي في AutoJs6, أو اختر "فتح في الطرفية" لمجلد في مدير الملفات, أو استدع `terminal.open(...)` من برنامج نصي. امنح "الوصول إلى كل الملفات" عندما يطلبه المكون الإضافي للدخول إلى مجلدات التخزين المشترك مثل `/sdcard`.

******

### أوامر Node.js

******

كيف تحصل الطرفية على node / npm وما هي الحدود:

- يتطلب المكون الإضافي Node.js Runtime 1.5.0 أو أحدث; يقرأ المكون الإضافي عقد البيان الخاص به, ويتحقق من التوقيع والمشغل وأرشيف npm / corepack, ويربط الأوامر في `PATH` عند بدء كل جلسة. بدون المكون الإضافي أو عند فشل التحقق تبقى الطرفية قابلة للاستخدام بدون هذه الأوامر.
- يمنع Android تنفيذ الملفات التي يكتبها التطبيق. يعطل npm روابط bin افتراضيا, لذا لا يستطيع `node_modules/.bin/*` أو `npx <package>` تشغيل نقاط دخول الحزم مباشرة. استخدم `node node_modules/<package>/<entry>.js`. تفشل الملفات التنفيذية الأصلية المضمنة في حزم npm مع `EACCES`, ولا يمكن تحميل الإضافات الأصلية (`.node`).
- يستخدم corepack افتراضيا pnpm 11.x و Yarn 1.x المضمنين (`COREPACK_DEFAULT_TO_LATEST=0`) وينزل الإصدار المسمى صراحة عند الطلب; يمكن تبديل سجل npm إلى npmmirror أو عنوان https مخصص في الإعدادات.

******

### بداية سريعة

******

برنامج نصي يفتح مجلد البرنامج النصي, ويثبت التبعيات بشكل مرئي وينتظر النتيجة, ويقود أمرا تفاعليا (متاح اعتبارا من المرحلة P4):

```js
// Open the script directory in the terminal; the screen comes to the front and the session keeps running in the background.
let session = terminal.open(files.cwd());
console.log(session.id, terminal.sessions().length);

// Visible execution: install dependencies in a session the user can watch and wait for the exit code (0 = no timeout).
let install = terminal.exec('npm install', { cwd: '/sdcard/Scripts/my-project', keepOpen: false, wait: true, timeout: 0 });
toastLog('npm install exited with ' + install.exitCode);

// Drive an interactive command: output / exit events, write and waitFor; every failure is a TerminalError with a stable code.
let driven = terminal.exec('sleep 1; printf "name? "; read name; echo "received:$name"; sleep 1', {
    cwd: files.cwd(), show: true, keepOpen: false,
});
driven.on('output', line => { if (/name\?/.test(line)) driven.write('AutoJs6\n'); });
driven.on('exit', code => console.log('Session exited with ' + code));
console.log(driven.waitFor(/received:AutoJs6/, 15e3));
```

******

### التوافق

******

حقائق المنصة التي تحدد ما تستطيع الإضافة فعله:

- Android 7.0 (API 24) وأحدث; ملفات APK لـ arm64-v8a و armeabi-v7a و x86_64 و x86 بالإضافة إلى APK شامل, مع مكتبات أصلية محاذاة لصفحات 16 كيلوبايت; يتم التحقق من بنية المضيف والمكون الإضافي معا على مصفوفة الأجهزة في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md).
- تعمل عمليات الطرفية تحت uid وأذونات المكون الإضافي نفسه ولا ترث أذونات AutoJs6; استخدم واجهة البرمجة النصية `shell()` للأوامر التي تحتاج أذونات AutoJs6.
- تعيش الجلسات ما دامت عملية المكون الإضافي حية; بعد أن ينهي النظام العملية لا يمكن استعادتها, وهو ما تجعله خدمة المقدمة وإشعارها غير مرجح.

******

### الأسئلة الشائعة

******

- **لماذا يفشل `cd /sdcard/Scripts`?** يحتاج المكون الإضافي إلى إذن تخزين خاص به. افتح إعدادات المكون الإضافي أو اتبع لافتة الطرفية لمنح "الوصول إلى كل الملفات" (Android 11+), أو إذن التخزين في الأنظمة الأقدم.
- **لماذا لا يوجد أمر node?** ثبت المكون الإضافي Node.js Runtime (1.5.0+) من مركز المكونات الإضافية في AutoJs6; يعرض "فحص البيئة" في إعدادات المكون الإضافي السبب الدقيق (غير مثبت, قديم جدا, توقيع غير موثوق, أو مشغل غير قابل للتنفيذ).
- **هل يستمر الأمر في العمل بعد مغادرة الطرفية?** نعم. تحافظ خدمة المقدمة على الجلسة ويعرض إشعارها عدد الجلسات; لا ينهي shell سوى "إغلاق الجلسات" في الإشعار أو مفتاح الدرج أو الجلسة نفسها.

******

### الصلاحيات والأمان

******

يلتزم المكون الإضافي بحدود صريحة:

- خدمة Binder ومدخل الشاشة محميان بإذن التوقيع `org.autojs.permission.PLUGIN` ويتحققان من توقيع المستدعي, لذا لا يصل إليهما سوى AutoJs6; مدخل المشغل يفتح الطرفية فقط ولا يقبل أوامر خارجية.
- يستخدم إذن التخزين ("الوصول إلى كل الملفات" على Android 11+) فقط للدخول إلى المجلدات التي تختارها; لا تفحص الطرفية الملفات ولا ترفعها أبدا.
- يستخدم إذن `INTERNET` بواسطة الأوامر التي تشغلها في shell (مثل `npm install`) وبواسطة فحص الإصدارات اليدوي عبر واجهة GitHub Releases الثابتة لهذا المكون الإضافي; لا يتصل المكون الإضافي نفسه بالشبكة في الخلفية أبدا.
- لا ينفذ مشغل المكون الإضافي Node.js Runtime إلا إذا كان توقيعه هو التوقيع الرسمي (أو مطابقا لهذا المكون الإضافي); لا يسجل المكون الإضافي مدخلات الجلسات أو مخرجاتها ويستثني تخزينه الخاص من النسخ الاحتياطي.

احصل على المكون الإضافي فقط من صفحة [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) الرسمية أو من مركز المكونات الإضافية في AutoJs6. قد تفشل الحزم من مصادر غير معروفة في التحقق من المضيف أو تحمل مخاطر حتى لو بدا رقم الإصدار متطابقا.

******

### واجهة المكون الإضافي

******

المعلومات التالية موجهة لمطوري مضيف AutoJs6 والمكونات الإضافية; يستخدم المضيف هذه المعرفات لاكتشاف المكون الإضافي والتفاوض على التوافق:

```text
application id: io.github.supermonster003.autojs6.plugin.three.shell.terminal
plugin id: three-shell-terminal
engine: terminal
variant: default
service action: org.autojs.plugin.TERMINAL
service category: terminal
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.terminal.api.ITerminalPlugin
minimum host build: 5304 (6.8.0)
```

تستجيب `ThreeShellTerminalPluginService` للإجراء `org.autojs.plugin.TERMINAL` (category `terminal`) وتنفذ عقد terminal-api الخاص بالمضيف `org.autojs.plugin.terminal.api.ITerminalPlugin` اعتبارا من المرحلة P2. تستجيب `ThreeShellTerminalPluginInfoService` للإجراء `org.autojs.plugin.INFO` بكائن PluginInfo. يتيح `WakeActivity` للمضيف تنشيط المكون الإضافي; وتفتح شاشة الطرفية عبر `org.autojs.plugin.TERMINAL_OPEN`.

******

### خارطة الطريق

******

تدار خطط المكون الإضافي وتقدمه كقائمة قابلة للتحقق في ROADMAP.md, منظمة حسب المرحلة مع معايير القبول ومستويات الأدلة. تعبر البنود غير المحددة عن النية لا عن القدرات الحالية; والنقاش عبر Issues موضع ترحيب.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/10/02_

- `تلميح` معاينة تطوير محلية P4: تم تنفيذ واجهة الطرفية والجلسات المتعددة وواجهة البرمجة النصية, بما يشمل أحداث المخرجات والإدخال التفاعلي وانتظار رمز الخروج. تأتي الإعدادات المستقلة في P5 من ROADMAP.md. تتطلب الواجهة إصدار AutoJs6 يتضمن تنفيذ P4.
- `ميزة` هوية المكون الإضافي `three-shell-terminal` (engine `terminal`) مع خدمة INFO و Wake Activity وهيكل خدمة `org.autojs.plugin.TERMINAL` لاكتشاف المضيف
- `ميزة` ملفات APK مقسمة حسب ABI (arm64-v8a, armeabi-v7a, x86_64, x86) بالإضافة إلى APK شامل, مع مكتبات أصلية محاذاة لصفحات 16 كيلوبايت
- `ميزة` README وتعليمات مركز المكونات الإضافية وسجل التغييرات بـ 10 لغات
- `ميزة` نقل نواة الجلسات من طرفية المضيف: جلسات shell على pty مع سجل على مستوى العملية (تسجيل العنوان ورمز الخروج لصالح Binder), بيئة الجلسة وتخطيط الدلائل داخل دليل ملفات المكون الإضافي, اكتشاف مشغل Node.js مع مثبت npm / corepack, وخدمة أمامية تبقي الجلسات قيد التشغيل مع إشعار "إغلاق الجلسات" (القناة `three.shell.terminal.sessions`)
- `ميزة` تحديد الوصول إلى التخزين (`StorageAccess`): حالة أذونات المكون الإضافي نفسه (أذونات وقت التشغيل القديمة دون API 30, "الوصول إلى جميع الملفات" بدءا من API 30), التعرف على التخزين المشترك مثل `/sdcard` و `/storage/...` ومجلدات `Android/{data,obb,media}` الخاصة, الرجوع بدليل البدء إلى `$HOME` مع `STORAGE_PERMISSION_REQUIRED` أو `DIRECTORY_INACCESSIBLE`, وإضافة Intent الإعدادات التي تفتح الوصول إلى جميع الملفات
- `ميزة` تكامل Node.js مع الثقة بالموقع (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): لا يستخدم المكون الإضافي Node.js Runtime إلا إذا كان موقعا بمفتاح المكونات الإضافية الرسمي لـ AutoJs6 أو بمفتاح هذا المكون الإضافي نفسه, ومفتاح الإعدادات يختصر المسار قبل أي بحث, وكل نتيجة تقابل حالات `node-cli` في العقد (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), وكل بدء جلسة يجدد روابط الأوامر في `usr/bin` ويستخرج أرشيف npm / corepack مرة واحدة لكل ملخص ويصدر بيئة npm / corepack
- `ميزة` يمكن لـ AutoJs6 إنشاء ما يصل إلى 16 جلسة طرفية والتحكم بها, وتلقي المخرجات المباشرة عبر 4 مستمعين لكل جلسة, وقراءة المخرجات الأخيرة والاستعلام عن بيئة shell. تستمر الجلسات بعد إغلاق AutoJs6, وترفض الطلبات غير الصالحة مع سبب محدد.
- `ميزة` تدعم إدارة الحزم npm init وتثبيت التبعيات والحزم وعرض نصوص package.json وتشغيلها وأوامر Yarn / pnpm والبحث في npm. يمكن اختيار npmjs أو npmmirror أو عنوان HTTPS مخصص مع تجاهل نصوص التثبيت. يغلق مسح البيانات جميع الجلسات قبل إعادة إنشاء home / usr مع الاحتفاظ بالإعدادات والمشاريع الخارجية. ستضاف القوائم وواجهة الإعدادات في مراحل لاحقة.
- `ميزة` شاشة الطرفية (`TerminalActivity`): واجهة طرفية المضيف منقولة إلى سمة Material 3 الخاصة بالإضافة, مع شريط المفاتيح (Esc / Tab / Ctrl / Alt / الأسهم / التصفح), تغيير حجم النص بالقرص, تحديد النص بالضغط المطول مع النسخ, قوائم الجلسات / النص / إدارة الحزم / الإعدادات / المساعدة, عنوان فرعي لشريط الأدوات يعرض دليل الصدفة وينسخه عند النقر, ولافتة Node.js تشرح غياب Node.js Runtime أو عدم الثقة به أو قدمه أو تعطيله مع إجراءات التثبيت / التحديث / التفعيل / التفاصيل; تظهر لافتة التخزين عند تعذر دخول دليل في التخزين المشترك وتوفر "منح" و "إعادة دخول الدليل"; تتبع الشاشة لغة المضيف ووضعه الليلي ولون سمته عبر موفر إعدادات المضيف وتعود إلى قيم النظام مع اللون المشترك `#FFDEAD`
- `ميزة` مدير الجلسات: مربع حوار بأقسام قابلة للطي للحالة / عناصر التحكم / الجلسات / الإعدادات يسرد كل جلسة قيد التشغيل مع دليلها ومعرف العملية ومدة التشغيل, ويفتح جلسة واحدة أو يغلقها, ويبدأ جلسة جديدة, ويغلق الكل, ويعرض تفاصيل الجلسة مع إجراء نسخ, ويوفر إعدادات حجم النص وسجل npm و ignore-scripts; يتبع نفس سجل الجلسات الذي يستخدمه `onSessionsChanged` في المضيف, ويمكن الوصول إليه من قائمة الطرفية ومن إشعار الجلسات (بالنقر), ولمدخل المضيف `manager=true` عبر `TerminalManagerActivity` شفافة لا تترك أي طرفية خلفها عند الإغلاق
- `ميزة` مدخل المضيف والمشغل: Activity المدخل المصدرة `TERMINAL_OPEN` المحمية بإذن التوقيع `org.autojs.permission.PLUGIN` تتحقق من المستدعي الذي يمكنها تحديده (يحمل الإذن وموقع مثل المكون الإضافي), وتتحقق من extras `directory` / `sessionId` / `newSession` / `command` / `manager` وفق حدود العقد ثم تمررها إلى شاشة الطرفية في مهمتها الخاصة أو إلى مدير الجلسات فوق المستدعي; `LauncherActivity` (هدف أسماء الأيقونة المستعارة) يستعيد أحدث جلسة أو يبدأ جلسة في الدليل الرئيسي; الرجوع من طرفية فتحها المضيف يعود إلى المضيف, ومن المشغل إلى الشاشة الرئيسية, وتغادر مهمة الطرفية قائمة التطبيقات الحديثة حتى لا يعاد تشغيل طلب البدء أبدا
- `إصلاح` لم تعد قيود النظام على النشاط في الخلفية تتسبب في تعطل الإضافة عند بدء جلسة. تستمر الجلسة دون حماية خدمة المقدمة.
- `إصلاح` تحتفظ قراءة المخرجات الطويلة بأحدث النصوص دون تجاوز الحد الأقصى لحجم الرد بين العمليات.
- `إصلاح` قراءة المخرجات وإعادة عرضها تتجاوز الأسطر الفارغة المستخدمة لملء الشاشة مع الحفاظ على مسافات الموجه وحدود الأسطر قبل المخرجات التالية
- `إصلاح` الجلسة أثناء بدء التشغيل كانت تختفي مؤقتا من استعلامات المضيف مما يمنع فتح الطرفية للتنفيذ المرئي
- `تبعية` إضافة jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) لمحاكاة الطرفية ومكتبات pty الأصلية, مع قفل التجزئة في `locks/vendored-aars.lock`
- `تبعية` إضافة `common-plugin-api.aar` و `nodejs-api.aar` (وحدتا AutoJs6 `plugin-api/common-plugin-api` و `plugin-api/nodejs-api`, بنية المضيف 6.8.0 / 5303, MPL 2.0) كعقد المكون الإضافي المشترك وعقد بيان Node.js, مع قفل التجزئة في `locks/host-api-aars.lock`
- `تبعية` إضافة `terminal-api.aar` (وحدة AutoJs6 `plugin-api/terminal-api`, بنية المضيف 6.8.0 / 5304, MPL 2.0) كعقد الطرفية V1 (`ITerminalPlugin` / `ITerminalCallback`, الهوية, الحدود, رموز الأخطاء); ثوابت هوية المكون الإضافي تأتي منه الآن, مع قفل التجزئة في `locks/host-api-aars.lock`

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء والتحقق

******

يستهدف هذا القسم المطورين الراغبين في بناء المكون الإضافي من المصدر; ويمكن للمستخدمين العاديين ببساطة تثبيت ملف APK الجاهز من صفحة Releases.

بناء APK للتصحيح:

```powershell
.\gradlew.bat :app:assembleDebug
```

تشغيل اختبارات وحدة JVM وبناء APK اختبارات الأجهزة:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

بناء APK الإصدار:

```powershell
.\gradlew.bat :app:assembleRelease
```

جمع ناتج الإصدار وإلحاق الإصدار وملخص CRC32 باسم الملف:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

التحقق من تزامن مصادر التوثيق متعدد اللغات مع النواتج المولدة (يفرض ذلك CI أيضا):

```powershell
py .python\generate_markdown.py --check
```

يتطلب البناء JDK 21 أو أحدث و Android SDK 37; وتدار إصدارات Gradle والمكونات الإضافية مركزيا عبر `version.properties` و `io.github.supermonster003.autojs6-platform-versions`.

******

### التعريب وتوليد التوثيق

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

ملفات JSON اللغوية في `.readme/` و `.changelog/` هي المصدر الوحيد لملف README وتعليمات مركز المكونات الإضافية وسجل التغييرات. عدل دائما مصادر JSON هذه وأعد تشغيل `py .python/generate_markdown.py`; ولا تحرر يدويا نواتج README و `plugin_instruction.md` وسجل التغييرات المولدة أبدا. شغل `py .python/generate_markdown.py --check` للتحقق من جميع النواتج المولدة.

******

### الترخيص

******

كود المشروع مرخص بموجب [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE). المكونات الخارجية وتراخيصها مدرجة في [إشعارات الجهات الخارجية](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md).

******

### روابط

******

- مشروع AutoJs6: https://github.com/SuperMonster003/AutoJs6
- توثيق AutoJs6: https://docs.autojs6.com
- توثيق وحدة الطرفية: https://docs.autojs6.com/#/terminal
- المكون الإضافي Node.js Runtime: https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime
- jackpal Android-Terminal-Emulator (محاكاة الطرفية ومكتبات pty الأصلية, Apache-2.0): https://github.com/jackpal/Android-Terminal-Emulator
- إشعارات الجهات الخارجية: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md
