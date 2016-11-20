package com.kodis.holder;

import android.content.Context;
import android.media.Image;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.kodis.R;
import com.unnamed.b.atv.model.TreeNode;

public class IconTreeItemHolder extends TreeNode.BaseNodeViewHolder<IconTreeItemHolder.FileTreeItem> {

    ImageView arrow;

    public IconTreeItemHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode node, FileTreeItem value) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_tree_view, null, false);
        TextView fileName = (TextView) view.findViewById(R.id.file_browser_name);
        ImageView fileIcon = (ImageView) view.findViewById(R.id.file_browser_icon);
        arrow = (ImageView) view.findViewById(R.id.file_browser_arrow);

        fileName.setText(value.text);
        fileIcon.setImageResource(value.icon);

        if (node.isLeaf()) {
            arrow.setVisibility(View.INVISIBLE);
        }

        if (!node.isExpanded()) {
            arrow.setRotation(0);
        }

        return view;
    }

    @Override
    public void toggle(boolean active) {
        ViewCompat.animate(arrow).rotation(active?90:0);
    }

    public static class FileTreeItem {

        public int icon;

        public String text;
        public String path;

        public FileTreeItem(int icon, String text, String path) {
            this.icon = icon;
            this.text = text;
            this.path = path;
        }
    }
}