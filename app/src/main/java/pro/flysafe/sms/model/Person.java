package pro.flysafe.sms.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Minimal person model returned from /mobile/searchPeople.
 * Kept intentionally small for the open-source SMS companion app.
 */
public class Person {
    public final String uid;
    public final String name;

    public Person(JSONObject o) {
        this.uid = o.optString("uid", "");
        this.name = o.optString("name", "");
    }

    public static ArrayList<Person> getArray(JSONArray a) {
        ArrayList<Person> list = new ArrayList<>();
        if (a != null) {
            for (int i = 0; i < a.length(); i++) {
                try {
                    list.add(new Person(a.getJSONObject(i)));
                } catch (Exception ignored) {
                    // Skip invalid entries.
                }
            }
        }
        return list;
    }
}
