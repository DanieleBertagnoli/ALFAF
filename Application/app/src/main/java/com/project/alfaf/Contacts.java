package com.project.alfaf;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

public class Contacts extends AppCompatActivity {

    private static final int PICK_CONTACT_REQUEST = 1;

    private TextView priorityInfo;
    private TextView noContacts;
    private LinearLayout contactListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contacts);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        priorityInfo = findViewById(R.id.priority_info_txt);
        priorityInfo.setVisibility(View.INVISIBLE);

        noContacts = findViewById(R.id.no_contacts_txt);
        noContacts.setVisibility(View.VISIBLE);

        contactListContainer = findViewById(R.id.contact_list_container);

        // Add event listener to add contact button
        ImageButton addContactBtn = findViewById(R.id.add_contact_btn);
        addContactBtn.setOnClickListener(v -> {
            priorityInfo.setVisibility(View.VISIBLE); // Turn on the priority info text
            noContacts.setVisibility(View.INVISIBLE); // Turn off the no contacts text

            pickContact();
        });
    }

    private void pickContact() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                Uri contactUri = data.getData();
                if (contactUri != null) {
                    String contactName = getContactName(contactUri);
                    if (contactName != null) {
                        addContact(contactName);
                    }
                }
            }
        }
    }

    private String getContactName(Uri contactUri) {
        String contactName = null;
        Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            contactName = cursor.getString(nameIndex);
            cursor.close();
        }
        return contactName;
    }

    private void addContact(String contactName) {
        LayoutInflater inflater = LayoutInflater.from(this);
        ConstraintLayout contactLayout = (ConstraintLayout) inflater.inflate(R.layout.contact_layout, null);

        TextView contactNameTextView = contactLayout.findViewById(R.id.contact_name);
        contactNameTextView.setText(contactName);

        ImageButton deleteButton = contactLayout.findViewById(R.id.delete_contact);
        ImageButton prioritizeButton = contactLayout.findViewById(R.id.prioritize_contact);

        deleteButton.setOnClickListener(v -> {
            contactListContainer.removeView(contactLayout);
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

        // Add the contact layout to the LinearLayout inside the ScrollView
        contactListContainer.addView(contactLayout);
        updatePrioritizeButtonsVisibility();
    }

    private void updatePrioritizeButtonsVisibility() {
        int childCount = contactListContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View contactLayout = contactListContainer.getChildAt(i);
            ImageButton prioritizeButton = contactLayout.findViewById(R.id.prioritize_contact);
            if (i == 0) {
                prioritizeButton.setVisibility(View.INVISIBLE);
            } else {
                prioritizeButton.setVisibility(View.VISIBLE);
            }
        }
    }
}
