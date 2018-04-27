package com.daose.ksanime.api.ka;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;

import com.daose.ksanime.R;
import com.daose.ksanime.model.Episode;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.CaptchaListener;

public class CaptchaActivity extends AppCompatActivity {

    private ViewGroup root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captcha);
        root = (ViewGroup) findViewById(R.id.root_layout);
        root.addView(Browser.getInstance(this).getWebView());

        Browser.getInstance(this).answerCaptcha(new CaptchaListener() {
            @Override
            public void onSubmit() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(CaptchaActivity.this)
                                .setTitle(getString(R.string.captcha_title))
                                .setMessage(getString(R.string.captcha_message))
                                .setPositiveButton(getString(R.string.captcha_button), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent();
                                        intent.putExtra(Episode.URL, getIntent().getStringExtra(Episode.URL));
                                        setResult(KA.CAPTCHA_CODE, intent);
                                        root.removeAllViews();
                                        finish();
                                    }
                                })
                                .show();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        root.removeAllViews();
    }
}
