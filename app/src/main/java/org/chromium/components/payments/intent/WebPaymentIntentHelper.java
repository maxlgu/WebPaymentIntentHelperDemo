// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.components.payments.intent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.JsonWriter;

import androidx.annotation.Nullable;

import org.chromium.components.payments.ErrorStrings;
import org.chromium.components.payments.intent.WebPaymentIntentHelperType.PaymentCurrencyAmount;
import org.chromium.components.payments.intent.WebPaymentIntentHelperType.PaymentDetailsModifier;
import org.chromium.components.payments.intent.WebPaymentIntentHelperType.PaymentItem;
import org.chromium.components.payments.intent.WebPaymentIntentHelperType.PaymentMethodData;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The helper that handles intent for AndroidPaymentApp.
 */
public class WebPaymentIntentHelper {
    /** The action name for the Pay Intent. */
    public static final String ACTION_PAY = "org.chromium.intent.action.PAY";

    // Freshest parameters sent to the payment app.
    public static final String EXTRA_CERTIFICATE = "certificate";
    public static final String EXTRA_MERCHANT_NAME = "merchantName";
    public static final String EXTRA_METHOD_DATA = "methodData";
    public static final String EXTRA_METHOD_NAMES = "methodNames";
    public static final String EXTRA_MODIFIERS = "modifiers";
    public static final String EXTRA_PAYMENT_REQUEST_ID = "paymentRequestId";
    public static final String EXTRA_PAYMENT_REQUEST_ORIGIN = "paymentRequestOrigin";
    public static final String EXTRA_TOP_CERTIFICATE_CHAIN = "topLevelCertificateChain";
    public static final String EXTRA_TOP_ORIGIN = "topLevelOrigin";
    public static final String EXTRA_TOTAL = "total";

    // Deprecated parameters sent to the payment app for backward compatibility.
    public static final String EXTRA_DEPRECATED_CERTIFICATE_CHAIN = "certificateChain";
    public static final String EXTRA_DEPRECATED_DATA = "data";
    public static final String EXTRA_DEPRECATED_DATA_MAP = "dataMap";
    public static final String EXTRA_DEPRECATED_DETAILS = "details";
    public static final String EXTRA_DEPRECATED_ID = "id";
    public static final String EXTRA_DEPRECATED_IFRAME_ORIGIN = "iframeOrigin";
    public static final String EXTRA_DEPRECATED_METHOD_NAME = "methodName";
    public static final String EXTRA_DEPRECATED_ORIGIN = "origin";

    // Response from the payment app.
    public static final String EXTRA_DEPRECATED_RESPONSE_INSTRUMENT_DETAILS = "instrumentDetails";
    public static final String EXTRA_RESPONSE_DETAILS = "details";
    public static final String EXTRA_RESPONSE_METHOD_NAME = "methodName";

    private static final String EMPTY_JSON_DATA = "{}";

    /** Invoked to report error for {@link #parsePaymentResponse}. */
    public interface PaymentErrorCallback {
        /** @param errorString The string that explains the error. */
        void onError(String errorString);
    }

    /** Invoked to receive parsed data for {@link #parsePaymentResponse}. */
    public interface PaymentSuccessCallback {
        /**
         * @param methodName The method name parsed from the intent response.
         * @param details The instrument details parsed from the intent response.
         */
        void onIsReadyToPayServiceResponse(String methodName, String details);
    }

    /**
     * Parse the Payment Intent response.
     * @param resultCode Result code of the requested intent.
     * @param data The intent response data.
     * @param errorCallback Callback to handle parsing errors. Invoked synchronously.
     * @param successCallback Callback to receive the parsed data. Invoked synchronously.
     **/
    public static void parsePaymentResponse(int resultCode, Intent data,
            PaymentErrorCallback errorCallback, PaymentSuccessCallback successCallback) {
        if (data == null) {
            errorCallback.onError(ErrorStrings.MISSING_INTENT_DATA);
        } else if (data.getExtras() == null) {
            errorCallback.onError(ErrorStrings.MISSING_INTENT_EXTRAS);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            errorCallback.onError(ErrorStrings.RESULT_CANCELED);
        } else if (resultCode != Activity.RESULT_OK) {
            errorCallback.onError(String.format(
                    Locale.US, ErrorStrings.UNRECOGNIZED_ACTIVITY_RESULT, resultCode));
        } else {
            String details = data.getExtras().getString(EXTRA_RESPONSE_DETAILS);
            if (details == null) {
                details = data.getExtras().getString(EXTRA_DEPRECATED_RESPONSE_INSTRUMENT_DETAILS);
            }
            if (details == null) details = EMPTY_JSON_DATA;
            String methodName = data.getExtras().getString(EXTRA_RESPONSE_METHOD_NAME);
            if (methodName == null) methodName = "";
            // TODO(crbug.com/1026667): Support payer data delegation for native apps instead of
            // returning empty PayerData.
            successCallback.onIsReadyToPayServiceResponse(
                    /*methodName=*/methodName, /*details=*/details);
        }
    }

