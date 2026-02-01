package pro.flysafe.sms.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Friend {
    public String uid;
    public String name;
    public String lockey;
    public String phone;
    public boolean loc;

    public Friend(JSONObject o) {
        // Accept either "friend"/"fname" or "uid"/"name" fields depending on endpoint.
        this.name = o.optString("fname", o.optString("name", ""));
        this.uid = o.optString("friend", o.optString("uid", ""));
        this.lockey = o.optString("lockey", "");
        this.phone = o.optString("phone", "");
        this.loc = "1".equals(o.optString("loc", "0"));
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
}
