package com.example.numberaddrdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    Button btn;

    TextView addrTextView;

    private static final  String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            Scanner scanner = new Scanner(getAssets().open("dialling-code.txt"));
            while (scanner.hasNext()){
                Log.i("MainActivity","next:" + scanner.next());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        btn = findViewById(R.id.findaddr);
        addrTextView = findViewById(R.id.addrtv);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String addr1 = AddressQueryUtil.getInstance().getAddress("18523388909");
                String addr2 = AddressQueryUtil.getInstance().getAddress("17754943338");
                String addr3 = AddressQueryUtil.getInstance().getAddress("15523584578");
                String addr4 = AddressQueryUtil.getInstance().getAddress("02367836023");
                Log.d(TAG,addr1 + addr2 +addr3 + addr4);
            }
        });

    }
}
