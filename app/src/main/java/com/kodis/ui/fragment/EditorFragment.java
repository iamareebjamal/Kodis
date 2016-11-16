package com.kodis.ui.fragment;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.kodis.R;
import com.kodis.listener.FileChangeListener;
import com.kodis.listener.OnBottomReachedListener;
import com.kodis.listener.OnScrollListener;
import com.kodis.ui.MainActivity;
import com.kodis.ui.component.CodeEditText;
import com.kodis.ui.component.InteractiveScrollView;

import java.io.*;

public class EditorFragment extends Fragment implements TextWatcher, Serializable {
    public static final String FILE_KEY = "FILE";

    private transient Context context;
    private File file;
    private transient FileChangeListener fileChangeListener;

    private int CHUNK = 20000;
    private String FILE_CONTENT;
    private String currentBuffer;
    private StringBuilder loaded;

    private transient View rootView;

    private transient CodeEditText contentView;
    private transient View hidden;
    private transient InteractiveScrollView scrollView;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("CHUNK", CHUNK);
        outState.putSerializable(FILE_KEY, file);
        outState.putString("FILE_CONTENT", FILE_CONTENT);
        outState.putString("currentBuffer", currentBuffer);
        outState.putSerializable("loaded", loaded);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_editor, container, false);
        this.rootView = rootView;

        if(savedInstanceState!=null) {
            CHUNK = savedInstanceState.getInt("CHUNK");
            file = (File) savedInstanceState.getSerializable(FILE_KEY);
            FILE_CONTENT = savedInstanceState.getString("FILE_CONTENT");
            currentBuffer = savedInstanceState.getString("currentBuffer");
            loaded = (StringBuilder) savedInstanceState.getSerializable("loaded");
        }

        setupViews();
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
    }

    public void setupViews() {
        contentView = (CodeEditText) rootView.findViewById(R.id.fileContent);
        hidden = rootView.findViewById(R.id.hidden);

        LinearLayout symbolLayout = (LinearLayout) rootView.findViewById(R.id.symbolLayout);
        View.OnClickListener symbolClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contentView.getText().insert(contentView.getSelectionStart(), ((TextView) view).getText().toString() );
            }
        };
        for(int i = 0; i < symbolLayout.getChildCount(); i++){
            symbolLayout.getChildAt(i).setOnClickListener(symbolClickListener);
        }

        if (file != null) {
            contentView.setVisibility(View.GONE);
            contentView.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Consolas.ttf"));

            hidden.setVisibility(View.VISIBLE);
            scrollView = (InteractiveScrollView) rootView.findViewById(R.id.scrollView);
            scrollView.setOnBottomReachedListener(null);
            scrollView.setOnScrollListener((OnScrollListener) fileChangeListener);

            if(FILE_CONTENT!=null && loaded!=null && currentBuffer!=null){
                restoreDocument();
            } else {
                new DocumentLoader().execute();
            }
        }
    }

    private String getFileSize() {
        String modifiedFileSize = null;
        double fileSize = 0.0;
        if (file.isFile()) {
            fileSize = (double) file.length();//in Bytes

            if (fileSize < 1024) {
                modifiedFileSize = String.valueOf(fileSize).concat("B");
            } else if (fileSize > 1024 && fileSize < (1024 * 1024)) {
                modifiedFileSize = String.valueOf(Math.round((fileSize / 1024 * 100.0)) / 100.0).concat("KB");
            } else {
                modifiedFileSize = String.valueOf(Math.round((fileSize / (1024 * 1204) * 100.0)) / 100.0).concat("MB");
            }
        } else {
            modifiedFileSize = "Unknown";
        }

        return modifiedFileSize;
    }

    public String getFileExtension() {
        int index = file.getName().lastIndexOf('.');
        if(index > 0) {
            return file.getName().substring(index+1);
        }

        return null;
    }

    public String getFilePath() {
        return file.getPath();
    }

    public String getFileName() {
        return file.getName();
    }

    public String getFileInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Size : " + getFileSize() + "\n");
        sb.append("Path : " + file.getPath() + "\n");

        return sb.toString();
    }

    public boolean isChanged() {
        if(FILE_CONTENT == null) {
            return false;
        }

        if (FILE_CONTENT.length() >= CHUNK && FILE_CONTENT.substring(0, loaded.length()).equals(currentBuffer))
            return false;
        else if (FILE_CONTENT.equals(currentBuffer))
            return false;

        return true;
    }

    private void loadInChunks(InteractiveScrollView scrollView, final String bigString) {
        loaded.append(bigString.substring(0, CHUNK));
        contentView.setTextHighlighted(loaded);
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

                contentView.setTextHighlighted(loaded);
            }
        });
    }

    private void loadDocument(final String fileContent) {
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
            contentView.setTextHighlighted(loaded);
        }


        hidden.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        contentView.addTextChangedListener(this);
        currentBuffer = contentView.getText().toString();

        if (isFileChangeListenerAttached()) fileChangeListener.onFileOpen();
    }

    private void restoreDocument(){
        scrollView.smoothScrollTo(0, 0);

        contentView.setFocusable(false);
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contentView.setFocusableInTouchMode(true);
            }
        });
        contentView.setTextHighlighted(loaded);

        hidden.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        contentView.addTextChangedListener(this);

        if (isFileChangeListenerAttached()) fileChangeListener.onFileOpen();
    }

    public void save() {
        if (isChanged())
            new DocumentSaver().execute();
        else
            Toast.makeText(getContext(), "No change in file", Toast.LENGTH_SHORT).show();
    }

    private void onPostSave(){
        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();

        if (isFileChangeListenerAttached()) fileChangeListener.onFileSave();
    }

    private boolean isFileChangeListenerAttached() {
        return fileChangeListener != null;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

    @Override
    public void afterTextChanged(Editable editable) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                currentBuffer = contentView.getText().toString();

                if (isFileChangeListenerAttached()) fileChangeListener.onFileChanged(isChanged());
            }
        }, 1000);
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
            onPostSave();
        }
    }
}
