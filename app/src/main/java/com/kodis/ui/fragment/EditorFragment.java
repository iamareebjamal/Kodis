package com.kodis.ui.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.kodis.R;
import com.kodis.listener.FileChangeListener;
import com.kodis.listener.OnBottomReachedListener;
import com.kodis.listener.OnScrollListener;
import com.kodis.ui.component.InteractiveScrollView;

import java.io.*;

public class EditorFragment extends Fragment implements TextWatcher {
    public static final String FILE_KEY = "FILE";

    private File file;
    private FileChangeListener fileChangeListener;

    private int CHUNK = 20000;
    private String FILE_CONTENT;
    private String currentBuffer;
    private StringBuilder loaded;

    private View rootView;

    private EditText contentView;
    private View hidden;

    public EditorFragment() {
    }

    public void setArguments(Bundle arguments) {
        if (arguments.containsKey(FILE_KEY))
            file = (File) arguments.getSerializable(FILE_KEY);
    }

    public void setFileChangeListener(FileChangeListener fileChangeListener) {
        this.fileChangeListener = fileChangeListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_editor, container, false);
        this.rootView = rootView;

        setupViews();
        return rootView;
    }

    public void setupViews() {
        contentView = (EditText) rootView.findViewById(R.id.fileContent);
        hidden = rootView.findViewById(R.id.hidden);

        if (file != null) {
            contentView.setVisibility(View.GONE);
            hidden.setVisibility(View.VISIBLE);
            new DocumentLoader().execute();
        }
    }

    private boolean isChanged() {
        if (FILE_CONTENT.length() >= CHUNK && FILE_CONTENT.substring(0, loaded.length()).equals(currentBuffer))
            return false;
        else if (FILE_CONTENT.equals(currentBuffer))
            return false;

        return true;
    }

    private void loadInChunks(InteractiveScrollView scrollView, final String bigString) {
        loaded.append(bigString.substring(0, CHUNK));
        contentView.setText(loaded);
        scrollView.setOnBottomReachedListener(new OnBottomReachedListener() {
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

                contentView.setText(loaded);
            }
        });
    }

    private void loadDocument(final String fileContent) {

        final InteractiveScrollView scrollView = (InteractiveScrollView) rootView.findViewById(R.id.scrollView);
        scrollView.setOnBottomReachedListener(null);
        scrollView.setOnScrollListener((OnScrollListener) fileChangeListener);
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

        if (isFileChangeListenerAttached()) fileChangeListener.onFileOpen();
    }

    public void save() {
        if (isChanged())
            new DocumentSaver().execute();
        else
            Toast.makeText(getContext(), "No change in file", Toast.LENGTH_SHORT).show();
    }

    private boolean isFileChangeListenerAttached() {
        return fileChangeListener != null;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        currentBuffer = contentView.getText().toString();
        if (isFileChangeListenerAttached()) fileChangeListener.onFileChanged(isChanged());
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }


    private class DocumentLoader extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... paths) {

            try {
                BufferedReader br = new BufferedReader(new FileReader(file.getAbsoluteFile()));
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
                    } catch (IOException ioe) {
                    }
                }
            } catch (FileNotFoundException fnfe) {
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            loadDocument(s);
        }
    }

    private class DocumentSaver extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            BufferedWriter output = null;
            String toSave = currentBuffer;
            try {
                output = new BufferedWriter(new FileWriter(file));
                if (FILE_CONTENT.length() > CHUNK) {
                    toSave = currentBuffer + FILE_CONTENT.substring(loaded.length(), FILE_CONTENT.length());
                }
                output.write(toSave);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (output != null) {
                    try {
                        output.close();
                        FILE_CONTENT = toSave;
                    } catch (IOException ioe) {
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
            if (isFileChangeListenerAttached()) fileChangeListener.onFileSave();
        }
    }
}
