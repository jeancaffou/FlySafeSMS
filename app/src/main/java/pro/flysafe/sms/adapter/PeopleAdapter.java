package pro.flysafe.sms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import pro.flysafe.sms.R;
import pro.flysafe.sms.model.Person;

/**
 * Simple people list adapter for the "Add friend" search dialog.
 */
public class PeopleAdapter extends BaseAdapter {
    private final Context ctx;
    private final ArrayList<Person> people = new ArrayList<>();

    public PeopleAdapter(Context ctx) {
        this.ctx = ctx;
    }

    public void setPeople(ArrayList<Person> items) {
        // Replace current list with new search results.
        people.clear();
        if (items != null) {
            people.addAll(items);
        }
        notifyDataSetChanged();
    }

    public Person getPerson(int position) {
        return people.get(position);
    }

    @Override
    public int getCount() {
        return people.size();
    }

    @Override
    public Object getItem(int position) {
        return people.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(ctx).inflate(R.layout.row_person, parent, false);
        }

        Person person = getPerson(position);
        TextView name = view.findViewById(R.id.personName);
        name.setText(person.name);

        return view;
    }
}
