package com.quaap.audiometer;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;

            System.out.println(getResources().getString(R.string.app_name));

            TextView txtversion = (TextView)findViewById(R.id.txtversion);
            txtversion.setText(version);


            TextView txtnamelink = (TextView)findViewById(R.id.txtnamelink);
            txtnamelink.setMovementMethod(LinkMovementMethod.getInstance());
            txtnamelink.setText(Html.fromHtml("This program was written by <a href='http://quaap.com/D/programming-index'>Tom Kliethermes</a>"));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
