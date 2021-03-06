package com.inspiredandroid.linuxcommandbibliotheca.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.inspiredandroid.linuxcommandbibliotheca.R;
import com.inspiredandroid.linuxcommandbibliotheca.models.Command;
import com.inspiredandroid.linuxcommandbibliotheca.view.TerminalTextView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;

/**
 * Created by Simon Schubert
 */
public class ManExpandableListAdapter extends BaseExpandableListAdapter {

    public ArrayList<ArrayList<CharSequence>> mChild;
    private Activity mContext;
    private ArrayList<String> mGroup;

    public ManExpandableListAdapter(Activity context, ArrayList<String> group, ArrayList<ArrayList<CharSequence>> child) {
        mContext = context;
        mChild = child;
        mGroup = group;
    }

    public CharSequence getChild(int groupPosition, int childPosition) {
        return mChild.get(groupPosition).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        CharSequence description = getChild(groupPosition, childPosition);
        CommandViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            convertView = inflater.inflate(R.layout.row_man_child, parent, false);
            holder = new CommandViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (CommandViewHolder) convertView.getTag();
        }

        holder.desc.setText(description);

        if (getGroup(groupPosition).toString().toUpperCase().equals("SEE ALSO")) {
            holder.desc.setCommands(extractCommandsFromDescription(description.toString()));
        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChild.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mGroup.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return mGroup.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String title = (String) getGroup(groupPosition);
        CommandGroupViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            convertView = inflater.inflate(R.layout.row_man_group, parent, false);
            holder = new CommandGroupViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (CommandGroupViewHolder) convertView.getTag();
        }

        holder.title.setText(title.toUpperCase());

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    /**
     * Search for commands and return list of commands which exist in database
     *
     * @param description
     * @return
     */
    private String[] extractCommandsFromDescription(String description) {
        Realm realm = Realm.getDefaultInstance();

        // match "command(category)" e.g: gzip(1)
        Pattern p = Pattern.compile("[[:graph:]]+\\s?\\(\\w\\)");
        Matcher m = p.matcher(description);

        // loop results and ad if command exists in db
        ArrayList<String> tmp = new ArrayList<>();
        while (m.find()) {
            String extractedCommand = m.group(0).substring(0, m.group(0).length() - 3).trim();
            Command command = realm.where(Command.class).equalTo(Command.NAME, extractedCommand).findFirst();
            if (command != null) {
                tmp.add(extractedCommand);
            }
        }

        // convert String[] to ArrayList
        String[] commands = new String[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            String cmd = tmp.get(i);
            commands[i] = cmd;
        }

        realm.close();

        return commands;
    }

    class CommandViewHolder {
        @BindView(R.id.row_man_child_tv_description)
        TerminalTextView desc;

        CommandViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    class CommandGroupViewHolder {
        @BindView(R.id.row_man_group_tv_title)
        TextView title;

        CommandGroupViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}