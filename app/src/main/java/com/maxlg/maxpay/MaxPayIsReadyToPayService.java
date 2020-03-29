package com.maxlg.maxpay;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import  org.chromium.IsReadyToPayService;
import  org.chromium.IsReadyToPayServiceCallback;

// This is copied from https://developers.google.com/web/fundamentals/payments/payment-apps-developer-guide/android-payment-apps?hl=fr.
public  class MaxPayIsReadyToPayService extends Service {
    private final IsReadyToPayService.Stub mBinder =
            new IsReadyToPayService.Stub() {
                @Override
                public void isReadyToPay(IsReadyToPayServiceCallback callback) throws RemoteException {
                    // Check permission here.
                    callback.handleIsReadyToPay(true);
                }
            };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}