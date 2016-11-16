package com.kodis.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.kodis.R;
import com.kodis.ui.fragment.MainFragment;
import com.kodis.utils.ExtensionManager;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private TextView projectStructure, headerProject;
    private transient DrawerLayout drawerLayout;
    private MainFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            mainFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.mainFragment);
        } else {
            mainFragment = (MainFragment) getSupportFragmentManager().getFragment(savedInstanceState, "mainFragment");
        }

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.navView);
        headerProject = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header_project_name);
        projectStructure = (TextView) findViewById(R.id.project_structure);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.vector_menu);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
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
                drawerLayout.openDrawer(GravityCompat.START);
                return true;

            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String[] getSavedFiles(){
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

        if(mainFragment==null) {
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

        if (files.length>0) {
            editor.putString("files", array.toString());
        } else {
            editor.putString("files", null);
        }
        editor.commit();

        super.onPause();
    }

    public void updateNavViews(String header, String projectInfo) {
        headerProject.setText(header);
        projectStructure.setText(projectInfo);
    }

    public void updateExtension(String extension){
        ImageView extImage = (ImageView) findViewById(R.id.extImage);
        TextView extText = (TextView) findViewById(R.id.extText);

        if(extImage == null || extText == null)
            return;

        extImage.setVisibility(View.VISIBLE);

        ExtensionManager.Language language = ExtensionManager.getLanguage(extension);

        if(language == ExtensionManager.Language.NONE) {
            extImage.setVisibility(View.GONE);
            extText.setVisibility(View.VISIBLE);
            extText.setText(extension);
        } else if(language == ExtensionManager.Language.TEXT){
            extImage.setImageResource(R.drawable.vector_txt);
        } else if(language == ExtensionManager.Language.PYTHON){
            extImage.setImageResource(R.drawable.vector_python);
        } else if(language == ExtensionManager.Language.JAVA){
            extImage.setImageResource(R.drawable.vector_java);
        } else if(language == ExtensionManager.Language.HTML){
            extImage.setImageResource(R.drawable.vector_html);
        } else if(language == ExtensionManager.Language.CSS){
            extImage.setImageResource(R.drawable.vector_css);
        } else if(language == ExtensionManager.Language.PHP){
            extImage.setImageResource(R.drawable.vector_php);
        } else if(language == ExtensionManager.Language.XML){
            extImage.setImageResource(R.drawable.vector_xml);
        } else if(language == ExtensionManager.Language.C){
            extImage.setImageResource(R.drawable.vector_cpp);
        }
    }

}
