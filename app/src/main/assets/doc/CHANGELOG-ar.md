******

### سجل الإصدارات

******

# v1.0.0

###### 2026/10/01

* `تلميح` معاينة تطوير المرحلة P0: هيكل المستودع, وهوية المكون الإضافي التي يتعرف عليها مركز المكونات الإضافية في AutoJs6, وتجربة pty / التخزين / مشغل Node.js. يتبع عقد Binder ونواة الجلسات وشاشة الطرفية وواجهة البرمجة النصية وصفحة الإعدادات مراحل ROADMAP.md.
* `ميزة` هوية المكون الإضافي `three-shell-terminal` (engine `terminal`) مع خدمة INFO و Wake Activity وهيكل خدمة `org.autojs.plugin.TERMINAL` لاكتشاف المضيف
* `ميزة` ملفات APK مقسمة حسب ABI (arm64-v8a, armeabi-v7a, x86_64, x86) بالإضافة إلى APK شامل, مع مكتبات أصلية محاذاة لصفحات 16 كيلوبايت
* `ميزة` README وتعليمات مركز المكونات الإضافية وسجل التغييرات بـ 10 لغات
* `ميزة` نقل نواة الجلسات من طرفية المضيف: جلسات shell على pty مع سجل على مستوى العملية (تسجيل العنوان ورمز الخروج لصالح Binder), بيئة الجلسة وتخطيط الدلائل داخل دليل ملفات المكون الإضافي, اكتشاف مشغل Node.js مع مثبت npm / corepack, وخدمة أمامية تبقي الجلسات قيد التشغيل مع إشعار "إغلاق الجلسات" (القناة `three.shell.terminal.sessions`)
* `تبعية` إضافة jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) لمحاكاة الطرفية ومكتبات pty الأصلية, مع قفل التجزئة في `locks/vendored-aars.lock`
* `تبعية` إضافة `common-plugin-api.aar` و `nodejs-api.aar` (وحدتا AutoJs6 `plugin-api/common-plugin-api` و `plugin-api/nodejs-api`, بنية المضيف 6.8.0 / 5303, MPL 2.0) كعقد المكون الإضافي المشترك وعقد بيان Node.js, مع قفل التجزئة في `locks/host-api-aars.lock`
* `تبعية` إضافة `terminal-api.aar` (وحدة AutoJs6 `plugin-api/terminal-api`, بنية المضيف 6.8.0 / 5304, MPL 2.0) كعقد الطرفية V1 (`ITerminalPlugin` / `ITerminalCallback`, الهوية, الحدود, رموز الأخطاء); ثوابت هوية المكون الإضافي تأتي منه الآن, مع قفل التجزئة في `locks/host-api-aars.lock`
