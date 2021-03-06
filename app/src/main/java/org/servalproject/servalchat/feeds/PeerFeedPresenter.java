package org.servalproject.servalchat.feeds;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

import org.servalproject.mid.Identity;
import org.servalproject.mid.KnownPeers;
import org.servalproject.mid.MessageFeed;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;
import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.Subscriber;

/**
 * Created by jeremy on 3/08/16.
 */
public class PeerFeedPresenter extends Presenter<PeerFeed> {

	private Serval serval;
	private FeedAdapter adapter;
	private Peer peer;
	private MessageFeed feed;
	private boolean busy;

	protected PeerFeedPresenter(PresenterFactory<PeerFeed, ?> factory, String key, Identity identity) {
		super(factory, key, identity);
	}

	public static PresenterFactory<PeerFeed, PeerFeedPresenter> factory
			= new PresenterFactory<PeerFeed, PeerFeedPresenter>() {

		@Override
		protected String getKey(Identity id, Bundle savedState) {
			try {
				Subscriber them = KnownPeers.getSubscriber(savedState);
				return id.subscriber.sid.toHex() + ":" + them.sid.toHex();
			} catch (AbstractId.InvalidBinaryException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		protected PeerFeedPresenter create(String key, Identity id) {
			return new PeerFeedPresenter(this, key, id);
		}

	};

	@Override
	protected void bind() {
		PeerFeed feed = getView();
		feed.list.setAdapter(adapter);
	}

	@Override
	protected void restore(Bundle config) {
		try {
			serval = Serval.getInstance();
			peer = serval.knownPeers.getPeer(config);
			feed = peer.getFeed();
			adapter = new FeedAdapter(feed);

		} catch (AbstractId.InvalidBinaryException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void onVisible() {
		super.onVisible();
		adapter.onVisible();
	}

	@Override
	public void onHidden() {
		super.onHidden();
		adapter.onHidden();
	}

	public void subscribe(final boolean subscribe){
		busy = true;
		new AsyncTask<Void, Void, Void>(){
			private Exception e;

			@Override
			protected void onPostExecute(Void aVoid) {
				super.onPostExecute(aVoid);
				busy = false;
				PeerFeed view = getView();
				if (view == null)
					return;

				if (e != null)
					view.activity.showError(e);
				else
					view.activity.showSnack(
							subscribe ? R.string.followed : R.string.ignored,
							Snackbar.LENGTH_SHORT);
			}

			@Override
			protected Void doInBackground(Void... voids) {
				try {
					if (subscribe) {
						identity.follow(feed.id);
					} else {
						identity.ignore(feed.id);
					}
				} catch (Exception e) {
					this.e = e;
				}
				return null;
			}
		}.execute();
	}
}
