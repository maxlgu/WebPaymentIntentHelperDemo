package com.maxlg.intenthelperdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Set;

public class PaymentAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_app);
        TextView descriptionView = findViewById(R.id.description);
        descriptionView.setMovementMethod(new ScrollingMovementMethod());
        descriptionView.setText(parseIntentExtras());

        Button payButton = findViewById(R.id.pay_button);
        payButton.setOnClickListener((v)->{
            setResult(Activity.RESULT_OK, createResultIntent());
            finish();
        });
        Button declineButton = findViewById(R.id.decline_button);
        declineButton.setOnClickListener((v)->{
            setResult(Activity.RESULT_CANCELED, new Intent());
            finish();
        });
    }

    private Intent createResultIntent() {
        Intent resultIntent = new Intent();
        Bundle result = new Bundle();
        result.putString("methodName", "maxPay");
        Long timeStamp = System.currentTimeMillis();
        result.putString("details", "{\"status\":\"success\",\"amount\":50,\"currency\":\"USD\",\"timestamp\":"+timeStamp.toString()+"}");
        resultIntent.putExtras(result);
        return resultIntent;
    }

    private String parseIntentExtras() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        JSONObject json = new JSONObject();
        String description;
        Set<String> keys = bundle.keySet();
        try {
            for (String key : keys) {
                json.put(key, JSONObject.wrap(bundle.get(key)));
            }
            description = json.toString();
        } catch(JSONException e) {
            description = e.toString();
        }
        return description;
    }
}