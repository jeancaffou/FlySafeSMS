package pro.flysafe.sms.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Friend {
    public String uid;
    public String name;
    public String lockey;
    public String phone;
    public String mylockey;
    public boolean loc;
    public boolean sharedWithMe;
    public boolean isFriend;
    public boolean hasLocData;
    public String distance;
    public double dist;
    public int bearing;
    public double locAge;
    protected boolean isLocFriend;

    public Friend(JSONObject o) {
        // Accept either "friend"/"fname" or "uid"/"name" fields depending on endpoint.
        this.name = o.optString("fname", o.optString("name", ""));
        this.uid = o.optString("friend", o.optString("uid", ""));
        this.lockey = o.optString("lockey", "");
        this.phone = o.optString("phone", "");
        this.loc = parseLoc(o);
    }

    public static ArrayList<Friend> getArray(JSONArray a) {
        // Convert JSON array into model list while ignoring invalid entries.
        ArrayList<Friend> l = new ArrayList<>();
        if (a != null) {
            for (int i = 0; i < a.length(); i++) {
                try {
                    l.add(new Friend(a.getJSONObject(i)));
                } catch (Exception e) {
                    // ignore bad entries
                }
            }
        }
        return l;
    }

    public static ArrayList<Friend> merge(JSONArray friends, JSONArray sharedLocFriends) {
        ArrayList<Friend> merged = new ArrayList<>();
        java.util.LinkedHashMap<String, Friend> map = new java.util.LinkedHashMap<>();

        if (friends != null) {
            for (int i = 0; i < friends.length(); i++) {
                try {
                    Friend f = new Friend(friends.getJSONObject(i));
                    if (isEmpty(f.uid)) {
                        continue;
                    }
                    f.isFriend = true;
                    map.put(f.uid, f);
                } catch (Exception ignored) {
                    // Skip invalid entries.
                }
            }
        }

        if (sharedLocFriends != null) {
            for (int i = 0; i < sharedLocFriends.length(); i++) {
                try {
                    Friend locFriend = fromLoc(sharedLocFriends.getJSONObject(i));
                    if (isEmpty(locFriend.uid)) {
                        continue;
                    }
                    Friend existing = map.get(locFriend.uid);
                    if (existing == null) {
                        map.put(locFriend.uid, locFriend);
                    } else {
                        existing.sharedWithMe = true;
                        if (isEmpty(existing.name) && !isEmpty(locFriend.name)) {
                            existing.name = locFriend.name;
                        }
                        if (!isEmpty(locFriend.lockey)) {
                            existing.lockey = locFriend.lockey;
                        }
                        if (!isEmpty(locFriend.phone)) {
                            existing.phone = locFriend.phone;
                        }
                        existing.hasLocData = locFriend.hasLocData;
                        existing.distance = locFriend.distance;
                        existing.dist = locFriend.dist;
                        existing.bearing = locFriend.bearing;
                        existing.locAge = locFriend.locAge;
                    }
                } catch (Exception ignored) {
                    // Skip invalid entries.
                }
            }
        }

        merged.addAll(map.values());
        return merged;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean parseLoc(JSONObject o) {
        try {
            if (o.has("loc")) {
                String raw = o.optString("loc", "0");
                if ("1".equals(raw) || "true".equalsIgnoreCase(raw)) {
                    return true;
                }
                if ("0".equals(raw) || "false".equalsIgnoreCase(raw)) {
                    return false;
                }
                return o.optInt("loc", 0) == 1 || o.optBoolean("loc", false);
            }
        } catch (Exception ignored) {
            // Fall through to default.
        }
        return false;
    }

    private static Friend fromLoc(JSONObject o) {
        Friend f = new Friend(o);
        f.uid = o.optString("uid", o.optString("friend", f.uid));
        f.name = o.optString("name", o.optString("fname", f.name));
        f.lockey = o.optString("lockey", f.lockey);
        f.phone = o.optString("phone", f.phone);
        // Loc list indicates they shared with me; it does not mean I shared with them.
        f.loc = false;
        f.sharedWithMe = true;
        f.isLocFriend = true;

        try {
            if (o.has("distance")) {
                String d = o.optString("distance", "");
                if (!isEmpty(d)) {
                    f.hasLocData = true;
                    try {
                        f.dist = Double.parseDouble(d);
                        f.distance = d + " km";
                    } catch (NumberFormatException ignored) {
                        f.distance = d;
                    }
                }
            }
            if (o.has("bearing")) {
                f.bearing = o.optInt("bearing", 0);
            }
            if (o.has("age")) {
                f.locAge = o.optDouble("age", 0);
            }
        } catch (Exception ignored) {
            // Best-effort parsing only.
        }
        return f;
    }
}
