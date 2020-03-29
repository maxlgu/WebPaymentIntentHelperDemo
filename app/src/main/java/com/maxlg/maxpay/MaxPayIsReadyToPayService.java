package com.maxlg.maxpay;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import org.chromium.IsReadyToPayService;
import org.chromium.IsReadyToPayServiceCallback;

public class MaxPayIsReadyToPayService extends Service implements IsReadyToPayService {
    private final static String CHROME_PACKAGE = "com.maxlg.fakechrome";
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private String mPackageName;

    public MaxPayIsReadyToPayService() {
    }

    public class LocalBinder extends Binder {
        MaxPayIsReadyToPayService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MaxPayIsReadyToPayService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mPackageName = intent.getPackage();
        return mBinder;
    }

    @Override
    public void isReadyToPay(IsReadyToPayServiceCallback callback) throws RemoteException {
        boolean isReady = mPackageName == CHROME_PACKAGE;
        callback.handleIsReadyToPay(isReady);
    }

    @Override
    public IBinder asBinder() {
        return mBinder;
    }
}