    /**
     * Create an intent to invoke a native payment app. This method throws IllegalArgumentException
     * for invalid arguments.
     *
     * @param packageName The name of the package of the payment app. Only non-empty string is
     *         allowed.
     * @param activityName The name of the payment activity in the payment app. Only non-empty
     *         string is allowed.
     * @param id The unique identifier of the PaymentRequest. Only non-empty string is allowed.
     * @param merchantName The name of the merchant. Cannot be null..
     * @param schemelessOrigin The schemeless origin of this merchant. Only non-empty string is
     *         allowed.
     * @param schemelessIframeOrigin The schemeless origin of the iframe that invoked
     *         PaymentRequest. Only non-empty string is allowed.
     * @param certificateChain The site certificate chain of the merchant. Can be null for
     *         localhost or local file, which are secure contexts without SSL. Each byte array
     * cannot be null.
     * @param methodDataMap The payment-method specific data for all applicable payment methods,
     *         e.g., whether the app should be invoked in test or production, a merchant identifier,
     *         or a public key. The map and its values cannot be null. The map should have at
     *         least one entry.
     * @param total The total amount. Cannot be null..
     * @param displayItems The shopping cart items. OK to be null.
     * @param modifiers The relevant payment details modifiers. OK to be null.
     * @return The intent to invoke the payment app.
     */
    public static Intent createPayIntent(String packageName, String activityName, String id,
            String merchantName, String schemelessOrigin, String schemelessIframeOrigin,
            @Nullable byte[][] certificateChain, Map<String, PaymentMethodData> methodDataMap,
            PaymentItem total, @Nullable List<PaymentItem> displayItems,
            @Nullable Map<String, PaymentDetailsModifier> modifiers) {
        Intent payIntent = new Intent();
        checkStringNotEmpty(activityName, "activityName");
        checkStringNotEmpty(packageName, "packageName");
        payIntent.setClassName(packageName, activityName);
        payIntent.setAction(ACTION_PAY);
        payIntent.putExtras(
                buildPayIntentExtras(id, merchantName, schemelessOrigin, schemelessIframeOrigin,
                        certificateChain, methodDataMap, total, displayItems, modifiers));
        return payIntent;
    }

    /**
     * Create an intent to invoke a service that can answer "is ready to pay" query, or null of
     * none.
     *
     * @param packageName The name of the package of the payment app. Only non-empty string is
     *         allowed.
     * @param serviceName The name of the service. Only non-empty string is allowed.
     * @param schemelessOrigin The schemeless origin of this merchant. Only non-empty string is
     *         allowed.
     * @param schemelessIframeOrigin The schemeless origin of the iframe that invoked
     *         PaymentRequest. Only non-empty string is allowed.
     * @param certificateChain The site certificate chain of the merchant. Can be null for localhost
     *         or local file, which are secure contexts without SSL. Each byte array
     *         cannot be null.
     * @param methodDataMap The payment-method specific data for all applicable payment methods,
     *         e.g., whether the app should be invoked in test or production, a merchant identifier,
     *         or a public key. The map should have at least one entry.
     * @return The intent to invoke the service.
     */
    public static Intent createIsReadyToPayIntent(String packageName, String serviceName,
            String schemelessOrigin, String schemelessIframeOrigin,
            @Nullable byte[][] certificateChain, Map<String, PaymentMethodData> methodDataMap) {
        Intent isReadyToPayIntent = new Intent();
        checkStringNotEmpty(serviceName, "serviceName");
        checkStringNotEmpty(packageName, "packageName");
        isReadyToPayIntent.setClassName(packageName, serviceName);
        // CHANGE_NOTE: should be a bug.
        isReadyToPayIntent.setPackage(packageName);
        isReadyToPayIntent.setAction("org.chromium.intent.action.IS_READY_TO_PAY");

        checkStringNotEmpty(schemelessOrigin, "schemelessOrigin");
        checkStringNotEmpty(schemelessIframeOrigin, "schemelessIframeOrigin");
        // certificateChain is ok to be null, left unchecked here.
        checkNotEmpty(methodDataMap, "methodDataMap");
        isReadyToPayIntent.putExtras(buildExtras(/*id=*/null,
                /*merchantName=*/null, schemelessOrigin, schemelessIframeOrigin, certificateChain,
                methodDataMap, /*total=*/null, /*displayItems=*/null, /*modifiers=*/null));
        return isReadyToPayIntent;
    }

