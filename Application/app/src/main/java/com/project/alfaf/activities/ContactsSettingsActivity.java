package com.project.alfaf.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.project.alfaf.R;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ContactsSettingsActivity extends AppCompatActivity {

    private static final int PICK_CONTACT_REQUEST = 1;
    private static final int REQUEST_READ_CONTACTS = 2;

    private LinearLayout priorityInfo;
    private TextView noContacts;
    private LinearLayout contactListContainer;
    private ArrayList<Long> selectedContactIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contacts_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        priorityInfo = findViewById(R.id.priority_info_layout);
        priorityInfo.setVisibility(View.INVISIBLE);

        noContacts = findViewById(R.id.no_contacts_txt);
        noContacts.setVisibility(View.VISIBLE);

        contactListContainer = findViewById(R.id.contact_list_container);

        ImageButton addContactBtn = findViewById(R.id.add_contact_btn);
        addContactBtn.setOnClickListener(v -> checkPermissionsAndPickContacts());

        ImageView backBtn = findViewById(R.id.back_btn_contacts);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        loadContactsFromFile();
    }

    private void checkPermissionsAndPickContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
        } else {
            pickContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickContacts();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
            }
        }
    }

    private void pickContacts() {
        Intent pickContactIntent = new Intent(this, ContactPickerActivity.class);
        pickContactIntent.putExtra("selectedContactIds", selectedContactIds);
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                ArrayList<String> selectedContacts = data.getStringArrayListExtra("selectedContacts");
                @SuppressWarnings("unchecked")
                ArrayList<Long> contactIds = (ArrayList<Long>) data.getSerializableExtra("selectedContactIds");
                if (selectedContacts != null && contactIds != null) {
                    for (int i = 0; i < selectedContacts.size(); i++) {
                        Long contactId = contactIds.get(i);
                        if (!selectedContactIds.contains(contactId)) {
                            addContact(selectedContacts.get(i), contactId);
                            selectedContactIds.add(contactId);
                        }
                    }
                    saveContactsToFile(selectedContacts, contactIds);
                }
            }
        }
    }

    private void saveContactsToFile(ArrayList<String> contacts, ArrayList<Long> contactIds) {
        try {
            FileOutputStream fos;
            fos = openFileOutput("contacts.txt", Context.MODE_PRIVATE);
            // Write contacts to the file
            for (int i = 0; i < contacts.size(); i++) {
                String contact = contacts.get(i);
                Long contactId = contactIds.get(i);
                String data = contact + "," + contactId + "\n";
                fos.write(data.getBytes());
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadContactsFromFile() {
        try (FileInputStream fis = openFileInput("contacts.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String contactName = parts[0];
                    Long contactId = Long.parseLong(parts[1]);
                    addContact(contactName, contactId);
                    selectedContactIds.add(contactId);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addContact(String contactName, Long contactId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        ConstraintLayout contactLayout = (ConstraintLayout) inflater.inflate(R.layout.contact_layout, null);
        contactLayout.setTag(contactId);

        TextView contactNameTextView = contactLayout.findViewById(R.id.contact_name);
        contactNameTextView.setText(contactName);

        ImageButton deleteButton = contactLayout.findViewById(R.id.delete_contact);
        ImageButton prioritizeButton = contactLayout.findViewById(R.id.prioritize_contact);

        deleteButton.setOnClickListener(v -> {
            contactListContainer.removeView(contactLayout);
            selectedContactIds.remove(contactId);  // Remove contact ID from the list
            updatePrioritizeButtonsVisibility();
            if (contactListContainer.getChildCount() == 0) {
                priorityInfo.setVisibility(View.INVISIBLE);
                noContacts.setVisibility(View.VISIBLE);
            }
        });

        prioritizeButton.setOnClickListener(v -> {
            contactListContainer.removeView(contactLayout);
            contactListContainer.addView(contactLayout, 0);
            updatePrioritizeButtonsVisibility();
        });

        contactListContainer.addView(contactLayout);
        updatePrioritizeButtonsVisibility();
    }

    private void updatePrioritizeButtonsVisibility() {
        int childCount = contactListContainer.getChildCount();
        ArrayList<String> contacts = new ArrayList<>();
        ArrayList<Long> contactIds = new ArrayList<>();

        for (int i = 0; i < childCount; i++) {
            View contactLayout = contactListContainer.getChildAt(i);
            TextView contactNameTextView = contactLayout.findViewById(R.id.contact_name);
            ImageButton prioritizeButton = contactLayout.findViewById(R.id.prioritize_contact);

            String contactName = contactNameTextView.getText().toString();
            Long contactId = (Long) contactLayout.getTag();

            contacts.add(contactName);
            contactIds.add(contactId);

            if (i == 0) {
                prioritizeButton.setVisibility(View.INVISIBLE);
            } else {
                prioritizeButton.setVisibility(View.VISIBLE);
            }
        }

        saveContactsToFile(contacts, contactIds);
    }
}
