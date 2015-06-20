package ray.cyberpup.com.githubdrill2;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created on 6/18/15
 *
 * @author Raymond Tong
 */
public class RepoView extends AppCompatActivity implements DownloadReposTask.TaskListener {

    private String mRepoUrl = null;
    private TextView tvUserName = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_table_repo_v3);

        Intent dataFromIntent = getIntent();

        mRepoUrl = dataFromIntent.getStringExtra(Constants.GIT_REPOS);

        tvUserName = (TextView) findViewById(R.id.tv_username);

        tvUserName.setText(dataFromIntent.getStringExtra(Constants.GIT_USERID));



    }

    private void downloadRepos(){
        DownloadReposTask taskFrag = (DownloadReposTask) getSupportFragmentManager().
                findFragmentByTag(Constants.DOWNLOAD_REPOS);

        if (taskFrag == null) {

            FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
            DownloadReposTask frag = DownloadReposTask.getInstance(mRepoUrl);
            tran.add(frag, Constants.DOWNLOAD_REPOS).commit();
            frag.beginTask();

        } else {

            taskFrag.beginTask();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        downloadRepos();
    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onProgressUpdate(Integer... progress) {

    }

    @Override
    public void onPostExecute(String results) {

        // Parse json repositories string & returns a custom adapter called GitRepoAdapter
        RepoViewLoaderTask task = new RepoViewLoaderTask();
        task.execute(results);

        cleanUp();

    }

    public void cleanUp(){
        final FragmentManager man = getSupportFragmentManager();
        final Fragment fragment = man.findFragmentByTag(Constants.DOWNLOAD_REPOS);

        if(fragment!=null)
            man.beginTransaction().remove(fragment).commit();
    }

    @Override
    public void onCancelled() {

    }


    private class RepoViewLoaderTask extends AsyncTask<String, Integer, List<HashMap<String,Object>>> {

        @Override
        protected List<HashMap<String,Object>> doInBackground(String... jsonString) {

            // Store list of repositories
            List<HashMap<String, Object>> repos = null;

            try {

                repos = getListOfReposFromJson(jsonString[0]);


            } catch (JSONException e) {
                e.printStackTrace();
            }


            return repos;
        }

        private List<HashMap<String, Object>> getListOfReposFromJson(final String jsonArrayOfRepos) throws JSONException {


            List<HashMap<String, Object>> listOfRepos = new ArrayList<HashMap<String, Object>>();

            JSONArray queryResults = new JSONArray(jsonArrayOfRepos);

            //iterate through JSONArray (Each entry is a single repository)
            for (int i = 0; i < queryResults.length(); i++) {

                listOfRepos.add(getSingleRepository((JSONObject) queryResults.get(i)));

            }


            return listOfRepos;

        }

        private HashMap<String, Object> getSingleRepository(JSONObject repoInfo) {

            HashMap<String,Object> singleRepoHM = new HashMap<String,Object>();
            try {
                singleRepoHM.put(Constants.GIT_REPO_NAME, repoInfo.getString(Constants.GIT_REPO_NAME));
                singleRepoHM.put(Constants.GIT_DESCRIPTION, repoInfo.getString(Constants.GIT_DESCRIPTION));
                singleRepoHM.put(Constants.GIT_STARS, repoInfo.getInt(Constants.GIT_STARS));
                singleRepoHM.put(Constants.GIT_WATCHERS, repoInfo.getInt(Constants.GIT_WATCHERS));
                return singleRepoHM;
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Something went wrong if we reach this
            return null;

        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(List<HashMap<String,Object>> listOfRepos){



            createHeaderRow();
            TableLayout scrollablePart = (TableLayout) findViewById(R.id.scrollable_part);


                int i=0;

                for (HashMap<String, Object> single_repo : listOfRepos) {

                    HashMap<String, Object> repository = new HashMap<String,Object>();
                    repository.put(Constants.GIT_REPO_NAME, single_repo.get(Constants.GIT_REPO_NAME));
                    repository.put(Constants.GIT_DESCRIPTION, single_repo.get(Constants.GIT_DESCRIPTION));
                    repository.put(Constants.GIT_STARS, single_repo.get(Constants.GIT_STARS));
                    repository.put(Constants.GIT_WATCHERS, single_repo.get(Constants.GIT_WATCHERS));

                    TableRow row = createTableRow(repository);
                    if(i%2!=0)
                        row.setBackgroundResource(R.color.rowColoreven);
                    i++;

                    scrollablePart.addView(row);

                }



/*
            int i=0;
            for(HashMap<String,Object> hm:listOfRepos){

                Set<String> keys = hm.keySet();

                for(String key:keys)
                    System.out.printf("%s i=%d%n",hm.get(key),i);
                i++;
            }
            */

        }
        int[] mFixedColumnWidths = new int[]{30, 50, 10, 10};
        // This remains fixed as the rest of the table scrolls
        private void createHeaderRow() {
            TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);


            int fixedHeaderHeight=60;

            TableRow row = new TableRow(RepoView.this);
            TableLayout header = (TableLayout) findViewById(R.id.table_header);
            row.setLayoutParams(tableRowParams);
            row.setGravity(Gravity.FILL);
            row.setBackgroundResource(R.color.rowColoreven);

            TextView nameHeader = getTableRowCell("Name", mFixedColumnWidths[0], fixedHeaderHeight);
            nameHeader.setPadding(convertFromDipToPx(16), 0, 0, 0);

            row.addView(nameHeader);
            row.addView(getTableRowCell("Description", mFixedColumnWidths[1], fixedHeaderHeight));
            row.addView(getTableRowCell("Stars", mFixedColumnWidths[2], fixedHeaderHeight));

            TextView watcherHeader = getTableRowCell("Watchers",mFixedColumnWidths[3], fixedHeaderHeight);
            watcherHeader.setPadding(0,0,convertFromDipToPx(8),0);
            watcherHeader.setEllipsize(TextUtils.TruncateAt.END);
            watcherHeader.setSingleLine();
            row.addView(watcherHeader);

            header.addView(row);
        }

        private TableRow createTableRow(HashMap<String,Object> singleRepository) {

            TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);

            int fixedRowHeight = 60;

            TableRow row = new TableRow(RepoView.this);

            row.setLayoutParams(tableRowParams);
            row.setGravity(Gravity.FILL);

            TextView repoName = getTableRowCell((String)singleRepository.get(Constants.GIT_REPO_NAME)
                    , mFixedColumnWidths[0],fixedRowHeight);
            repoName.setPadding(convertFromDipToPx(8),0,0,0);
            row.addView(repoName);
            row.addView(getTableRowCell((String)singleRepository.get(Constants.GIT_DESCRIPTION)
                    , mFixedColumnWidths[1],fixedRowHeight));
            row.addView(getTableRowCell(Integer.toString((int)singleRepository.get(Constants.GIT_STARS))
                    , mFixedColumnWidths[2],fixedRowHeight));
            row.addView(getTableRowCell(Integer.toString((int)singleRepository.get(Constants.GIT_WATCHERS))
                    , mFixedColumnWidths[3],fixedRowHeight));

            return row;
        }

        private TextView recyclableTextView;

        public TextView getTableRowCell (String text, int widthInPercentOfScreenWidth,
                                         int fixedHeightInPixels){
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            recyclableTextView = new TextView(RepoView.this);
            recyclableTextView.setText(text);
            recyclableTextView.setTextColor(getResources().getColor(R.color.primaryColorDark));
            recyclableTextView.setTextSize(12);
            recyclableTextView.setEllipsize(TextUtils.TruncateAt.END);
            recyclableTextView.setWidth(widthInPercentOfScreenWidth * screenWidth / 100);
            recyclableTextView.setHeight(fixedHeightInPixels);

            return recyclableTextView;
        }

    }

    private int convertFromDipToPx(int dip){
        Resources r = getResources();
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }


}
