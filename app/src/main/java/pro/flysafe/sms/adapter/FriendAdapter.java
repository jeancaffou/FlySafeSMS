package pro.flysafe.sms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.android.volley.toolbox.ImageLoader;

import java.util.ArrayList;

import pro.flysafe.sms.R;
import pro.flysafe.sms.model.Friend;
import pro.flysafe.sms.util.Util;
import pro.flysafe.sms.util.VolleyImageLoader;

public class FriendAdapter extends BaseAdapter {
    public interface OnLocToggleListener {
        void onLocToggle(Friend friend, boolean enabled);
    }

    public interface OnRemoveListener {
        void onRemove(Friend friend);
    }

    private final Context ctx;
    private final ArrayList<Friend> friends = new ArrayList<>();
    private final OnLocToggleListener listener;
    private final OnRemoveListener removeListener;

    public FriendAdapter(Context ctx, OnLocToggleListener listener) {
        this(ctx, listener, null);
    }

    public FriendAdapter(Context ctx, OnLocToggleListener listener, OnRemoveListener removeListener) {
        this.ctx = ctx;
        this.listener = listener;
        this.removeListener = removeListener;
    }

    public void setFriends(ArrayList<Friend> items) {
        // Replace current list with freshly fetched data.
        friends.clear();
        if (items != null) {
            friends.addAll(items);
        }
        notifyDataSetChanged();
    }

    public Friend getFriend(int position) {
        return friends.get(position);
    }

    @Override
    public int getCount() {
        return friends.size();
    }

    @Override
    public Object getItem(int position) {
        return friends.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(ctx).inflate(R.layout.row_friend, parent, false);
        }

        Friend friend = getFriend(position);
        TextView name = view.findViewById(R.id.friendName);
        ImageView avatar = view.findViewById(R.id.friendAvatar);
        ImageView remove = view.findViewById(R.id.friendRemove);
        SwitchMaterial locSwitch = view.findViewById(R.id.friendLocSwitch);

        // Show friend and allow toggling location permission for this friend.
        name.setText(friend.name);
        loadAvatar(avatar, friend);
        locSwitch.setOnCheckedChangeListener(null);
        locSwitch.setChecked(friend.loc);
        locSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            friend.loc = isChecked;
            if (listener != null) {
                listener.onLocToggle(friend, isChecked);
            }
        });

        // Explicit remove affordance to avoid relying on long-press behavior.
        remove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(friend);
            }
        });

        return view;
    }

    private void loadAvatar(ImageView avatar, Friend friend) {
        // Build the same circle-image URL as the main app using the stored session.
        // No secrets are hardcoded; the user id is derived from local preferences.
        String userId = Util.getUserId(ctx);
        if (Util.isEmpty(friend.uid) || Util.isEmpty(userId)) {
            avatar.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }

        String url = Util.API_ENDPOINT + "/image/circle/" + friend.uid + "/?__autologin=" + userId;
        ImageLoader loader = VolleyImageLoader.get(ctx);
        ImageLoader.ImageListener listener = ImageLoader.getImageListener(
                avatar,
                R.drawable.ic_person_placeholder,
                R.drawable.ic_person_placeholder
        );
        loader.get(url, listener);
    }
}
