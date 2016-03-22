package edu.byu.cet.founderdirectory;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import edu.byu.cet.founderdirectory.provider.FounderProvider;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_founder_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.founder_list);
        assert mRecyclerView != null;
        setupRecyclerView(mRecyclerView);

        if (findViewById(R.id.founder_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        getSupportLoaderManager().initLoader(0, null, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new FounderAdapter());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return(new CursorLoader(this,
                FounderProvider.Contract.CONTENT_URI,
                null, null, null,
                FounderProvider.Contract.PREFERRED_FULL_NAME));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        ((FounderAdapter) mRecyclerView.getAdapter()).setFounders(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public class FounderRowController extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView mPhoto = null;
        private TextView mName = null;

        public FounderRowController(View row) {
            super(row);

            mPhoto = (ImageView) row.findViewById(R.id.photo);
            mName = (TextView) row.findViewById(R.id.name);

            row.setOnClickListener(this);
        }

        public void bindModel(Cursor founder) {
            Context context = getApplicationContext();
            String imageFileName = founder.getString(founder.getColumnIndexOrThrow(FounderProvider.Contract.IMAGE_URL));
            String imageUrl = PhotoManager.getSharedPhotoManager(context).urlForFileName(imageFileName);

            Log.d(TAG, "bindModel url: " + imageUrl);

            if (imageUrl != null) {
                BitmapWorkerTask.loadBitmap(context, imageUrl, mPhoto);
            } else {
                mPhoto.setImageResource(R.drawable.rollins_logo_e_40);
            }

            mName.setText(founder.getString(founder.getColumnIndexOrThrow(FounderProvider.Contract.PREFERRED_FULL_NAME)));
        }

        @Override
        public void onClick(View v) {
            // NEEDSWORK: process a click
            Log.d(TAG, "You clicked on " + mName.getText());
        }
    }

    public class FounderAdapter extends RecyclerView.Adapter<FounderRowController> {
        private Cursor mFounders = null;

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
    }
}
