# Video Library Manager - One UI 8.5 Inspired + GitHub Actions

تطبيق Android لتنظيم الفيديوهات وليس تشغيلها، بواجهة قريبة من أسلوب One UI 8.5.

## الوظائف

- قراءة فيديوهات الجهاز عبر MediaStore.
- دعم Android 16 / Target SDK 36.
- عرض Grid/List.
- عرض المجلدات.
- البحث.
- الفرز حسب التاريخ، الاسم، الحجم، والمدة.
- مفضلة مؤقتة.
- فتح الفيديو بمشغل خارجي.
- بناء APK من الويب عبر GitHub Actions.

## البناء من الهاتف

راجع الملف:

```text
BUILD_FROM_ANDROID_PHONE_AR.md
```

## ملف GitHub Actions

```text
.github/workflows/android-apk.yml
```

يبني APK تلقائيًا عند push إلى main/master أو يدويًا من زر Run workflow.

## ملاحظة

هذه نسخة MVP. المفضلة مؤقتة ولا تحفظ بعد إغلاق التطبيق. في النسخة التالية نضيف Room Database.
