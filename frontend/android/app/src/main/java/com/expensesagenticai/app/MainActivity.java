package com.expensesagenticai.app;

import android.os.Bundle;
import android.webkit.CookieManager;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            if (this.bridge != null && this.bridge.getWebView() != null) {
                cookieManager.setAcceptThirdPartyCookies(this.bridge.getWebView(), true);
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
