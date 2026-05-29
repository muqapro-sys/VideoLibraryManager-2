# بناء APK من هاتف Android باستخدام GitHub Actions

هذا المشروع يحتوي على ملف GitHub Actions جاهز:

```text
.github/workflows/android-apk.yml
```

وظيفته:
- تشغيل بيئة Ubuntu على GitHub.
- إعداد Java 17.
- إعداد Android SDK.
- تثبيت Android Platform 36.
- بناء Debug APK.
- رفع ملف APK في صفحة Actions.

---

## الطريقة من الهاتف

### 1. أنشئ مستودع GitHub جديد

من المتصفح:
```text
github.com
```

ثم:
```text
New repository
```

اختر اسمًا مثل:
```text
VideoLibraryManager
```

---

### 2. ارفع ملفات المشروع

من صفحة المستودع:

```text
Add file > Upload files
```

ارفع محتويات مجلد المشروع، وليس ملف zip نفسه.

المهم أن تظهر الملفات هكذا داخل GitHub:

```text
app/
.github/workflows/android-apk.yml
build.gradle.kts
settings.gradle.kts
README_AR.md
```

---

### 3. شغّل البناء

بعد رفع الملفات:

```text
Actions
Build Android APK
Run workflow
```

أو سيعمل تلقائيًا عند رفع الملفات إلى branch اسمه:

```text
main
```

أو:

```text
master
```

---

### 4. تحميل APK

بعد انتهاء البناء بنجاح:

```text
Actions
آخر تشغيل ناجح
Artifacts
VideoLibraryManager-debug-apk
```

نزّل الملف المضغوط، ستجد داخله APK.

---

## تثبيت APK على هاتفك

بعد تحميل APK:

```text
افتح APK
اسمح بالتثبيت من المتصفح أو مدير الملفات
ثبّت التطبيق
```

قد تحتاج تفعيل:

```text
Install unknown apps
```

---

## ملاحظات مهمة

- APK الناتج هو Debug APK مناسب للتجربة.
- ليس مخصصًا للنشر في Google Play.
- للنشر الرسمي نحتاج Release APK أو AAB مع signing key.
- لا تحتاج Android Studio على الهاتف.
- أي تعديل ترفعه إلى GitHub سيبني APK جديد تلقائيًا.

---

## إذا فشل البناء

افتح صفحة الخطأ في GitHub Actions وانسخ آخر 30 سطرًا من الخطأ، ثم أرسله لي لأصححه.
