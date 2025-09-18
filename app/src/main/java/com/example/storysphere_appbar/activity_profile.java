package com.example.storysphere_appbar;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class activity_profile extends AppCompatActivity {

    private TextView tvUsername, tvEmail;
    private ImageView imgProfile;
    private DBHelper dbHelper;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        dbHelper   = new DBHelper(this);
        tvUsername = findViewById(R.id.tv_username);
        tvEmail    = findViewById(R.id.tv_email);
        imgProfile = findViewById(R.id.imageView); // ใช้ id ให้ตรงกับ XML

        // [ADDED] รับ email ได้หลายทาง: "email" → "extra_email" → session
        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = getIntent().getStringExtra("extra_email"); // เผื่อบางหน้าส่ง key นี้มา
        }
        if (userEmail == null || userEmail.isEmpty()) {
            String fromSession = getSharedPreferences("session", MODE_PRIVATE).getString("email", "");
            if (fromSession != null && !fromSession.isEmpty()) {
                userEmail = fromSession;
            }
        }

        // Toolbar back
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(activity_profile.this, HomeActivity.class);
            if (userEmail != null && !userEmail.isEmpty()) {
                intent.putExtra("email", userEmail); // [UNCHANGED] ส่งต่ออีเมล
            }
            startActivity(intent);
            finish();
        });

        // [CHANGED] ย้ายการโหลดข้อมูลออกเป็นเมทอดเดียว เพื่อเรียกซ้ำได้
        loadUserProfile();
    }

    // [ADDED] รีเฟรชข้อมูลทุกครั้งที่กลับมาหน้านี้ (เช่น หลังแก้ไขจาก Edit Profile)
    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    // [ADDED] รวม logic โหลดชื่อ + อีเมลจาก DB ตามอีเมลปัจจุบัน
    private void loadUserProfile() {
        if (userEmail == null || userEmail.isEmpty()) {
            // กรณีทดสอบ ยังไม่มี email
            tvUsername.setText("Guest User");
            tvEmail.setText("guest@example.com");
            Toast.makeText(this, "No email provided - test mode", Toast.LENGTH_SHORT).show();
            return; // ข้าม query DB
        }

        // ถ้ามี email → ใช้ข้อมูลจริงจาก DB
        tvEmail.setText(userEmail);
        try (Cursor cursor = dbHelper.getUserByEmail(userEmail)) {
            if (cursor != null && cursor.moveToFirst()) {
                // [FIX] ดึงชื่อจากคอลัมน์ "username" (มาจาก Sign Up: insertUser(username,...))
                int index = cursor.getColumnIndex("username");
                if (index != -1) {
                    String username = cursor.getString(index);
                    tvUsername.setText(username != null ? username : "No username");
                } else {
                    tvUsername.setText("No username");
                }
            } else {
                tvUsername.setText("Unknown");
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void edit_profile(View view) {
        Intent intent = new Intent(this, activity_edit_profile.class);
        intent.putExtra("email", userEmail); // [UNCHANGED] ส่งอีเมลไปหน้าแก้ไข
        startActivity(intent);
        // [ADDED] ไม่ต้องทำอะไรเพิ่ม — เมื่อกลับมาหน้านี้ onResume() จะรีโหลดชื่อใหม่ให้เอง
    }

    public void logOut(View view) {
        // [ADDED] ล้าง session ด้วย (กันหลงค้าง)
        getSharedPreferences("session", MODE_PRIVATE).edit().remove("email").apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void delete_account(View view) {
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "No account to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean deleted = dbHelper.deleteUser(userEmail);
        if (deleted) {
            // [ADDED] ล้าง session เมื่อบัญชีถูกลบแล้ว
            getSharedPreferences("session", MODE_PRIVATE).edit().remove("email").apply();

            Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, activity_sign_up.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
        }
    }
}
