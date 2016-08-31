package com.kodis;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ScrollingView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.*;

public class MainActivity extends AppCompatActivity {
    private String title;
    private int cursor = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setInitialFAB();
    }

    private void openFilePicker() {
        DialogProperties properties=new DialogProperties();
        properties.selection_mode=DialogConfigs.SINGLE_MODE;
        properties.selection_type=DialogConfigs.FILE_SELECT;
        properties.root=new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions=new String[]{".txt", ".c", ".java", ".py", ".cpp", ".html"};

        FilePickerDialog dialog = new FilePickerDialog(MainActivity.this,properties);
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                RelativeLayout hidden = (RelativeLayout) findViewById(R.id.hidden);
                hidden.setVisibility(View.VISIBLE);
                new DocumentLoader().execute(files);
            }
        });
        dialog.show();
    }

    private void setInitialFAB(){
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFilePicker();
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getApplicationContext(), "Open File", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void loadDocument(final String fileContent) {
        final TextView textView = (TextView) findViewById(R.id.fileContent);
        final RelativeLayout hidden = (RelativeLayout) findViewById(R.id.hidden);

        final InteractiveScrollView scrollView = (InteractiveScrollView) findViewById(R.id.scrollView);
        scrollView.setVerticalScrollBarEnabled(true);

        if(fileContent.length()>10000) {
            textView.setText(fileContent.substring(0, 10000));
            scrollView.setOnBottomReachedListener(new InteractiveScrollView.OnBottomReachedListener() {
                @Override
                public void onBottomReached() {
                    if(cursor>=fileContent.length())
                        return;
                    else if(cursor+10000 > fileContent.length())
                        textView.append(fileContent.substring(cursor+1, fileContent.length()));
                    else
                        textView.append(fileContent.substring(cursor+1, cursor+10000));

                    cursor+=10000;
                }
            });


        } else {
            textView.setText(fileContent);
        }

        getSupportActionBar().setTitle(title);

        hidden.setVisibility(View.GONE);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class DocumentLoader extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... paths) {

            try {
                title = new File(paths[0]).getName();
                BufferedReader br = new BufferedReader(new FileReader(paths[0]));
                try {
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();

                    while (line != null) {
                        sb.append(line);
                        sb.append("\n");
                        line = br.readLine();
                    }
                    String everything = sb.toString();
                    return everything;
                } catch (IOException ioe){

                } finally {
                    try {
                        br.close();
                    } catch (IOException ioe) {}
                }
            } catch (FileNotFoundException fnfe) {}

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(getApplicationContext(), "got", Toast.LENGTH_SHORT).show();
            loadDocument(s);
        }
    }
}
