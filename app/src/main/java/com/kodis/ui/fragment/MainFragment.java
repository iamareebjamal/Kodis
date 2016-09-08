package com.kodis.ui.fragment;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.kodis.R;
import com.kodis.listener.FileChangeListener;
import com.kodis.ui.adapder.ViewPagerAdapter;
import com.kodis.utils.PermissionManager;

import java.io.*;

public class MainFragment extends Fragment implements FileChangeListener {


    private View rootView;
    private FloatingActionButton fab;

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter viewPagerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        this.rootView = rootView;

        setupViews();
        return rootView;
    }

    private void setupViews(){
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);


        setInitialFAB();

        viewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
        viewPagerAdapter = new ViewPagerAdapter(getActivity().getSupportFragmentManager());
        setupViewPager();

        tabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setVisibility(View.GONE);

        PermissionManager.verifyStoragePermissions(getActivity());
    }

    private void setupViewPager() {
        viewPager.setAdapter(viewPagerAdapter);
    }

    private void addTab(String path){
        File file = new File(path);

        EditorFragment fragment = new EditorFragment();

        Bundle bundle = new Bundle();
        bundle.putSerializable(EditorFragment.FILE_KEY, file);
        fragment.setArguments(bundle);
        fragment.setFileChangeListener(this);

        viewPagerAdapter.addFragment(fragment, file.getName());
        viewPagerAdapter.notifyDataSetChanged();
    }

    private void setInitialFAB() {
        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        setOpenFAB();
    }

    private void setSaveFAB(){
        fab.setImageResource(R.drawable.vector_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditorFragment editorFragment = (EditorFragment) viewPagerAdapter.getItem(tabLayout.getSelectedTabPosition());
                if(editorFragment!=null)
                    editorFragment.save();
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getContext(), "Save File", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "Open File", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void openFilePicker() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        FilePickerDialog dialog = new FilePickerDialog(getActivity(), properties);
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                addTab(files[0]);
            }
        });
        dialog.show();
    }


    @Override
    public void onFileOpen() {
        tabLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFileChanged(boolean save) {
        if(save)
            setSaveFAB();
        else
            setOpenFAB();
    }

    @Override
    public void onFileSave() {
        setOpenFAB();
    }
}
