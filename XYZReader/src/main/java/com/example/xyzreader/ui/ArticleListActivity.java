package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ConnectivityReceiver;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.MyApplication;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.utils.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends MyApplication implements
        LoaderManager.LoaderCallbacks<Cursor>, ConnectivityReceiver.ConnectivityReceiverListener {

    private static final String TAG = ArticleListActivity.class.toString();

    @BindView(R.id.rv_articles)
    RecyclerView mRVArticles;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.getInstance().setConnectivityListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }

        ButterKnife.bind(this);

        checkConnection();
    }

    private void checkConnection() {
        boolean isConnected = ConnectivityReceiver.isConnected();
        showSnack(isConnected);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRVArticles.setAdapter(adapter);

        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(
                        getResources().getInteger(R.integer.list_column_count),
                        StaggeredGridLayoutManager.VERTICAL);

        mRVArticles.setLayoutManager(sglm);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRVArticles.setAdapter(null);
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        showSnack(isConnected);
    }

    private void showSnack(boolean isConnected) {
        String message;
        int color;
        if (isConnected) {
            message = "Good! Connected to Internet";
            color = Color.WHITE;
        } else {
            message = "Sorry! Not connected to internet";
            color = Color.RED;
        }

        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.cl_articlelist), message, Snackbar.LENGTH_LONG);

        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(color);
        snackbar.show();
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private Cursor cursor;

        Adapter(Cursor cursor) {
            this.cursor = cursor;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder viewHolder = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    Uri itemUri = ItemsContract.Items.buildItemUri(getItemId(viewHolder.getAdapterPosition()));
                    Intent intent = new Intent(Intent.ACTION_VIEW, itemUri);
                    startActivity(intent);
                }
            });
            return viewHolder;
        }

        @Override
        public long getItemId(int position) {
            cursor.moveToPosition(position);
            long id = cursor.getLong(ArticleLoader.Query._ID);
            return id;
        }

        @Override
        public int getItemCount() {
            return cursor.getCount();
        }

        @Override
        public void onBindViewHolder(ViewHolder vh, int position) {
            cursor.moveToPosition(position);
            vh.bind(cursor);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private SimpleDateFormat outputFormat = new SimpleDateFormat();
        private GregorianCalendar epoch = new GregorianCalendar(2, 1, 1);

        private Context mContext;

        @BindView(R.id.thumbnail)
        DynamicHeightNetworkImageView thumbnailView;

        @BindView(R.id.article_title)
        TextView mTVTitle;

        @BindView(R.id.article_subtitle)
        TextView mTVSubtitle;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

            mContext = view.getContext();
        }

        void bind(@NonNull Cursor cursor) {

            mTVTitle.setText(cursor.getString(ArticleLoader.Query.TITLE));
            thumbnailView.setImageUrl(
                    cursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(mContext).getImageLoader()
            );

            thumbnailView.setAspectRatio(cursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            renderSubtitle(cursor);
        }

        private void renderSubtitle(@NonNull Cursor cursor) {

            Date publishedDt = DateUtil.parse(cursor.getString(ArticleLoader.Query.PUBLISHED_DATE));

            if (publishedDt == null)
                publishedDt = new Date();

            if (publishedDt.before(epoch.getTime())) {
                mTVSubtitle.setText(Html.fromHtml(
                        outputFormat.format(publishedDt) + "<br/>" + " by "
                                + cursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                mTVSubtitle.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDt.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by " + cursor.getString(ArticleLoader.Query.AUTHOR)));
            }
        }

    }
}
