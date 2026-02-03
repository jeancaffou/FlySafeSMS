package pro.flysafe.sms.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pro.flysafe.sms.R;
import pro.flysafe.sms.model.Friend;
import pro.flysafe.sms.util.Util;
import pro.flysafe.sms.util.VolleyImageLoader;

public class FriendAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnLocToggleListener {
        void onLocToggle(Friend friend, boolean enabled);
    }

    public interface OnRemoveListener {
        void onRemove(Friend friend);
    }

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_FRIEND = 1;

    private final Context ctx;
    private final ArrayList<ListItem> items = new ArrayList<>();
    private final OnLocToggleListener locToggleListener;
    private final OnRemoveListener removeListener;
    private final OnFriendClickListener clickListener;

    public FriendAdapter(Context ctx,
                         OnLocToggleListener locToggleListener,
                         OnRemoveListener removeListener,
                         OnFriendClickListener clickListener) {
        this.ctx = ctx;
        this.locToggleListener = locToggleListener;
        this.removeListener = removeListener;
        this.clickListener = clickListener;
    }

    public void setFriends(List<Friend> friends) {
        items.clear();
        if (friends == null || friends.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        ArrayList<Friend> outgoing = new ArrayList<>();
        ArrayList<Friend> incoming = new ArrayList<>();
        ArrayList<Friend> neutral = new ArrayList<>();

        for (Friend friend : friends) {
            boolean sharesFromMe = friend.isFriend && friend.loc;
            if (sharesFromMe) {
                outgoing.add(friend);
            } else if (friend.sharedWithMe) {
                incoming.add(friend);
            } else {
                neutral.add(friend);
            }
        }

        sortByName(outgoing);
        sortByName(incoming);
        sortByName(neutral);

        if (!outgoing.isEmpty()) {
            items.add(new HeaderItem(ctx.getString(R.string.friends_section_sharing_my_location)));
            for (Friend friend : outgoing) {
                items.add(new FriendItem(friend));
            }
        }

        if (!incoming.isEmpty()) {
            items.add(new HeaderItem(ctx.getString(R.string.friends_section_shared_with_me)));
            for (Friend friend : incoming) {
                items.add(new FriendItem(friend));
            }
        }

        if (!neutral.isEmpty()) {
            items.add(new HeaderItem(ctx.getString(R.string.friends_section_no_sharing)));
            for (Friend friend : neutral) {
                items.add(new FriendItem(friend));
            }
        }

        notifyDataSetChanged();
    }

    public Friend getFriendAt(int adapterPosition) {
        if (adapterPosition < 0 || adapterPosition >= items.size()) {
            return null;
        }
        ListItem item = items.get(adapterPosition);
        if (item instanceof FriendItem) {
            return ((FriendItem) item).friend;
        }
        return null;
    }

    public boolean isHeaderPosition(int adapterPosition) {
        if (adapterPosition < 0 || adapterPosition >= items.size()) {
            return false;
        }
        return items.get(adapterPosition) instanceof HeaderItem;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof HeaderItem ? TYPE_HEADER : TYPE_FRIEND;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(ctx);
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.row_friend_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = inflater.inflate(R.layout.row_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((HeaderItem) item);
        } else if (holder instanceof FriendViewHolder) {
            ((FriendViewHolder) holder).bind(((FriendItem) item).friend);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void sortByName(List<Friend> friends) {
        Collections.sort(friends, Comparator.comparing(a -> a.name == null ? "" : a.name.toLowerCase()));
    }

    private void loadAvatar(ImageView avatar, Friend friend) {
        // Build the same circle-image URL as the main app using the stored session.
        // No secrets are hardcoded; the user id is derived from local preferences.
        String userId = Util.getUserId(ctx);
        if (Util.isEmpty(friend.uid) || Util.isEmpty(userId)) {
            avatar.setTag(null);
            avatar.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }

        String url = Util.API_ENDPOINT + "/image/circle/" + friend.uid + "/?__autologin=" + userId;
        ImageLoader loader = VolleyImageLoader.get(ctx);
        avatar.setTag(url);
        avatar.setImageResource(R.drawable.ic_person_placeholder);
        ImageLoader.ImageListener listener = new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                if (!url.equals(avatar.getTag())) {
                    return;
                }
                if (response.getBitmap() != null) {
                    avatar.setImageBitmap(response.getBitmap());
                }
            }

            @Override
            public void onErrorResponse(com.android.volley.VolleyError error) {
                if (url.equals(avatar.getTag())) {
                    avatar.setImageResource(R.drawable.ic_person_placeholder);
                }
            }
        };
        loader.get(url, listener);
    }

    private abstract static class ListItem {
    }

    private static class HeaderItem extends ListItem {
        final String title;

        HeaderItem(String title) {
            this.title = title;
        }
    }

    private static class FriendItem extends ListItem {
        final Friend friend;

        FriendItem(Friend friend) {
            this.friend = friend;
        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.friendHeaderTitle);
        }

        void bind(HeaderItem item) {
            title.setText(item.title);
        }
    }

    private class FriendViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView status;
        private final ImageView avatar;
        private final SwitchMaterial locSwitch;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.friendName);
            status = itemView.findViewById(R.id.friendStatus);
            avatar = itemView.findViewById(R.id.friendAvatar);
            locSwitch = itemView.findViewById(R.id.friendLocSwitch);
        }

        void bind(Friend friend) {
            String label = Util.isEmpty(friend.name) ? "Friend" : friend.name;
            name.setText(label);
            loadAvatar(avatar, friend);

            if (friend.isFriend) {
                locSwitch.setVisibility(View.VISIBLE);
                locSwitch.setOnCheckedChangeListener(null);
                locSwitch.setChecked(friend.loc);
                locSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    friend.loc = isChecked;
                    if (locToggleListener != null) {
                        locToggleListener.onLocToggle(friend, isChecked);
                    }
                });
            } else {
                locSwitch.setVisibility(View.GONE);
            }

            boolean sharesFromMe = friend.isFriend && friend.loc;
            boolean mutual = sharesFromMe && friend.sharedWithMe;
            String statusText;
            int statusColor;
            int rowColor;

            if (mutual) {
                statusText = ctx.getString(R.string.friends_status_both);
                statusColor = ContextCompat.getColor(ctx, R.color.sms_status_both);
                rowColor = ContextCompat.getColor(ctx, R.color.sms_share_highlight);
            } else if (sharesFromMe) {
                statusText = ctx.getString(R.string.friends_status_sharing_my_location);
                statusColor = ContextCompat.getColor(ctx, R.color.sms_status_outgoing);
                rowColor = ContextCompat.getColor(ctx, R.color.sms_share_highlight);
            } else if (friend.sharedWithMe) {
                statusText = ctx.getString(R.string.friends_status_shared_with_me);
                statusColor = ContextCompat.getColor(ctx, R.color.sms_status_incoming);
                rowColor = ContextCompat.getColor(ctx, R.color.sms_share_subtle);
            } else {
                statusText = ctx.getString(R.string.friends_status_no_sharing);
                statusColor = ContextCompat.getColor(ctx, R.color.sms_status_neutral);
                rowColor = Color.TRANSPARENT;
            }

            name.setTypeface(name.getTypeface(), friend.loc ? Typeface.BOLD : Typeface.NORMAL);
            status.setText(statusText);
            status.setTextColor(statusColor);
            itemView.setBackgroundColor(rowColor);

            itemView.setOnClickListener(view -> {
                if (clickListener != null) {
                    clickListener.onFriendClick(friend);
                }
            });
        }
    }
}
