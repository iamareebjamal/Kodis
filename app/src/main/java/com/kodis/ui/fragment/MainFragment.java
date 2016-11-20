package com.kodis.ui.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.kodis.R;
import com.kodis.listener.FileChangeListener;
import com.kodis.listener.OnScrollListener;
import com.kodis.ui.MainActivity;
import com.kodis.ui.adapter.ViewPagerAdapter;
import com.kodis.utils.PermissionManager;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MainFragment extends Fragment implements FileChangeListener, OnScrollListener, Serializable {

    private View rootView;
    private FloatingActionButton fab;

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter viewPagerAdapter;

    private List<Fragment> retainedPages;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        retainedPages = viewPagerAdapter.getFragmentList();
        if (retainedPages.size() > 0)
            outState.putSerializable("pages", (Serializable) retainedPages);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (savedInstanceState != null) {
            retainedPages = (List<Fragment>) savedInstanceState.getSerializable("pages");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        this.rootView = rootView;

        setupViews();
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_editor, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_close:
                closeTab();
                return true;
            default:
                break;
        }

        return false;
    }

    private void setupViews() {
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        setInitialFAB();

        viewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
        viewPagerAdapter = new ViewPagerAdapter(getActivity().getSupportFragmentManager());
        if (retainedPages != null && retainedPages.size() > 0) {
            for (Fragment fragment : retainedPages) {
                viewPagerAdapter.addFragment(fragment);
            }
        }
        viewPager.setAdapter(viewPagerAdapter);

        tabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.setVisibility(View.GONE);
        PermissionManager.verifyStoragePermissions(getActivity());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String[] files = ((MainActivity) getActivity()).getSavedFiles();
        if (files != null && files.length > 0) {
            for (String file : files) {
                if (!isOpen(file))
                    addTab(file);
            }
        }
    }

    public void addTab(String path) {
        File file = new File(path);
        EditorFragment fragment = new EditorFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(EditorFragment.FILE_KEY, file);
        fragment.setArguments(bundle);
        fragment.setFileChangeListener(this);

        viewPagerAdapter.addFragment(fragment);
        viewPagerAdapter.notifyDataSetChanged();
    }


    private void setInitialFAB() {
        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        setOpenFAB();
    }

    private void setSaveFAB() {
        fab.setImageResource(R.drawable.vector_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditorFragment editorFragment = (EditorFragment) viewPagerAdapter.getItem(tabLayout.getSelectedTabPosition());
                if (editorFragment != null)
                    editorFragment.save();
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                closeTab();

                if (!((EditorFragment) viewPagerAdapter.getItem(tabLayout.getSelectedTabPosition())).isChanged())
                    setOpenFAB();
                return true;
            }
        });
    }

    private void setOpenFAB() {
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
                if (tabLayout.getTabCount() > 0) //added this
                    closeTab();
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
                if (files.length == 0) {
                    Toast.makeText(getContext(), "No file selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isOpen(files[0])) {
                    Toast.makeText(getContext(), "File already open", Toast.LENGTH_SHORT).show();
                    return;
                }
                addTab(files[0]);
            }
        });
        dialog.show();
    }

    private boolean isOpen(String path) {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            File file = new File(path);

            if (((EditorFragment) viewPagerAdapter.getItem(i)).getFilePath().equals(file.getPath())) {

                return true;
            }
        }
        return false;
    }

    private void removeTab(int position) {
        if (tabLayout.getTabCount() < 1 || position >= tabLayout.getTabCount())
            return;

        tabLayout.removeTabAt(position);
        viewPagerAdapter.removeTabPage(position);
        viewPager.setAdapter(viewPagerAdapter);
        if (tabLayout.getTabCount() <= 0) {
            tabLayout.setVisibility(View.GONE);
            rootView.findViewById(R.id.intro).setVisibility(View.VISIBLE);
        }
        setInitialFAB();
    }

    private void closeTab() {
        if (tabLayout.getTabCount() <= 0) {
            Toast.makeText(getContext(), "No file is open", Toast.LENGTH_SHORT).show();
            return;
        }

        final int position = tabLayout.getSelectedTabPosition();
        final EditorFragment editorFragment = (EditorFragment) viewPagerAdapter.getItem(position);

        if (editorFragment != null && editorFragment.isChanged()) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("File is changed")
                    .setMessage("Do you want to save file before quitting?")
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            editorFragment.save();
                            removeTab(position);

                            viewPagerAdapter.notifyDataSetChanged();

                        }
                    })
                    .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            removeTab(position);
                            viewPagerAdapter.notifyDataSetChanged();

                        }
                    })
                    .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
        } else {
            removeTab(position);
        }

    }

    public String[] getOpenFiles() {
        String[] files = new String[viewPagerAdapter.getCount()];
        for (int i = 0; i < viewPagerAdapter.getCount(); i++) {
            EditorFragment editorFragment = (EditorFragment) viewPagerAdapter.getItem(i);
            files[i] = editorFragment.getFilePath();
        }

        return files;
    }

    public EditorFragment getSelectedTab(){
        if(viewPagerAdapter.getCount() > 0)
            return (EditorFragment) viewPagerAdapter.getItem(tabLayout.getSelectedTabPosition());

        return null;
    }

    @Override
    public void onFileOpen() {
        tabLayout.setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.intro).setVisibility(View.GONE);
    }

    @Override
    public void onFileChanged(boolean save) {
        if (save)
            setSaveFAB();
        else
            setOpenFAB();
    }

    @Override
    public void onFileSave() {
        setOpenFAB();
    }

    @Override
    public void onScrolled() {

    }

    @Override
    public void onScrolledUp() {
        fab.show();
    }

    @Override
    public void onScrolledDown() {
        fab.hide();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        try {
            super.onDestroy();
        } catch (NullPointerException npe) {
            Log.e(TAG, "NPE: Bug workaround");
        }
    }

}
