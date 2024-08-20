package com.node.holders;

import android.view.View;
import android.widget.TextView;

import com.node.anonchat.R;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.node.common.data.model.Message;
import com.node.utils.FormatUtils;
import com.stfalcon.chatkit.utils.DateFormatter;

/*
 * Created by troy379 on 05.04.17.
 */
public class OutcomingVoiceMessageViewHolder
        extends MessageHolders.OutcomingTextMessageViewHolder<Message> {

    private TextView tvDuration;
    private TextView tvTime;

    public OutcomingVoiceMessageViewHolder(View itemView, Object payload) {
        super(itemView, payload);
        tvDuration = (TextView) itemView.findViewById(R.id.duration);
        tvTime = (TextView) itemView.findViewById(R.id.time);
    }

    @Override
    public void onBind(Message message) {
        super.onBind(message);
        tvDuration.setText(
                FormatUtils.getDurationString((int)
                        message.getVoice().getDuration()));
        tvTime.setText(DateFormatter.format(message.getCreatedAt(), DateFormatter.Template.TIME));
    }
}
