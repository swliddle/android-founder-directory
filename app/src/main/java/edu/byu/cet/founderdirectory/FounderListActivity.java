package edu.byu.cet.founderdirectory;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import edu.byu.cet.founderdirectory.fastscroller.FastScroller;
import edu.byu.cet.founderdirectory.fastscroller.SectionTitleProvider;
import edu.byu.cet.founderdirectory.provider.FounderProvider;
import edu.byu.cet.founderdirectory.utilities.AnalyticsManager;
import edu.byu.cet.founderdirectory.utilities.BitmapWorkerTask;
import edu.byu.cet.founderdirectory.utilities.PhotoManager;

/**
 * An activity representing a list of Founders. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link FounderDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class FounderListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Tag for logging.
     */
    private static final String TAG = "FounderListActivity";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private RecyclerView mRecyclerView;
    private FastScroller mFastScroller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_founder_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        mRecyclerView = (RecyclerView) findViewById(R.id.founder_list);
        assert mRecyclerView != null;
        setupRecyclerView();

        if (findViewById(R.id.founder_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        AnalyticsManager.getInstance(getApplication()).report("list", "");
    }

    private void setupRecyclerView() {
        getSupportLoaderManager().initLoader(0, null, this);
        mFastScroller = (FastScroller) findViewById(R.id.fastscroll);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new FounderAdapter());
        mFastScroller.setRecyclerView(mRecyclerView);

        // NEEDSWORK: change color of scrolling thumb to accent color when scrolling
        // NEEDSWORK: also change color of section title popup to accent color
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader: " + id + ", args: " + args);
        return(new CursorLoader(this,
                FounderProvider.Contract.CONTENT_URI,
                null, null, null,
                FounderProvider.Contract.PREFERRED_FULL_NAME));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished: " + loader + ", cursor: " + cursor);
        ((FounderAdapter) mRecyclerView.getAdapter()).setFounders(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset: " + loader);
    }

    public class FounderRowController extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView mPhoto = null;
        private TextView mName = null;
        private int mNameColumn = -1;
        private int mUrlColumn = -1;

        public FounderRowController(View row) {
            super(row);

            mPhoto = (ImageView) row.findViewById(R.id.photo);
            mName = (TextView) row.findViewById(R.id.name);

            row.setOnClickListener(this);
        }

        public void bindModel(Cursor founder) {
            Context context = getApplicationContext();

            if (mUrlColumn < 0) {
                mUrlColumn = founder.getColumnIndexOrThrow(FounderProvider.Contract.IMAGE_URL);
                mNameColumn = founder.getColumnIndexOrThrow(FounderProvider.Contract.PREFERRED_FULL_NAME);
            }

            String url = founder.getString(mUrlColumn);

            if (!TextUtils.isEmpty(url)) {
                url = PhotoManager.getSharedPhotoManager(context).urlForFileName(url);

                if (url != null) {
                    BitmapWorkerTask.loadBitmap(context, url, mPhoto);
                }
            } else {
                url = null;
            }

            if (url == null) {
                mPhoto.setImageResource(R.drawable.rollins_logo_e_40);
            }

            mName.setText(founder.getString(mNameColumn));
        }

        @Override
        public void onClick(View v) {
            // NEEDSWORK: process a click
            Log.d(TAG, "You clicked on " + mName.getText());
        }
    }

    public class FounderAdapter extends RecyclerView.Adapter<FounderRowController> implements SectionTitleProvider {
        private Cursor mFounders = null;
        private int mNameColumn = -1;

        public void setFounders(Cursor cursor) {
            mFounders = cursor;
            notifyDataSetChanged();
        }

        @Override
        public FounderRowController onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FounderRowController(getLayoutInflater().inflate(R.layout.founder_list_content, parent, false));
        }

        @Override
        public void onBindViewHolder(FounderRowController holder, int position) {
            mFounders.moveToPosition(position);
            holder.bindModel(mFounders);
        }

        @Override
        public int getItemCount() {
            if (mFounders == null) {
                return 0;
            }

            return mFounders.getCount();
        }

        @Override
        public String getSectionTitle(int position) {
            mFounders.moveToPosition(position);

            if (mNameColumn < 0) {
                mNameColumn = mFounders.getColumnIndexOrThrow(FounderProvider.Contract.PREFERRED_FULL_NAME);
            }

            return mFounders.getString(mNameColumn).substring(0, 1);
        }
    }
}