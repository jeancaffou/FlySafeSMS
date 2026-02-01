package pro.flysafe.sms;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;

import pro.flysafe.sms.adapter.FriendAdapter;
import pro.flysafe.sms.adapter.PeopleAdapter;
import pro.flysafe.sms.model.Friend;
import pro.flysafe.sms.model.Person;
import pro.flysafe.sms.util.PrivatePref;
import pro.flysafe.sms.util.Util;
import pro.flysafe.sms.util.SmsRequestHelper;

public class FriendsFragment extends Fragment {
    public static final String CACHE_KEY = "cache_friends_json_v1";

    private FriendAdapter adapter;
    private RequestQueue queue;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton addFriendFab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_friends, container, false);
        Context ctx = root.getContext();
        queue = Volley.newRequestQueue(ctx);

        SwitchMaterial smsEnabled = root.findViewById(R.id.smsEnabledSwitch);
        SwitchMaterial smsRoaming = root.findViewById(R.id.smsRoamingSwitch);

        // SMS auto-reply toggles are local preferences (used by SmsReceiver).
        smsEnabled.setChecked(Util.getBoolean(ctx, "pref_sms_loc", true));
        smsRoaming.setChecked(Util.getBoolean(ctx, "pref_sms_roaming", true));

        smsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> Util.save(ctx, "pref_sms_loc", isChecked));
        smsRoaming.setOnCheckedChangeListener((buttonView, isChecked) -> Util.save(ctx, "pref_sms_roaming", isChecked));

        ListView list = root.findViewById(R.id.friendsList);
        adapter = new FriendAdapter(ctx, (friend, enabled) -> {
            // Mirror the main app behavior: allow/deny location sharing per friend.
            if (enabled) {
                allowLoc(ctx, friend.uid);
            } else {
                denyLoc(ctx, friend.uid);
            }
        }, friend -> confirmRemoveFriend(ctx, friend));
        list.setAdapter(adapter);
        // Tap a friend to generate an SMS request using the same message format as FlySafe.
        list.setOnItemClickListener((parent, view, position, id) -> showRequestDialog(ctx, adapter.getFriend(position)));
        // Long-press a friend to quickly remove them.
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmRemoveFriend(ctx, adapter.getFriend(position));
            return true;
        });

        swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this::refreshFriends);

        addFriendFab = root.findViewById(R.id.addFriendFab);
        addFriendFab.setOnClickListener(view -> openSearchDialog(ctx, root));

        applyWindowInsets(root);

        setFriendsFromCache(ctx);
        refreshFriends();

        return root;
    }

    private void showRequestDialog(Context ctx, Friend friend) {
        // Provide quick actions for GPS vs last-known location requests.
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(friend.name);
        builder.setItems(new CharSequence[] {
                ctx.getString(R.string.sms_request_gps),
                ctx.getString(R.string.sms_request_lkl),
                ctx.getString(R.string.remove_friend)
        }, (dialog, which) -> {
            if (which == 0) {
                SmsRequestHelper.sendRequest(ctx, friend, "GPS");
            } else if (which == 1) {
                SmsRequestHelper.sendRequest(ctx, friend, "LastKnownLocation");
            } else if (which == 2) {
                confirmRemoveFriend(ctx, friend);
            }
        });
        builder.show();
    }

    private void confirmRemoveFriend(Context ctx, Friend friend) {
        // Confirm before removing a friend from the server list.
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.remove_friend)
                .setMessage(R.string.remove_friend_confirm)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> removeFriend(ctx, friend.uid))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setFriendsFromCache(Context ctx) {
        try {
            // Cache is shared with the main app schema for compatibility.
            JSONArray cached = new JSONArray(PrivatePref.getString(ctx, CACHE_KEY, "[]"));
            adapter.setFriends(Friend.getArray(cached));
        } catch (Exception e) {
            Util.log(e);
        }
    }

    private void refreshFriends() {
        Context ctx = getContext();
        if (ctx == null) {
            return;
        }
        swipeRefreshLayout.setRefreshing(true);

        // Pull current friends list from the same endpoint as the main app.
        JsonArrayRequest r = new JsonArrayRequest(Request.Method.GET, Util.API_ENDPOINT + "/mobile/friends/" + Util.getUserId(ctx), null,
                response -> {
                    adapter.setFriends(Friend.getArray(response));
                    PrivatePref.save(ctx, CACHE_KEY, response.toString());
                    swipeRefreshLayout.setRefreshing(false);
                },
                error -> {
                    Util.log(error.toString());
                    swipeRefreshLayout.setRefreshing(false);
                }
        );
        r.setTag(this);
        queue.add(r);
    }

    private void allowLoc(Context ctx, String uid) {
        // Server-side toggle; friends list is refreshed after response.
        apiFriend(ctx, uid, "allowLoc");
    }

    private void denyLoc(Context ctx, String uid) {
        apiFriend(ctx, uid, "denyLoc");
    }

    private void removeFriend(Context ctx, String uid) {
        apiFriend(ctx, uid, "removeFriend");
    }

    private void apiFriend(Context ctx, String friend, String action) {
        // Reuse the same friend-management API endpoints as FlySafe.
        JsonArrayRequest r = new JsonArrayRequest(Request.Method.GET, Util.API_ENDPOINT + "/mobile/" + action + "/" + Util.getUserId(ctx) + "/" + friend, null,
                response -> refreshFriends(),
                error -> refreshFriends()
        );
        r.setTag(this);
        queue.add(r);
    }

    private void openSearchDialog(Context ctx, View root) {
        // Show a people search dialog similar to the main FlySafe app.
        View layout = LayoutInflater.from(ctx).inflate(R.layout.dialog_search, root.findViewById(R.id.friendsRoot), false);
        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle(R.string.find_people)
                .setView(layout)
                .create();
        dialog.show();

        ListView peopleList = layout.findViewById(R.id.peopleList);
        PeopleAdapter peopleAdapter = new PeopleAdapter(ctx);
        peopleList.setAdapter(peopleAdapter);

        peopleList.setOnItemClickListener((parent, view, position, id) -> {
            Person person = peopleAdapter.getPerson(position);
            addFriend(ctx, person);
            dialog.dismiss();
        });

        EditText query = layout.findViewById(R.id.searchQuery);
        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    searchPeople(ctx, peopleAdapter, s.toString());
                } else if (s.length() == 0) {
                    searchPeople(ctx, peopleAdapter, "");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        });

        // Load initial suggestions.
        searchPeople(ctx, peopleAdapter, "");
    }

    private void searchPeople(Context ctx, PeopleAdapter peopleAdapter, String query) {
        // Matches the main app search endpoint for adding friends.
        String safeQuery = Uri.encode(query == null ? "" : query);
        JsonArrayRequest r = new JsonArrayRequest(Request.Method.GET, Util.API_ENDPOINT + "/mobile/searchPeople/" + safeQuery, null,
                response -> peopleAdapter.setPeople(Person.getArray(response)),
                error -> Util.log(error.toString())
        );
        r.setTag(this);
        queue.add(r);
    }

    private void addFriend(Context ctx, Person person) {
        // Add a friend using the same API action as the main app.
        if (person == null || Util.isEmpty(person.uid)) {
            return;
        }
        JsonArrayRequest r = new JsonArrayRequest(Request.Method.GET, Util.API_ENDPOINT + "/mobile/addFriend/" + Util.getUserId(ctx) + "/" + person.uid, null,
                response -> refreshFriends(),
                error -> refreshFriends()
        );
        r.setTag(this);
        queue.add(r);
    }

    private void applyWindowInsets(View root) {
        // Apply system bar insets to the fragment content and FAB.
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int[] initial = (int[]) view.getTag(R.id.initial_padding);
            if (initial == null) {
                initial = new int[] {
                        view.getPaddingLeft(),
                        view.getPaddingTop(),
                        view.getPaddingRight(),
                        view.getPaddingBottom()
                };
                view.setTag(R.id.initial_padding, initial);
            }

            int insetLeft = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
            int insetRight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;
            int insetBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            view.setPadding(
                    initial[0] + insetLeft,
                    initial[1],
                    initial[2] + insetRight,
                    initial[3] + insetBottom
            );
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(addFriendFab, (view, insets) -> {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            int[] initial = (int[]) view.getTag(R.id.initial_margin);
            if (initial == null) {
                initial = new int[] { params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin };
                view.setTag(R.id.initial_margin, initial);
            }

            int insetRight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;
            int insetBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            params.rightMargin = initial[2] + insetRight;
            params.bottomMargin = initial[3] + insetBottom;
            view.setLayoutParams(params);
            return insets;
        });
    }
}
