package com.kodis;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.*;

public class MainActivity extends AppCompatActivity implements TextWatcher {
    private File file;

    private int CHUNK = 20000;
    private String FILE_CONTENT;
    private String currentBuffer;
    private StringBuilder loaded;

    private FloatingActionButton fab;
    private EditText contentView;
    private RelativeLayout hidden;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        contentView = (EditText) findViewById(R.id.fileContent);
        hidden = (RelativeLayout) findViewById(R.id.hidden);
        setInitialFAB();

        PermissionManager.verifyStoragePermissions(this);
    }

    private void openFilePicker() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties);
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                contentView.setVisibility(View.GONE);
                hidden.setVisibility(View.VISIBLE);
                new DocumentLoader().execute(files);
            }
        });
        dialog.show();
    }

    private void setInitialFAB() {
        fab = (FloatingActionButton) findViewById(R.id.fab);
        setOpenFAB();
    }

    private void setSaveFAB(){
        fab.setImageResource(R.drawable.vector_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isChanged()) {
                    new DocumentSaver().execute();
                } else
                    Toast.makeText(getApplicationContext(), "No change in file", Toast.LENGTH_SHORT).show();
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getApplicationContext(), "Save File", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void setOpenFAB(){
        fab.setImageResource(R.drawable.vector_open);
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

        ActionBar supportActionBar = getSupportActionBar();
        if(supportActionBar!=null)
            getSupportActionBar().setTitle(file.getName());

        final InteractiveScrollView scrollView = (InteractiveScrollView) findViewById(R.id.scrollView);
        scrollView.setOnBottomReachedListener(null);
        scrollView.setOnScrollListener(new InteractiveScrollView.OnScrollListener() {
            @Override
            public void onScrolled() {
                contentView.setFocusable(false);
            }

            @Override
            public void onScrolledUp() {
                fab.show();
            }

            @Override
            public void onScrolledDown() {
                fab.hide();
            }
        });
        scrollView.smoothScrollTo(0, 0);

        contentView.setFocusable(false);
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contentView.setFocusableInTouchMode(true);
            }
        });

        loaded = new StringBuilder();
        if (fileContent.length() > CHUNK)
            loadInChunks(scrollView, fileContent);
        else {
            loaded.append(fileContent);
            contentView.setText(loaded);
        }


        hidden.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        contentView.addTextChangedListener(this);
        currentBuffer = contentView.getText().toString();
    }

    private boolean isChanged(){
        if(FILE_CONTENT.length() >= CHUNK && FILE_CONTENT.substring(0, loaded.length()).equals(currentBuffer))
            return false;
        else if(FILE_CONTENT.equals(currentBuffer))
            return false;

        return true;
    }

    private void loadInChunks(InteractiveScrollView scrollView, final String bigString) {
        loaded.append(bigString.substring(0, CHUNK));
        contentView.setText(loaded);
        scrollView.setOnBottomReachedListener(new InteractiveScrollView.OnBottomReachedListener() {
            @Override
            public void onBottomReached() {
                if (loaded.length() >= bigString.length())
                    return;
                else if (loaded.length() + CHUNK > bigString.length()) {
                    String buffer = bigString.substring(loaded.length(), bigString.length());
                    loaded.append(buffer);
                } else {
                    String buffer = bigString.substring(loaded.length(), loaded.length() + CHUNK);
                    loaded.append(buffer);
                }

                Log.d("TEXT", "Updated");
                contentView.setText(loaded);
            }
        });
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

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        currentBuffer = contentView.getText().toString();
        if(isChanged())
            setSaveFAB();
        else
            setOpenFAB();
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    private class DocumentLoader extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... paths) {

            try {
                file = new File(paths[0]);
                BufferedReader br = new BufferedReader(new FileReader(paths[0]));
                try {
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();

                    while (line != null) {
                        sb.append(line);
                        sb.append("\n");
                        line = br.readLine();
                    }
                    FILE_CONTENT = sb.toString();
                    return FILE_CONTENT;
                } catch (IOException ioe) {
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
            loadDocument(s);
        }
    }

    private class DocumentSaver extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            BufferedWriter output = null;
            try {
                output = new BufferedWriter(new FileWriter(file));
                if(FILE_CONTENT.length() < CHUNK) {
                    output.write(currentBuffer);
                } else {
                    output.write(currentBuffer + FILE_CONTENT.substring(loaded.length(), FILE_CONTENT.length()));
                }
            } catch ( IOException e ) {
                e.printStackTrace();
            } finally {
                if ( output != null ) {
                    try {
                        output.close();
                    } catch (IOException ioe) {}
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
            setOpenFAB();
        }
    }
}
