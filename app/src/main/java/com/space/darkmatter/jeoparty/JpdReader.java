package com.space.darkmatter.jeoparty;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.space.darkmatter.jeoparty.JpdAnswer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static android.content.ContentValues.TAG;

public class JpdReader extends AsyncTask {

    public HashMap<Pair<String, String>, JpdAnswer> jpd;
    public HashMap<Pair<String, String>, JpdAnswer> dbl_jpd;

    public HashMap<String, HashMap<String, HashMap<Integer, JpdAnswer > > > jpdData;

    public JpdAnswer finalJpd;
    public Set<String> jpd_categories;
    public Set<String> dbl_jpd_categories;
    public String final_category;
    public HashMap<Integer, Boolean> mJpdValues;
    public HashMap<Integer, Boolean> mDblJpdValues;

    public Integer[] mJpdValuesArray;
    public Integer[] mDblJpdValuesArray;



    private String mJsonUrl;

    @Override
    protected Object doInBackground(Object[] params) {
        if(android.os.Debug.isDebuggerConnected())
            android.os.Debug.waitForDebugger();

        loadJSON();
        return null;
    }

    // you may separate this or combined to caller class.
    public interface AsyncResponse {
        void processFinish(String output);
    }

    public AsyncResponse delegate = null;

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        delegate.processFinish("");
    }

    public JpdReader(AsyncResponse delegate, String url) {
        mJsonUrl = url;
        this.delegate = delegate;
    }

    Integer getValue(String val) {

        String newval = val;
        newval = val.replace("$","");
        newval = newval.replace(",", "");
        return Integer.parseInt(newval);

    }

    public void loadJSON() {


        String json = null;

        try {

            URL website = new URL(mJsonUrl);
            URLConnection connection = website.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream()));

            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();

            json = response.toString();

        }catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        parseData(json);

    }

    public void getRoundValues( JSONArray games_arry) {

        try {
            for (int i = 0; i < games_arry.length(); i++) {

                JSONObject obj = games_arry.getJSONObject(i);


                String round = obj.getString("round");
                String vStr = obj.getString("value");

                if ( vStr == "null" ) {
                    continue;
                }

                Integer value = getValue(vStr);

                if ( round.equals("Jeopardy!")) {
                    mJpdValues.put(value, true);
                } else if (round.equals("Double Jeopardy!")) {
                    mDblJpdValues.put(value, true);
                }

            }


        } catch(JSONException e) {

        }

    }
    public void parseData(String content) {


        JSONArray games_arry = null;
        jpd = new HashMap<Pair<String, String>, JpdAnswer>();
        dbl_jpd = new HashMap<Pair<String, String>, JpdAnswer>();
        jpd_categories = new HashSet<String>();
        dbl_jpd_categories = new HashSet<String>();
        finalJpd = new JpdAnswer();
        final_category = new String();
        mJpdValues = new HashMap<>();
        mDblJpdValues = new HashMap<>();

        jpdData = new HashMap<>();


        try {
            games_arry = new JSONArray(content);

            getRoundValues(games_arry);


            Set<Integer> jpdKeys = mJpdValues.keySet();
            mJpdValuesArray = jpdKeys.toArray(new Integer[jpdKeys.size()]);
            Arrays.sort(mJpdValuesArray);

            Set<Integer> dblJpdKeys = mDblJpdValues.keySet();
            mDblJpdValuesArray = dblJpdKeys.toArray(new Integer[dblJpdKeys.size()]);
            Arrays.sort(mDblJpdValuesArray);


            for (int i = 0; i < games_arry.length(); i++) {

                JSONObject obj = games_arry.getJSONObject(i);
                JpdAnswer ans = new JpdAnswer();

                ans.category = obj.getString("category");
                ans.air_date = obj.getString("air_date");
                ans.answer = obj.getString("answer");
                ans.round = obj.getString("round");
                ans.show_number = obj.getString("show_number");
                ans.value = obj.getString("value");
                ans.question = obj.getString("question");

                if ( ans.round.equals("Jeopardy!")) {
                    jpd.put(Pair.create(ans.category, ans.value), ans);
                    jpd_categories.add(ans.category);
                } else if (ans.round.equals("Double Jeopardy!")) {
                    dbl_jpd.put(Pair.create(ans.category, ans.value), ans);
                    dbl_jpd_categories.add(ans.category);
                } else {
                    finalJpd = ans;
                    final_category = ans.category;
                }

                //
                String roundName = ans.round;
                String category = ans.category;
                Integer value;

                if ( ans.value == "null") {
                    value = 0;
                } else {
                   value = getValue(ans.value);
                }



                if ( jpdData.containsKey(roundName) ) {
                    HashMap<String, HashMap<Integer, JpdAnswer> > level = jpdData.get(roundName);
                    if ( level.containsKey(category) ) {
                        HashMap<Integer, JpdAnswer> valueMap = level.get(category);
                        valueMap.put(value, ans);

                    } else {

                        HashMap<Integer, JpdAnswer> valueMap = new HashMap<>();
                        valueMap.put(value, ans);
                        level.put(category, valueMap);
                    }

                } else {

                    HashMap<String, HashMap<Integer, JpdAnswer> > level = new HashMap<>();
                    HashMap<Integer, JpdAnswer> valueMap = new HashMap<>();
                    valueMap.put(value, ans);
                    level.put(category, valueMap);
                    jpdData.put(roundName, level);

                }
            }
           return;
        }
        catch (JSONException ex) {
            ex.printStackTrace();
            return;
        }
    }

}