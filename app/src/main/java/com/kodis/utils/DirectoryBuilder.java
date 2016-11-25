package com.kodis.utils;

import android.content.Context;
import android.os.AsyncTask;
import com.kodis.R;
import com.kodis.holder.IconTreeItemHolder;
import com.kodis.listener.OnDirectoryBuiltListener;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.io.File;
import java.io.FilenameFilter;

public class DirectoryBuilder {
    private Context context;
    private String path;
    private OnDirectoryBuiltListener onDirectoryBuiltListener;

    public DirectoryBuilder(Context context, String path){
        this.context = context;
        this.path = path;
    }

    public void setOnDirectoryBuiltListener(OnDirectoryBuiltListener onDirectoryBuiltListener) {
        this.onDirectoryBuiltListener = onDirectoryBuiltListener;
    }

    public void start(){
        new DirectoryBuild().execute();
    }

    private class DirectoryBuild extends AsyncTask<Void, Void, AndroidTreeView>{

        private void buildDirectory(TreeNode root, File path){
            File[] files = path.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String name) {
                    return !name.startsWith(".");
                }
            });

            if(files == null)
                return;

            for(File file : files) {
                if(file.isDirectory()){
                    TreeNode folderNode = new TreeNode(new IconTreeItemHolder.FileTreeItem(R.drawable.vector_open, file.getName(), file.getAbsolutePath()));
                    buildDirectory(folderNode, file);
                    root.addChild(folderNode);
                } else {
                    TreeNode folderNode = new TreeNode(new IconTreeItemHolder.FileTreeItem(ExtensionManager.getIcon(file.getName()), file.getName(), file.getAbsolutePath()));
                    root.addChild(folderNode);
                }
            }

        }

        @Override
        protected AndroidTreeView doInBackground(Void... voids) {
            TreeNode root = TreeNode.root();
            buildDirectory(root, new File(path).getParentFile());
            AndroidTreeView treeView = new AndroidTreeView(context, root);

            return treeView;
        }

        @Override
        protected void onPostExecute(AndroidTreeView androidTreeView) {
            if(onDirectoryBuiltListener!=null)
                onDirectoryBuiltListener.onDirectoryBuilt(androidTreeView);
        }
    }
}
