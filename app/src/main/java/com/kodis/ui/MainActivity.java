package com.kodis.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.kodis.R;
import com.kodis.ui.fragment.EditorFragment;
import com.kodis.ui.fragment.MainFragment;
import com.kodis.utils.ExtensionManager;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {
    private TextView projectStructure, headerProject;
    private DrawerLayout drawerLayout;
    private MainFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null)
            mainFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.mainFragment);
        else
            mainFragment = (MainFragment) getSupportFragmentManager().getFragment(savedInstanceState, "mainFragment");

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.navView);
        headerProject = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header_project_name);
        projectStructure = (TextView) findViewById(R.id.project_structure);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.vector_menu);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, "mainFragment", mainFragment);
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

        switch (id) {

            case android.R.id.home:
                if(mainFragment!=null) {
                    EditorFragment editorFragment = mainFragment.getSelectedTab();
                    if (editorFragment != null) {
                        updateNavViews(editorFragment.getFileName(), editorFragment.getFileInfo());
                        updateExtension(editorFragment.getFileExtension());
                    }
                }
                drawerLayout.openDrawer(GravityCompat.START);
                return true;

            case R.id.action_add:
                addFile();
                return true;
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addFile() {
        final Context context = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New File : ");

        View root = getLayoutInflater().inflate(R.layout.dialog_new_file, null);
        builder.setView(root);

        final TextView dir = (TextView) root.findViewById(R.id.dir_path);
        final EditText file_name = (EditText) root.findViewById(R.id.file_name);
        final FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.open);
        dir.setText(DialogConfigs.DEFAULT_DIR);

        View.OnClickListener openDir = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.DIR_SELECT;
                properties.root = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = null;

                final FilePickerDialog dialog = new FilePickerDialog(context, properties);
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        if (files.length == 0) {
                            Toast.makeText(context, "No directory selected", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dir.setText(files[0]);
                    }
                });
                dialog.show();
            }
        };

        fab.setOnClickListener(openDir);
        dir.setOnClickListener(openDir);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!Pattern.compile("[_a-zA-Z0-9\\-\\.]+").matcher(file_name.getText().toString()).matches()) {
                    Toast.makeText(context, "Invalid File name", Toast.LENGTH_SHORT).show();
                    return;
                }

                File file = new File(dir.getText().toString() + File.separator + file_name.getText().toString());
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                        Toast.makeText(context, "New file created", Toast.LENGTH_SHORT).show();
                        mainFragment.addTab(file.getPath());
                    } catch (IOException e) {
                        Toast.makeText(context, "Couldn't create new file : " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    mainFragment.addTab(file.getPath());
                }

            }
        });
        builder.show();


    }

    public String[] getSavedFiles() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String json = sharedPref.getString("files", null);
        String[] files = null;
        if (json != null) {
            try {
                JSONArray a = new JSONArray(json);
                files = new String[a.length()];
                for (int i = 0; i < a.length(); i++) {
                    files[i] = a.optString(i);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return files;
    }

    @Override
    protected void onPause() {

        if (mainFragment == null) {
            super.onPause();
            return;
        }

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String[] files = mainFragment.getOpenFiles();
        SharedPreferences.Editor editor = sharedPref.edit();
        JSONArray array = new JSONArray();

        for (int i = 0; i < files.length; i++) {
            array.put(files[i]);
        }

        if (files.length > 0) {
            editor.putString("files", array.toString());
        } else {
            editor.putString("files", null);
        }
        editor.commit();

        super.onPause();
    }

    private void updateNavViews(String header, String projectInfo) {
        headerProject.setText(header);
        projectStructure.setText(projectInfo);
    }

    private void updateExtension(String extension) {
        ImageView extImage = (ImageView) findViewById(R.id.extImage);
        TextView extText = (TextView) findViewById(R.id.extText);

        if (extImage == null || extText == null)
            return;

        extImage.setVisibility(View.VISIBLE);

        ExtensionManager.Language language = ExtensionManager.getLanguage(extension);

        if (language == ExtensionManager.Language.NONE) {
            extImage.setVisibility(View.GONE);
            extText.setVisibility(View.VISIBLE);
            extText.setText(extension);
        } else if (language == ExtensionManager.Language.TEXT) {
            extImage.setImageResource(R.drawable.vector_txt);
        } else if (language == ExtensionManager.Language.PYTHON) {
            extImage.setImageResource(R.drawable.vector_python);
        } else if (language == ExtensionManager.Language.JAVA) {
            extImage.setImageResource(R.drawable.vector_java);
        } else if (language == ExtensionManager.Language.HTML) {
            extImage.setImageResource(R.drawable.vector_html);
        } else if (language == ExtensionManager.Language.CSS) {
            extImage.setImageResource(R.drawable.vector_css);
        } else if (language == ExtensionManager.Language.PHP) {
            extImage.setImageResource(R.drawable.vector_php);
        } else if (language == ExtensionManager.Language.XML) {
            extImage.setImageResource(R.drawable.vector_xml);
        } else if (language == ExtensionManager.Language.C) {
            extImage.setImageResource(R.drawable.vector_cpp);
        }
    }

}
