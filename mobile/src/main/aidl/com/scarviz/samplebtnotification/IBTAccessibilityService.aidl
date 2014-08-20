package com.scarviz.samplebtnotification;

import com.scarviz.samplebtnotification.IBTAccessibilityServiceCallback;

interface IBTAccessibilityService {
    /** Serviceにコールバックを登録 */
    void registerCallback(IBTAccessibilityServiceCallback callback);

    /** Serviceからコールバックを解除 */
    void unregisterCallback(IBTAccessibilityServiceCallback callback);
}
