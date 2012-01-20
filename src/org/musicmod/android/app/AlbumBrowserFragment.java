package org.musicmod.android.app;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import org.musicmod.android.util.MusicUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class AlbumBrowserFragment extends Fragment implements Constants, OnItemClickListener,
		LoaderManager.LoaderCallbacks<Cursor> {

	private AlbumsAdapter mAdapter;
	private String mCurFilter;
	private GridView mGridView;

	private int mIdIdx, mAlbumIdx, mArtistIdx;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		mAdapter = new AlbumsAdapter(getActivity(), null, false);

		View fragmentView = getView();
		mGridView = (GridView) fragmentView.findViewById(R.id.album_gridview);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);
		// mGridView.setOnCreateContextMenuListener(this);
		mGridView.setTextFilterEnabled(true);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.albums_browser, container, false);
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_META_CHANGED);
		filter.addAction(BROADCAST_QUEUE_CHANGED);
		getActivity().registerReceiver(mMediaStatusReceiver, filter);
	}

	@Override
	public void onStop() {
		getActivity().unregisterReceiver(mMediaStatusReceiver);
		super.onStop();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String[] cols = new String[] { MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM,
				MediaStore.Audio.Albums.ARTIST };

		Uri baseUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
		if (mCurFilter != null) {
			baseUri = baseUri.buildUpon().appendQueryParameter("filter", Uri.encode(mCurFilter))
					.build();
		}

		return new CursorLoader(getActivity(), baseUri, cols, null, null,
				MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		mIdIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID);
		mAlbumIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
		mArtistIdx = data.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST);

		mAdapter.swapCursor(data);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

		mAdapter.swapCursor(null);
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		showDetails(position, id);
	}

	private void showDetails(int index, long id) {

		Bundle bundle = new Bundle();
		bundle.putString(INTENT_KEY_MIMETYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
		bundle.putLong(Audio.Albums._ID, id);

		View detailsFrame = getActivity().findViewById(R.id.frame_details);
		boolean mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE && getResources().getBoolean(R.bool.dual_pane);

		if (mDualPane) {
			mGridView.setSelection(index);

			TrackBrowserFragment fragment = new TrackBrowserFragment();
			fragment.setArguments(bundle);

			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.frame_details, fragment);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();

		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(getActivity(), TrackBrowserActivity.class);
			intent.putExtras(bundle);
			startActivity(intent);
		}
	}

	private BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mGridView.invalidateViews();
		}

	};

	private class AlbumsAdapter extends CursorAdapter {

		private final BitmapDrawable mDefaultAlbumIcon;

		private class ViewHolder {

			TextView album_name;
			TextView artist_name;
			ImageView album_art;

			public ViewHolder(View view) {
				album_name = (TextView) view.findViewById(R.id.album_name);
				artist_name = (TextView) view.findViewById(R.id.artist_name);
				album_art = (ImageView) view.findViewById(R.id.album_art);
			}

		}

		private AlbumsAdapter(Context context, Cursor cursor, boolean autoRequery) {

			super(context, cursor, autoRequery);

			Resources r = context.getResources();

			Bitmap b = BitmapFactory.decodeResource(r, R.drawable.ic_mp_albumart_unknown);
			mDefaultAlbumIcon = new BitmapDrawable(context.getResources(), b);
			// no filter or dither, it's a lot faster and we can't tell the
			// difference
			mDefaultAlbumIcon.setFilterBitmap(false);
			mDefaultAlbumIcon.setDither(false);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = LayoutInflater.from(context).inflate(R.layout.album_grid_item, null);
			ViewHolder viewholder = new ViewHolder(view);
			view.setTag(viewholder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder viewholder = (ViewHolder) view.getTag();

			String album_name = cursor.getString(mAlbumIdx);
			if (album_name == null || MediaStore.UNKNOWN_STRING.equals(album_name)) {
				viewholder.album_name.setText(R.string.unknown_album);
			} else {
				viewholder.album_name.setText(album_name);
			}

			String artist_name = cursor.getString(mArtistIdx);
			if (album_name == null || MediaStore.UNKNOWN_STRING.equals(album_name)) {
				viewholder.artist_name.setText(R.string.unknown_artist);
			} else {
				viewholder.artist_name.setText(artist_name);
			}

			// We don't actually need the path to the thumbnail file,
			// we just use it to see if there is album art or not
			long aid = cursor.getLong(mIdIdx);
			int width = getResources().getDimensionPixelSize(R.dimen.gridview_bitmap_width);
			int height = getResources().getDimensionPixelSize(R.dimen.gridview_bitmap_height);

			viewholder.album_art.setImageBitmap(MusicUtils.getCachedArtwork(getActivity(), aid,
					width, height));

			// viewholder.album_art.setTag(aid);
			// new AsyncAlbumArtLoader(viewholder.album_art,
			// mShowFadeAnimation,
			// aid, width, height).execute();

			long currentalbumid = MusicUtils.getCurrentAlbumId();
			if (currentalbumid == aid) {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_indicator_nowplaying_small, 0);
			} else {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

		}

	}
}