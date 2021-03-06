package org.servalproject.servalchat.identity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.KnownPeers;
import org.servalproject.mid.Messaging;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.ObservedRecyclerView;
import org.servalproject.servalchat.views.RecyclerHelper;
import org.servalproject.servaldna.meshms.MeshMSConversation;

/**
 * Created by jeremy on 13/07/16.
 */
public class ConversationList
		extends ObservedRecyclerView<MeshMSConversation, ConversationList.ConversationHolder>
		implements INavigate {

	private Messaging messaging;
	private static final String TAG = "ConversationList";
	private final Serval serval;

	public ConversationList(Context context, @Nullable AttributeSet attrs) {
		super(null, context, attrs);
		this.serval = Serval.getInstance();
		setHasFixedSize(true);
		RecyclerHelper.createLayoutManager(this, true, false);
		RecyclerHelper.createDivider(this);
	}


	@Override
	protected ConversationHolder createHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_item, parent, false);
		return new ConversationHolder(view);
	}

	@Override
	protected void bind(ConversationHolder holder, MeshMSConversation item) {
		holder.setConversation(item);
	}

	@Override
	protected void unBind(ConversationHolder holder, MeshMSConversation item) {
		holder.setConversation(null);
	}

	@Override
	protected MeshMSConversation get(int position) {
		if (position >= getCount())
			return null;
		return messaging.conversations.get(position);
	}

	@Override
	protected int getCount() {
		if (messaging == null)
			return 0;
		return messaging.conversations.size();
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args) {
		if (this.messaging != id.messaging) {
			this.messaging = id.messaging;
			this.setObserverSet(messaging.observers);
			notifyChanged();
		}
		return super.onAttach(activity, n, id, args);
	}

	public class ConversationHolder
			extends RecyclerView.ViewHolder
			implements View.OnClickListener, Observer<Peer> {
		final TextView name;
		private MeshMSConversation conversation;
		private Peer peer;

		public ConversationHolder(View itemView) {
			super(itemView);
			name = (TextView) this.itemView.findViewById(R.id.name);
			name.setOnClickListener(this);
		}

		public void setConversation(MeshMSConversation conversation) {
			this.conversation = conversation;
			Peer p = null;
			if (conversation != null)
				p = serval.knownPeers.getPeer(conversation.them);
			if (this.peer != p) {
				if (this.peer != null)
					this.peer.observers.remove(this);
				this.peer = p;
				if (p != null)
					this.peer.observers.add(this);
			}
			if (p != null)
				updated(p);
		}

		@Override
		public void onClick(View v) {
			Bundle args = new Bundle();
			KnownPeers.saveSubscriber(conversation.them, args);
			activity.go(identity, Navigation.PrivateMessages, args);
		}

		@Override
		public void updated(Peer obj) {
			name.setText(obj.displayName());
		}
	}
}