    private static void checkNotEmpty(Map map, String name) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(name + " should not be null or empty.");
        }
    }

    private static void checkStringNotEmpty(String value, String name) {
        if (TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException(name + " should not be null or empty.");
        }
    }
    private static void checkNotNull(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " should not be null.");
    }

    private static Bundle buildPayIntentExtras(String id, String merchantName,
            String schemelessOrigin, String schemelessIframeOrigin,
            @Nullable byte[][] certificateChain, Map<String, PaymentMethodData> methodDataMap,
            PaymentItem total, @Nullable List<PaymentItem> displayItems,
            @Nullable Map<String, PaymentDetailsModifier> modifiers) {
        // The following checks follow the order of the parameters.
        checkStringNotEmpty(id, "id");
        checkNotNull(merchantName, "merchantName");

        checkStringNotEmpty(schemelessOrigin, "schemelessOrigin");
        checkStringNotEmpty(schemelessIframeOrigin, "schemelessIframeOrigin");

        // certificateChain is ok to be null, left unchecked here.

        checkNotEmpty(methodDataMap, "methodDataMap");
        checkNotNull(total, "total");

        // displayItems is ok to be null, left unchecked here.
        // modifiers is ok to be null, left unchecked here.

        return buildExtras(id, merchantName, schemelessOrigin, schemelessIframeOrigin,
                certificateChain, methodDataMap, total, displayItems, modifiers);
    }

    // id, merchantName, total are ok to be null only for {@link #createIsReadyToPayIntent}.
    private static Bundle buildExtras(@Nullable String id, @Nullable String merchantName,
            String schemelessOrigin, String schemelessIframeOrigin,
            @Nullable byte[][] certificateChain, Map<String, PaymentMethodData> methodDataMap,
            @Nullable PaymentItem total, @Nullable List<PaymentItem> displayItems,
            @Nullable Map<String, PaymentDetailsModifier> modifiers) {
        Bundle extras = new Bundle();

        if (id != null) extras.putString(EXTRA_PAYMENT_REQUEST_ID, id);

        if (merchantName != null) extras.putString(EXTRA_MERCHANT_NAME, merchantName);

        assert !TextUtils.isEmpty(schemelessOrigin);
        extras.putString(EXTRA_TOP_ORIGIN, schemelessOrigin);

        assert !TextUtils.isEmpty(schemelessIframeOrigin);
        extras.putString(EXTRA_PAYMENT_REQUEST_ORIGIN, schemelessIframeOrigin);

        Parcelable[] serializedCertificateChain = null;
        if (certificateChain != null && certificateChain.length > 0) {
            serializedCertificateChain = buildCertificateChain(certificateChain);
            extras.putParcelableArray(EXTRA_TOP_CERTIFICATE_CHAIN, serializedCertificateChain);
        }

        assert methodDataMap != null && !methodDataMap.isEmpty();
        extras.putStringArrayList(EXTRA_METHOD_NAMES, new ArrayList<>(methodDataMap.keySet()));

        Bundle methodDataBundle = new Bundle();
        for (Map.Entry<String, PaymentMethodData> methodData : methodDataMap.entrySet()) {
            checkNotNull(methodData.getValue(), "methodDataMap's entry value");
            methodDataBundle.putString(methodData.getKey(), methodData.getValue().stringifiedData);
        }
        extras.putParcelable(EXTRA_METHOD_DATA, methodDataBundle);

        if (modifiers != null) {
            extras.putString(EXTRA_MODIFIERS, serializeModifiers(modifiers.values()));
        }

        if (total != null) {
            String serializedTotalAmount = serializeTotalAmount(total.amount);
            extras.putString(EXTRA_TOTAL,
                    serializedTotalAmount == null ? EMPTY_JSON_DATA : serializedTotalAmount);
        }

        return addDeprecatedExtras(id, schemelessOrigin, schemelessIframeOrigin,
                serializedCertificateChain, methodDataMap, methodDataBundle, total, displayItems,
                extras);
    }

    private static Bundle addDeprecatedExtras(@Nullable String id, String schemelessOrigin,
            String schemelessIframeOrigin, @Nullable Parcelable[] serializedCertificateChain,
            Map<String, PaymentMethodData> methodDataMap, Bundle methodDataBundle,
            @Nullable PaymentItem total, @Nullable List<PaymentItem> displayItems, Bundle extras) {
        if (id != null) extras.putString(EXTRA_DEPRECATED_ID, id);

        extras.putString(EXTRA_DEPRECATED_ORIGIN, schemelessOrigin);

        extras.putString(EXTRA_DEPRECATED_IFRAME_ORIGIN, schemelessIframeOrigin);

        if (serializedCertificateChain != null) {
            extras.putParcelableArray(
                    EXTRA_DEPRECATED_CERTIFICATE_CHAIN, serializedCertificateChain);
        }

        String methodName = methodDataMap.entrySet().iterator().next().getKey();
        extras.putString(EXTRA_DEPRECATED_METHOD_NAME, methodName);

        PaymentMethodData firstMethodData = methodDataMap.get(methodName);
        extras.putString(EXTRA_DEPRECATED_DATA,
                firstMethodData == null ? EMPTY_JSON_DATA : firstMethodData.stringifiedData);

        extras.putParcelable(EXTRA_DEPRECATED_DATA_MAP, methodDataBundle);

        String details = deprecatedSerializeDetails(total, displayItems);
        extras.putString(EXTRA_DEPRECATED_DETAILS, details == null ? EMPTY_JSON_DATA : details);

        return extras;
    }

    private static Parcelable[] buildCertificateChain(byte[][] certificateChain) {
        Parcelable[] result = new Parcelable[certificateChain.length];
        for (int i = 0; i < certificateChain.length; i++) {
            Bundle bundle = new Bundle();
            checkNotNull(certificateChain[i], "certificateChain[" + i + "]");
            bundle.putByteArray(EXTRA_CERTIFICATE, certificateChain[i]);
            result[i] = bundle;
        }
        return result;
    }

    private static String deprecatedSerializeDetails(
            @Nullable PaymentItem total, @Nullable List<PaymentItem> displayItems) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter json = new JsonWriter(stringWriter);
        try {
            // details {{{
            json.beginObject();

            if (total != null) {
                // total {{{
                json.name("total");
                serializeTotal(total, json);
                // }}} total
            }

            // displayitems {{{
            if (displayItems != null) {
                json.name("displayItems").beginArray();
                // Do not pass any display items to the payment app.
                json.endArray();
            }
            // }}} displayItems

            json.endObject();
            // }}} details
        } catch (IOException e) {
            return null;
        }

        return stringWriter.toString();
    }

    private static String serializeTotalAmount(PaymentCurrencyAmount totalAmount) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter json = new JsonWriter(stringWriter);
        try {
            // {{{
            json.beginObject();
            json.name("currency").value(totalAmount.currency);
            json.name("value").value(totalAmount.value);
            json.endObject();
            // }}}
        } catch (IOException e) {
            return null;
        }
        return stringWriter.toString();
    }

    private static void serializeTotal(PaymentItem item, JsonWriter json) throws IOException {
        // item {{{
        json.beginObject();
        // Sanitize the total name, because the payment app does not need it to complete the
        // transaction. Matches the behavior of:
        // https://w3c.github.io/payment-handler/#total-attribute
        json.name("label").value("");

        // amount {{{
        json.name("amount").beginObject();
        json.name("currency").value(item.amount.currency);
        json.name("value").value(item.amount.value);
        json.endObject();
        // }}} amount

        json.endObject();
        // }}} item
    }

    private static String serializeModifiers(Collection<PaymentDetailsModifier> modifiers) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter json = new JsonWriter(stringWriter);
        try {
            json.beginArray();
            for (PaymentDetailsModifier modifier : modifiers) {
                checkNotNull(modifier, "PaymentDetailsModifier");
                serializeModifier(modifier, json);
            }
            json.endArray();
        } catch (IOException e) {
            return EMPTY_JSON_DATA;
        }
        return stringWriter.toString();
    }

    private static void serializeModifier(PaymentDetailsModifier modifier, JsonWriter json)
            throws IOException {
        // {{{
        json.beginObject();

        // total {{{
        if (modifier.total != null) {
            json.name("total");
            serializeTotal(modifier.total, json);
        } else {
            json.name("total").nullValue();
        }
        // }}} total

        // TODO(https://crbug.com/754779): The supportedMethods field was already changed from array
        // to string but we should keep backward-compatibility for now.
        // supportedMethods {{{
        json.name("supportedMethods").beginArray();
        json.value(modifier.methodData.supportedMethod);
        json.endArray();
        // }}} supportedMethods

        // data {{{
        json.name("data").value(modifier.methodData.stringifiedData);
        // }}}

        json.endObject();
        // }}}
    }
}
