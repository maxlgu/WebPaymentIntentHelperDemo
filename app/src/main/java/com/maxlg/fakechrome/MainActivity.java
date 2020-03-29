package com.maxlg.fakechrome;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.chromium.components.payments.intent.IsReadyToPayServiceHelper;
import org.chromium.components.payments.intent.WebPaymentIntentHelper;
import org.chromium.components.payments.intent.WebPaymentIntentHelperType.PaymentCurrencyAmount;
import org.chromium.components.payments.intent.WebPaymentIntentHelperType.PaymentDetailsModifier;
import org.chromium.components.payments.intent.WebPaymentIntentHelperType.PaymentItem;
import org.chromium.components.payments.intent.WebPaymentIntentHelperType.PaymentMethodData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    // TODO: correct the package name after moving maxpay into another package.
    private static final String MAX_PAY_PACKAGE = "com.maxlg.fakechrome";

    private static final int PAYMENT_INTENT_REQUEST_CODE = 123;
    private TextView mDescriptionView;
    private Button mPayButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fakechrome);

        mDescriptionView = findViewById(R.id.description);
        mPayButton = findViewById(R.id.button);

        mDescriptionView.setMovementMethod(new ScrollingMovementMethod());

        new IsReadyToPayServiceHelper(this,
                createIsReadyToPayIntent(), new IsReadyToPayServiceHelper.ResultHandler() {
            @Override
            public void onIsReadyToPayServiceResponse(boolean isReadyToPay) {
                mPayButton.setEnabled(isReadyToPay);
                mDescriptionView.setText("Max pay is " + (isReadyToPay?"ready":"unready") + " to pay.");
                mDescriptionView.setTextColor(isReadyToPay?Color.BLACK:Color.RED);
            }

            @Override
            public void onIsReadyToPayServiceError() {
                mPayButton.setEnabled(false);
                mDescriptionView.setText("MaxPay's IsReadyToPay service has an error.");
                mDescriptionView.setTextColor(Color.RED);
            }
        });

        mPayButton.setOnClickListener((view)->{
            Intent intent = createIntent();
            startActivityForResult(intent, PAYMENT_INTENT_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYMENT_INTENT_REQUEST_CODE) {
            WebPaymentIntentHelper.parsePaymentResponse(resultCode, data, (errorString)->{
                mDescriptionView.setText(errorString);
                mDescriptionView.setTextColor(Color.RED);
            }, (methodName, details)->{
                String description = "methodName: " + methodName + ", details: "+details;
                mDescriptionView.setText(description);
                mDescriptionView.setTextColor(Color.BLACK);
            });
        }
    }

    private Intent createIsReadyToPayIntent() {
        Map<String, PaymentMethodData> methodDataMap = new HashMap<>();
        PaymentMethodData maxPayMethodData = new PaymentMethodData("maxPayMethod", "{}");
        methodDataMap.put("maxPay", maxPayMethodData);

        byte[][] certificateChain = new byte[][]{{0}};

        return WebPaymentIntentHelper.createIsReadyToPayIntent(MAX_PAY_PACKAGE, "com.maxlg.maxpay.MaxPayIsReadyToPayService",
                "maxlgu.github.io",
                "maxlgu.github.io", certificateChain, methodDataMap);
    }

    private Intent createIntent() {
        Map<String, PaymentMethodData> methodDataMap = new HashMap<>();
        PaymentMethodData maxPayMethodData = new PaymentMethodData("maxPayMethod", "{}");
        methodDataMap.put("maxPay", maxPayMethodData);

        PaymentItem total = new PaymentItem(new PaymentCurrencyAmount("CAD", "50"));

        List<PaymentItem> displayItems = new ArrayList<>();
        displayItems.add(new PaymentItem(new PaymentCurrencyAmount("CAD", "50")));

        Map<String, PaymentDetailsModifier> modifiers = new HashMap<>();
        PaymentDetailsModifier maxPayModifier = new PaymentDetailsModifier(total, maxPayMethodData);
        modifiers.put("maxPay", maxPayModifier);

        byte[][] certificateChain = new byte[][]{{0}};

        return WebPaymentIntentHelper.createPayIntent(MAX_PAY_PACKAGE, "com.maxlg.maxpay.MaxPayActivity",
                "pay_request_id_1411", "Linda's Bakery", "maxlgu.github.io",
                "maxlgu.github.io", certificateChain, methodDataMap, total,
                displayItems, modifiers);
    }
}
