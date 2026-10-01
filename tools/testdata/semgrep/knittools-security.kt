import android.util.Log

fun logFixtures(TAG: String, error: Throwable, projectId: String, tokenFailure: Throwable) {
    // ruleid: knittools-log-user-project-state
    Log.v(TAG, "project=$projectId")
    // ok: knittools-log-user-project-state
    Log.v(TAG, "failed")
    // ruleid: knittools-log-user-project-state
    Log.v(TAG, "project=$projectId", error)
    // ok: knittools-log-user-project-state
    Log.v(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    Log.v("token", "ready", tokenFailure)
    // ruleid: knittools-log-user-project-state
    Log.d(TAG, "project=$projectId")
    // ok: knittools-log-user-project-state
    Log.d(TAG, "failed")
    // ruleid: knittools-log-user-project-state
    Log.d(TAG, "project=$projectId", error)
    // ok: knittools-log-user-project-state
    Log.d(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    Log.d("token", "ready", tokenFailure)
    // ruleid: knittools-log-user-project-state
    Log.i(TAG, "project=$projectId")
    // ok: knittools-log-user-project-state
    Log.i(TAG, "failed")
    // ruleid: knittools-log-user-project-state
    Log.i(TAG, "project=$projectId", error)
    // ok: knittools-log-user-project-state
    Log.i(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    Log.i("token", "ready", tokenFailure)
    // ruleid: knittools-log-user-project-state
    Log.w(TAG, "project=$projectId")
    // ok: knittools-log-user-project-state
    Log.w(TAG, "failed")
    // ruleid: knittools-log-user-project-state
    Log.w(TAG, "project=$projectId", error)
    // ok: knittools-log-user-project-state
    Log.w(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    Log.w("token", "ready", tokenFailure)
    // ruleid: knittools-log-user-project-state
    Log.e(TAG, "project=$projectId")
    // ok: knittools-log-user-project-state
    Log.e(TAG, "failed")
    // ruleid: knittools-log-user-project-state
    Log.e(TAG, "project=$projectId", error)
    // ok: knittools-log-user-project-state
    Log.e(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    Log.e("token", "ready", tokenFailure)
    // ruleid: knittools-log-user-project-state
    android.util.Log.v(TAG, "project=$projectId")
    // ok: knittools-log-user-project-state
    android.util.Log.v(TAG, "failed")
    // ruleid: knittools-log-user-project-state
    android.util.Log.v(TAG, "project=$projectId", error)
    // ok: knittools-log-user-project-state
    android.util.Log.v(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    android.util.Log.v("token", "ready", tokenFailure)
    // ruleid: knittools-log-user-project-state
    android.util.Log.d(TAG, "project=$projectId")
    // ok: knittools-log-user-project-state
    android.util.Log.d(TAG, "failed")
    // ruleid: knittools-log-user-project-state
    android.util.Log.d(TAG, "project=$projectId", error)
    // ok: knittools-log-user-project-state
    android.util.Log.d(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    android.util.Log.d("token", "ready", tokenFailure)
    // ruleid: knittools-log-user-project-state
    android.util.Log.i(TAG, "project=$projectId")
    // ok: knittools-log-user-project-state
    android.util.Log.i(TAG, "failed")
    // ruleid: knittools-log-user-project-state
    android.util.Log.i(TAG, "project=$projectId", error)
    // ok: knittools-log-user-project-state
    android.util.Log.i(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    android.util.Log.i("token", "ready", tokenFailure)
    // ruleid: knittools-log-user-project-state
    android.util.Log.w(TAG, "project=$projectId")
    // ok: knittools-log-user-project-state
    android.util.Log.w(TAG, "failed")
    // ruleid: knittools-log-user-project-state
    android.util.Log.w(TAG, "project=$projectId", error)
    // ok: knittools-log-user-project-state
    android.util.Log.w(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    android.util.Log.w("token", "ready", tokenFailure)
    // ruleid: knittools-log-user-project-state
    android.util.Log.e(TAG, "project=$projectId")
    // ok: knittools-log-user-project-state
    android.util.Log.e(TAG, "failed")
    // ruleid: knittools-log-user-project-state
    android.util.Log.e(TAG, "project=$projectId", error)
    // ok: knittools-log-user-project-state
    android.util.Log.e(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    android.util.Log.e("token", "ready", tokenFailure)
    // ruleid: knittools-log-user-project-state
    Log.e(TAG, "AI output", error)
    // ruleid: knittools-log-user-project-state
    Log.e(TAG, "ocr output", error)
    // ruleid: knittools-log-user-project-state
    Log.e(TAG, "ai", error)
    // ruleid: knittools-log-user-project-state
    Log.e(TAG, "OCR", error)
    // ruleid: knittools-log-user-project-state
    Log.e(TAG, "(AI): done", error)
    // ruleid: knittools-log-user-project-state
    Log.e(TAG, "[ocr] done", error)
    // ok: knittools-log-user-project-state
    android.util.Log.d(TAG, "failed", error)
    // ok: knittools-log-user-project-state
    android.util.Log.d(TAG, "email ready", error)
    // ok: knittools-log-user-project-state
    android.util.Log.d(TAG, "democracy", error)
    // ok: knittools-log-user-project-state
    android.util.Log.d(TAG, "autocratic", error)
    // ok: knittools-log-user-project-state
    android.util.Log.d(TAG, "chair", error)
    // ok: knittools-log-user-project-state
    android.util.Log.d(TAG, "trailing", error)
    // ok: knittools-log-user-project-state
    android.util.Log.d(TAG, "ocracia", error)
    // ok: knittools-log-user-project-state
    android.util.Log.d(TAG, "said", error)
}
