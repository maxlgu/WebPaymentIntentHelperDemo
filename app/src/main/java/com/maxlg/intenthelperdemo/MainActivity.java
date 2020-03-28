package com.maxlg.intenthelperdemo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

    private static final int PAYMENT_INTENT_REQUEST_CODE = 123;
    private TextView mDescriptionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDescriptionView = findViewById(R.id.description);
        mDescriptionView.setMovementMethod(new ScrollingMovementMethod());

        Button payButton = findViewById(R.id.button);
        payButton.setOnClickListener((view)->{
            Intent intent = createIntent();
            startActivityForResult(intent, PAYMENT_INTENT_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYMENT_INTENT_REQUEST_CODE) {
            WebPaymentIntentHelper.parsePaymentResponse(resultCode, data, (errorString)->{
                android.util.Log.d("INTENT_HELPER", "errorStrinng: " + errorString);
                mDescriptionView.setText(errorString);
                mDescriptionView.setVisibility(View.VISIBLE);
                mDescriptionView.setTextColor(Color.RED);
            }, (methodName, details)->{
                String description = "methodName: " + methodName + ", details: "+details;
                mDescriptionView.setText(description);
                mDescriptionView.setVisibility(View.VISIBLE);
                mDescriptionView.setTextColor(Color.BLACK);
            });
        }
    }

    private Intent createIntent() {
        Map<String, PaymentMethodData> methodDataMap = new HashMap<String, PaymentMethodData>();
        PaymentMethodData maxPayMethodData = new PaymentMethodData("maxPayMethod", "{}");
        methodDataMap.put("maxPay", maxPayMethodData);

        PaymentItem total = new PaymentItem(new PaymentCurrencyAmount("CAD", "50"));

        List<PaymentItem> displayItems = new ArrayList<PaymentItem>();
        displayItems.add(new PaymentItem(new PaymentCurrencyAmount("CAD", "50")));

        Map<String, PaymentDetailsModifier> modifiers = new HashMap<String,
                PaymentDetailsModifier>();
        PaymentDetailsModifier maxPaymodifier = new PaymentDetailsModifier(total, maxPayMethodData);
        modifiers.put("maxPay", maxPaymodifier);

        byte[][] certificateChain = new byte[][]{{0}};

        return WebPaymentIntentHelper.createPayIntent("com.maxlg.intenthelperdemo", "com.maxlg.intenthelperdemo.PaymentAppActivity",
                "payment.request.id", "merchant.name", "maxlgu.github.io",
                "maxlgu.github.io", certificateChain, methodDataMap, total,
                displayItems, modifiers);
    }
}
