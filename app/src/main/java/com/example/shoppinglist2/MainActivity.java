package com.example.shoppinglist2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;


//es soll eine App gebaut werden, die Eingaben in Chips aufteilt
//durch Semikolon getrennt

//answer from stackoverflow: Use LinearLayout with orientation verticle and add views
// in that layout.

//does not work on my newer phone (file not found)

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //the app failed when one path was ...DIRECTORY_DOCUMENTS
    private static final String FILE_PATH = Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS).toString() + "/shoppinglist.txt";
    private static final String TEMP_FILE_PATH = Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS).toString() + "/tempfile.txt";
    //paths for api level 29 and higher
    private String FPATH29;
    private String TFPATH29;

    private EditText et1;
    private ScrollView sv1;
    private LinearLayout ll1;
    private Chip[] chips;
    private String[] products;
    private String[] inputProducts;
    private String deleteItem;
    private InputMethodManager inputManager;
    private int apiLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiLevel = Build.VERSION.SDK_INT;

        if(apiLevel >= 29) {
            Toast.makeText(MainActivity.this,
                    "your android api level is 29 or higher," +
                            " your file will be stored in the internal storage of" +
                            " this app", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this,
                    "your android api level is lower than 29, " +
                            "your file will be stored in the download folder",
                    Toast.LENGTH_SHORT).show();
        }
        //building the path for the awful api 29
        FPATH29 = this.getFilesDir() + "/shoppinglist.txt";
        TFPATH29 = this.getFilesDir() + "/tempfile.txt";

        et1 = findViewById(R.id.editText);
        sv1 = findViewById(R.id.scrollView);
        ll1 = new LinearLayout(this);
        ll1.setOrientation(LinearLayout.VERTICAL);
        sv1.addView(ll1);

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        } else {
            readFile();
        }
        inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        readFile();
    }

    public void inputToChips(View v) {
        products = getUserInput();

        saveToFile(products);

        splitToChips(products);
    }

    public String[] getUserInput() {
        String content = et1.getText().toString();
        inputProducts = content.split(";");
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
        et1.getText().clear();
        return inputProducts;
    }

    public void splitToChips(String[] products) {
        chips = new Chip[products.length];

        for (int i = 0; i < products.length; i++) {
            //the chip component requires your app theme to be Theme.MaterialComponents (or a
            //descendant)
            chips[i] = new Chip(this);
            //ScrollView can only host one direct child
            ll1.addView(chips[i]);
            chips[i].setText(products[i]);
            chips[i].setCloseIconVisible(true);
            chips[i].setOnCloseIconClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        //this method removes Chips on hitting the close icon
        //and deletes the item from the text file
        //maybe problems with deleting (still has to be tested)

        Chip chip = (Chip) v;
        deleteItem = chip.getText().toString();

        File inputFile;
        File tempFile;
        if(apiLevel>=29) {
            inputFile = new File(FPATH29);
            tempFile = new File(TFPATH29);
        } else {
            inputFile = new File(FILE_PATH);
            tempFile = new File(TEMP_FILE_PATH);
        }

        try {
            FileReader fileReader = new FileReader(inputFile);
            BufferedReader reader = new BufferedReader(fileReader);
            FileOutputStream fos;
            if(apiLevel>=29) {
                fos = new FileOutputStream(TFPATH29, true);
            } else {
                fos = new FileOutputStream(TEMP_FILE_PATH, true);
            }

            String currentLine;

            while((currentLine = reader.readLine()) != null) {
                //String values have to be compared with .equals() not ==
                if (!currentLine.contains(deleteItem)) {
                    currentLine = currentLine + " \n";
                    fos.write(currentLine.getBytes());
                }
            }
            fos.flush();
            fos.close();
            reader.close();
            fileReader.close();
            tempFile.renameTo(inputFile);
            ll1.removeView(chip);
            Toast.makeText(MainActivity.this,
                    "line deleted", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,
                    "deleting failed", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (SecurityException se) {
            Toast.makeText(MainActivity.this,
                    "security exception thrown", Toast.LENGTH_LONG).show();
            se.printStackTrace();
        }
    }

    public void saveToFile(String[] products) {
        try {
            FileOutputStream fos;
            if(apiLevel>=29) {
                fos = new FileOutputStream(FPATH29, true);
            } else {
                fos = new FileOutputStream(FILE_PATH, true);
            }

            String addedString;
            for(int i = 0; i<products.length; i++) {
                addedString = products[i] + " \n";
                fos.write(addedString.getBytes());
            }

            fos.flush();
            fos.close();
            Toast.makeText(MainActivity.this, "writing to file succeeded.",
                    Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "file not found",
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "IOException occured",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void readFile() {
        File file;
        if(apiLevel>=29) {
            file = new File(FPATH29);
        } else {
            file = new File(FILE_PATH);
        }
        StringBuilder stringBuilder = new StringBuilder();
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            if(line == null) {
                return;
            }

            while (line != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
                line = bufferedReader.readLine();
            }
            products = stringBuilder.toString().split(" \n");
            ll1.removeAllViews();

            splitToChips(products);

            fileReader.close();
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteAllAlertDialog(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle("Deleting");
        builder.setMessage("Do you really want to delete all items?");
        builder.setPositiveButton("Confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAll();
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void deleteAll() {
        File inputFile;
        File tempFile;
        if(apiLevel>=29) {
            inputFile = new File(FPATH29);
            tempFile = new File(TFPATH29);
        } else {
            inputFile = new File(FILE_PATH);
            tempFile = new File(TEMP_FILE_PATH);
        }

        try {
            FileOutputStream fos;
            if(apiLevel>=29) {
                fos = new FileOutputStream(TFPATH29);
            } else {
                fos = new FileOutputStream(TEMP_FILE_PATH, true);
            }

            fos.close();

            tempFile.renameTo(inputFile);
            ll1.removeAllViews();
            Toast.makeText(MainActivity.this,
                    "all lines deleted", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,
                    "deleting failed", Toast.LENGTH_LONG).show();
            e.printStackTrace();


        } catch (SecurityException se) {
            Toast.makeText(MainActivity.this,
                    "security exception thrown", Toast.LENGTH_LONG).show();
            se.printStackTrace();
        }
    }
}
