package com.maxlg.maxpay;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import  org.chromium.IsReadyToPayService;
import  org.chromium.IsReadyToPayServiceCallback;

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