package com.example.storysphere_appbar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText edtEmail, edtPassword;
    Button bttLogin;
    TextView ClickSignUp, Forgotpass, txtShowPassword;

    boolean isPasswordVisible = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        bttLogin = findViewById(R.id.bttLogin);
        ClickSignUp = findViewById(R.id.ClickSignUp);
        Forgotpass = findViewById(R.id.Forgotpass);
        txtShowPassword = findViewById(R.id.txtShowPass);

        bttLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกอีเมลและรหัสผ่าน", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ ตรวจจากฐานข้อมูลแทน SharedPreferences
            DBHelper dbHelper = new DBHelper(this);
            Cursor c = dbHelper.getUserByEmail(email);
            if (c != null) {
                boolean ok = false;
                try {
                    if (c.moveToFirst()) {
                        int idx = c.getColumnIndex("password");
                        String storedPassword = (idx >= 0) ? c.getString(idx) :
                                c.getString(c.getColumnIndexOrThrow("password"));
                        ok = password.equals(storedPassword);
                    }
                } catch (Exception e) {
                    ok = false;
                } finally {
                    c.close(); // ปิด Cursor ทุกครั้ง
                }

                if (ok) {
                    Toast.makeText(this, "เข้าสู่ระบบสำเร็จ", Toast.LENGTH_SHORT).show();

                    // session เก็บอีเมลที่ล็อกอิน
                    getSharedPreferences("session", MODE_PRIVATE)
                            .edit()
                            .putString("email", email)
                            .apply();

                    // ✅ ส่งอีเมลไปหน้า follow ด้วย key คงที่ ป้องกันคอมไพล์เออร์เรอร์
                    Intent it = new Intent(this, HomeActivity.class);
                    it.putExtra("extra_email", email);
                    startActivity(it);
                    finish();
                } else {
                    Toast.makeText(this, "อีเมลหรือรหัสผ่านไม่ถูกต้อง", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "เกิดข้อผิดพลาดในการเชื่อมต่อฐานข้อมูล", Toast.LENGTH_SHORT).show();
            }
        });

        ClickSignUp.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, activity_sign_up.class)));

        Forgotpass.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, activity_forgot_pass.class)));

        txtShowPassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                txtShowPassword.setText("Show");
            } else {
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                txtShowPassword.setText("Hide");
            }
            isPasswordVisible = !isPasswordVisible;
            edtPassword.setSelection(edtPassword.length());
        });
    }
}
