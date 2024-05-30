package com.project.alfaf.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.project.alfaf.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ContactPickerActivity extends AppCompatActivity {

    private ListView contactListView;
    private Set<Long> selectedContactIds; // Store the IDs of selected contacts

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_picker);

        contactListView = findViewById(R.id.contact_list_view);
        Button doneButton = findViewById(R.id.btn_done);
        EditText searchEditText = findViewById(R.id.et_search);

        selectedContactIds = new HashSet<>();
        // Retrieve selectedContactIds from the intent
        @SuppressWarnings("unchecked")
        ArrayList<Long> passedSelectedContactIds = (ArrayList<Long>) getIntent().getSerializableExtra("selectedContactIds");
        if (passedSelectedContactIds != null) {
            selectedContactIds.addAll(passedSelectedContactIds);
        }

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                filterContacts(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        loadContacts();

        doneButton.setOnClickListener(v -> {
            ArrayList<String> selectedContacts = getSelectedContactsNames();
            ArrayList<Long> contactIds = new ArrayList<>(selectedContactIds);

            Intent resultIntent = new Intent();
            resultIntent.putStringArrayListExtra("selectedContacts", selectedContacts);
            resultIntent.putExtra("selectedContactIds", contactIds);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void loadContacts() {
        @SuppressWarnings("unchecked")
        ArrayList<Long> selectedContactIds = (ArrayList<Long>) getIntent().getSerializableExtra("selectedContactIds");

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone._ID,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        );

        Cursor uniqueCursor = removeDuplicates(cursor, selectedContactIds);

        String[] from = {ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        int[] to = {R.id.text_contact_name, R.id.text_contact_number, R.id.checkbox_contact};

        CustomCursorAdapter adapter = new CustomCursorAdapter(this,
                R.layout.list_item_contact, uniqueCursor, from, to, 0);

        contactListView.setAdapter(adapter);
    }

    private void filterContacts(String query) {
        @SuppressWarnings("unchecked")
        ArrayList<Long> selectedContactIds = (ArrayList<Long>) getIntent().getSerializableExtra("selectedContactIds");

        ContentResolver contentResolver = getContentResolver();
        Cursor filteredCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone._ID,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                ContactsContract.Contacts.DISPLAY_NAME + " LIKE ? OR " + ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?",
                new String[]{"%" + query + "%", "%" + query + "%"},
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        );

        Cursor uniqueCursor = removeDuplicates(filteredCursor, selectedContactIds);

        ((CustomCursorAdapter) contactListView.getAdapter()).updateCursor(uniqueCursor, getSelectedIds());
    }

    private Cursor removeDuplicates(Cursor cursor, ArrayList<Long> selectedContactIds) {
        MatrixCursor uniqueCursor = new MatrixCursor(new String[]{
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        });

        HashSet<String> phoneNumbersSet = new HashSet<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                if (selectedContactIds != null && selectedContactIds.contains(contactId)) {
                    continue;  // Skip already selected contacts
                }

                String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("\\s+", "");
                if (phoneNumbersSet.add(phoneNumber)) {
                    uniqueCursor.addRow(new Object[]{
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    });
                }
            } while (cursor.moveToNext());
        }
        return uniqueCursor;
    }

    private ArrayList<String> getSelectedContactsNames() {
        ArrayList<String> selectedContacts = new ArrayList<>();
        for (Long contactId : selectedContactIds) {
            String contactName = getContactNameById(contactId);
            if (contactName != null) {
                selectedContacts.add(contactName);
            }
        }
        return selectedContacts;
    }

    private String getContactNameById(long contactId) {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                ContactsContract.Contacts._ID + " = ?",
                new String[]{String.valueOf(contactId)},
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            String contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            cursor.close();
            return contactName;
        }
        return null;
    }

    private Set<Long> getSelectedIds() {
        return selectedContactIds;
    }

    private class CustomCursorAdapter extends SimpleCursorAdapter {

        CustomCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView nameTextView = view.findViewById(R.id.text_contact_name);
            TextView numberTextView = view.findViewById(R.id.text_contact_number);
            CheckBox checkBox = view.findViewById(R.id.checkbox_contact);

            String contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            String contactNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
            long contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

            nameTextView.setText(contactName);
            numberTextView.setText(contactNumber);

            // Detach the listener
            checkBox.setOnCheckedChangeListener(null);
            // Set checkbox state
            checkBox.setChecked(selectedContactIds.contains(contactId));
            // Reattach the listener
            checkBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if (isChecked) {
                    selectedContactIds.add(contactId);
                } else {
                    System.out.println("REMOVING " + contactId);
                    selectedContactIds.remove(contactId);
                }
            });
        }

        void updateCursor(Cursor cursor, Set<Long> selectedIds) {
            super.changeCursor(cursor);
        }
    }
}
